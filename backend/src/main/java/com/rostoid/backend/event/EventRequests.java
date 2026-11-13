package com.rostoid.backend.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class EventRequests {
    private EventRequests() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecognizeRequest(@NotNull @Positive Long cameraId) {}
}
