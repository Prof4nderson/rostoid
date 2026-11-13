package com.rostoid.backend.people;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class PeopleRequests {
    private PeopleRequests() {}

    public record CreatePersonRequest(
            @NotBlank @Size(max = 1_048_576) String faceCiphertext,
            @NotBlank @Size(max = 512) String faceIv,
            @NotNull @Positive Long cameraId,
            @NotNull @AssertTrue Boolean consentConfirmed) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 1_048_576) String profileCiphertext,
            @NotBlank @Size(max = 512) String profileIv) {}
}
