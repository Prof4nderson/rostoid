package com.rostoid.backend.event;

import com.rostoid.backend.common.ApiViews;
import com.rostoid.backend.people.Person;
import com.rostoid.backend.people.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class EventsService {
    private final PersonRepository people;
    private final RecognitionEventRepository events;
    private final CameraConfigurationsService cameras;

    public EventsService(PersonRepository people, RecognitionEventRepository events,
                         CameraConfigurationsService cameras) {
        this.people = people;
        this.events = events;
        this.cameras = cameras;
    }

    @Transactional
    public ApiViews.CreatedEventView recognize(Long personId, Long cameraId) {
        Person person = people.findForUpdate(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));
        CameraConfiguration camera = cameras.requireConfigured(cameraId);
        Instant now = Instant.now();
        RecognitionEvent latest = events.findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
                personId, camera.getId(), camera.getDirection()).orElse(null);
        if (latest != null && latest.getRecognizedAt().isAfter(now.minusSeconds(30))) {
            return ApiViews.CreatedEventView.from(latest, false);
        }
        RecognitionEvent event = new RecognitionEvent(person, now, camera.getId(), camera.getLabel(), camera.getDirection());
        return ApiViews.CreatedEventView.from(events.save(event), true);
    }

    @Transactional(readOnly = true)
    public List<ApiViews.EventView> list(int requestedLimit) {
        int limit = Math.max(1, Math.min(500, requestedLimit));
        return events.findAllByOrderByRecognizedAtDesc().stream().limit(limit)
                .map(ApiViews.EventView::from).toList();
    }
}
