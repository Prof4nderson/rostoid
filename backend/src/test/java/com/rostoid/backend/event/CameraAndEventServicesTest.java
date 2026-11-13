package com.rostoid.backend.event;

import com.rostoid.backend.common.ApiViews;
import com.rostoid.backend.people.Person;
import com.rostoid.backend.people.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CameraAndEventServicesTest {
    @Mock private PersonRepository people;
    @Mock private RecognitionEventRepository events;
    @Mock private CameraConfigurationsService cameraService;
    @Mock private CameraConfigurationRepository cameraRepository;

    private Person person;

    @BeforeEach
    void setUp() {
        person = new Person("encrypted-face", "iv", Instant.now());
        ReflectionTestUtils.setField(person, "id", 41L);
        person.assignPublicId();
    }

    @Test
    void recognitionUsesServerConfiguredDirectionAndSnapshotsCamera() {
        CameraConfiguration camera = camera(12L, "browser-device-1", "Entrada Portão", CameraDirection.ENTRY);
        when(people.findForUpdate(41L)).thenReturn(Optional.of(person));
        when(cameraService.requireConfigured(12L)).thenReturn(camera);
        when(events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 12L, CameraDirection.ENTRY)).thenReturn(Optional.empty());
        when(events.save(org.mockito.ArgumentMatchers.any(RecognitionEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiViews.CreatedEventView response = new EventsService(people, events, cameraService).recognize(41L, 12L);

        ArgumentCaptor<RecognitionEvent> saved = ArgumentCaptor.forClass(RecognitionEvent.class);
        verify(events).save(saved.capture());
        assertEquals(12L, saved.getValue().getCameraId());
        assertEquals("Entrada Portão", saved.getValue().getCameraLabel());
        assertEquals(CameraDirection.ENTRY, saved.getValue().getDirection());
        assertEquals(CameraDirection.ENTRY, response.direction());
        assertTrue(response.created());
    }

    @Test
    void duplicateWithinThirtySecondsOnSamePersonCameraAndDirectionIsNotSavedAgain() {
        CameraConfiguration camera = camera(13L, "device-13", "Saída", CameraDirection.EXIT);
        RecognitionEvent previous = new RecognitionEvent(person, Instant.now().minusSeconds(8),
                13L, "Saída", CameraDirection.EXIT);
        when(people.findForUpdate(41L)).thenReturn(Optional.of(person));
        when(cameraService.requireConfigured(13L)).thenReturn(camera);
        when(events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 13L, CameraDirection.EXIT)).thenReturn(Optional.of(previous));

        ApiViews.CreatedEventView response = new EventsService(people, events, cameraService).recognize(41L, 13L);

        assertFalse(response.created());
        assertEquals(CameraDirection.EXIT, response.direction());
        verify(events, never()).save(org.mockito.ArgumentMatchers.any(RecognitionEvent.class));
    }

    @Test
    void oppositeDirectionOrAnotherCameraCanCreateAnImmediateEvent() {
        CameraConfiguration entry = camera(14L, "entry-device", "Entrada", CameraDirection.ENTRY);
        CameraConfiguration oppositeDirection = camera(14L, "entry-device", "Saída após reconfiguração", CameraDirection.EXIT);
        CameraConfiguration otherEntryCamera = camera(15L, "another-entry-device", "Outra entrada", CameraDirection.ENTRY);
        when(people.findForUpdate(41L)).thenReturn(Optional.of(person));
        when(cameraService.requireConfigured(14L)).thenReturn(entry, oppositeDirection);
        when(cameraService.requireConfigured(15L)).thenReturn(otherEntryCamera);
        when(events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 14L, CameraDirection.ENTRY)).thenReturn(Optional.empty());
        when(events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 14L, CameraDirection.EXIT)).thenReturn(Optional.empty());
        when(events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 15L, CameraDirection.ENTRY)).thenReturn(Optional.empty());
        when(events.save(org.mockito.ArgumentMatchers.any(RecognitionEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventsService service = new EventsService(people, events, cameraService);
        assertTrue(service.recognize(41L, 14L).created());
        assertTrue(service.recognize(41L, 14L).created());
        assertTrue(service.recognize(41L, 15L).created());

        verify(events).findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 14L, CameraDirection.ENTRY);
        verify(events).findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 14L, CameraDirection.EXIT);
        verify(events).findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                41L, 15L, CameraDirection.ENTRY);
        verify(events, org.mockito.Mockito.times(3)).save(org.mockito.ArgumentMatchers.any(RecognitionEvent.class));
    }

    @Test
    void cameraConfigurationAcceptsOneOrTwoDistinctOppositeDirections() {
        when(cameraRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        CameraConfigurationsService service = new CameraConfigurationsService(cameraRepository);

        assertTrue(service.replace(List.of()).isEmpty());
        List<ApiViews.CameraView> one = service.replace(List.of(request("one", "Câmera única", CameraDirection.ENTRY)));
        assertEquals(1, one.size());
        assertEquals(CameraDirection.ENTRY, one.getFirst().direction());

        List<ApiViews.CameraView> two = service.replace(List.of(
                request("entry", "Entrada", CameraDirection.ENTRY),
                request("exit", "Saída", CameraDirection.EXIT)));
        assertEquals(2, two.size());
        assertEquals(CameraDirection.ENTRY, two.get(0).direction());
        assertEquals(CameraDirection.EXIT, two.get(1).direction());
        verify(cameraRepository, org.mockito.Mockito.times(3)).findAll();
        verify(cameraRepository, never()).deleteAll(org.mockito.ArgumentMatchers.<CameraConfiguration>anyList());
        verify(cameraRepository, never()).deleteAllInBatch();
    }

    @Test
    void replacingSettingsKeepsTheIdentifierForAnExistingDevice() {
        CameraConfiguration existing = camera(21L, "same-device", "Entrada", CameraDirection.ENTRY);
        when(cameraRepository.findAll()).thenReturn(List.of(existing));
        when(cameraRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ApiViews.CameraView> result = new CameraConfigurationsService(cameraRepository)
                .replace(List.of(request("same-device", "Portaria atualizada", CameraDirection.EXIT)));

        assertEquals(21L, result.getFirst().id());
        assertEquals("Portaria atualizada", result.getFirst().label());
        assertEquals(CameraDirection.EXIT, result.getFirst().direction());
        verify(cameraRepository, never()).deleteAll(org.mockito.ArgumentMatchers.<CameraConfiguration>anyList());
    }

    @Test
    void cameraConfigurationRejectsPairsWithDuplicateDevicesOrSameDirection() {
        CameraConfigurationsService service = new CameraConfigurationsService(cameraRepository);

        assertThrows(ResponseStatusException.class, () -> service.replace(List.of(
                request("same", "Entrada", CameraDirection.ENTRY),
                request("same", "Saída", CameraDirection.EXIT))));
        assertThrows(ResponseStatusException.class, () -> service.replace(List.of(
                request("first", "Entrada 1", CameraDirection.ENTRY),
                request("second", "Entrada 2", CameraDirection.ENTRY))));
        assertThrows(ResponseStatusException.class, () -> service.replace(List.of(
                request("1", "Entrada", CameraDirection.ENTRY),
                request("2", "Saída", CameraDirection.EXIT),
                request("3", "Extra", CameraDirection.ENTRY))));

        verify(cameraRepository, never()).deleteAllInBatch();
        verify(cameraRepository, never()).saveAllAndFlush(anyList());
    }

    private static CameraConfiguration camera(Long id, String deviceId, String label, CameraDirection direction) {
        CameraConfiguration camera = new CameraConfiguration(deviceId, label, direction, Instant.now());
        ReflectionTestUtils.setField(camera, "id", id);
        return camera;
    }

    private static CameraRequests.CameraRequest request(String deviceId, String label, CameraDirection direction) {
        return new CameraRequests.CameraRequest(deviceId, label, direction);
    }
}
