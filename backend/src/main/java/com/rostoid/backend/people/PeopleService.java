package com.rostoid.backend.people;

import com.rostoid.backend.common.ApiViews;
import com.rostoid.backend.event.CameraConfiguration;
import com.rostoid.backend.event.CameraConfigurationsService;
import com.rostoid.backend.event.RecognitionEvent;
import com.rostoid.backend.event.RecognitionEventRepository;
import com.rostoid.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class PeopleService {
    private final PersonRepository people;
    private final RecognitionEventRepository events;
    private final CameraConfigurationsService cameras;

    public PeopleService(PersonRepository people, RecognitionEventRepository events,
                         CameraConfigurationsService cameras) {
        this.people = people;
        this.events = events;
        this.cameras = cameras;
    }

    @Transactional
    public ApiViews.PersonView create(PeopleRequests.CreatePersonRequest request) {
        CameraConfiguration camera = cameras.requireConfigured(request.cameraId());
        Instant now = Instant.now();
        Person person = people.saveAndFlush(new Person(request.faceCiphertext(), request.faceIv(), now, AuthenticatedUser.username(), AuthenticatedUser.username()));
        person.assignPublicId();
        events.save(new RecognitionEvent(person, now, camera.getId(), camera.getLabel(), camera.getDirection()));
        return ApiViews.PersonView.from(person, now);
    }

    @Transactional(readOnly = true)
    public List<ApiViews.PersonView> list() {
        return people.findAllByOwnerUsernameOrderByIdAsc(AuthenticatedUser.username()).stream()
                .map(person -> ApiViews.PersonView.from(person, events
                        .findTopByPerson_IdOrderByRecognizedAtDesc(person.getId())
                        .map(e -> e.getRecognizedAt()).orElse(null)))
                .toList();
    }

    @Transactional
    public ApiViews.PersonView updateProfile(Long id, PeopleRequests.UpdateProfileRequest request) {
        Person person = people.findByIdAndOwnerUsername(id, AuthenticatedUser.username()).orElseThrow(() -> notFound("Pessoa não encontrada"));
        person.updateProfile(request.profileCiphertext(), request.profileIv());
        Instant lastRecognizedAt = events.findTopByPerson_IdOrderByRecognizedAtDesc(id)
                .map(e -> e.getRecognizedAt()).orElse(null);
        return ApiViews.PersonView.from(person, lastRecognizedAt);
    }

    @Transactional
    public void delete(Long id) {
        Person person = people.findByIdAndOwnerUsername(id, AuthenticatedUser.username()).orElseThrow(() -> notFound("Pessoa não encontrada"));
        people.delete(person);
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
