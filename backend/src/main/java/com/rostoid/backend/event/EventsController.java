package com.rostoid.backend.event;

import com.rostoid.backend.common.ApiViews;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventsController {
    private final EventsService service;

    public EventsController(EventsService service) {
        this.service = service;
    }

    @PostMapping("/people/{id}/events")
    public ApiViews.CreatedEventView recognize(@PathVariable("id") Long personId,
                                               @Valid @RequestBody EventRequests.RecognizeRequest request) {
        return service.recognize(personId, request.cameraId());
    }

    @GetMapping("/events")
    public List<ApiViews.EventView> list(@RequestParam(defaultValue = "200") int limit) {
        return service.list(limit);
    }
}
