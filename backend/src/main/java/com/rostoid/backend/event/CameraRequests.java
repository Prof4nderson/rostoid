package com.rostoid.backend.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class CameraRequests {
    private CameraRequests() {}

    public record ReplaceCamerasRequest(
            @NotNull @Size(max = 2) List<@Valid CameraRequest> cameras) {}

    public record CameraRequest(
            @NotBlank @Size(max = 512) String deviceId,
            @NotBlank @Size(max = 255) String label,
            @NotNull CameraDirection direction) {}
}
