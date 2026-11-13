package com.rostoid.backend.event;

import com.rostoid.backend.common.ApiViews;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CamerasController {
    private final CameraConfigurationsService service;

    public CamerasController(CameraConfigurationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApiViews.CameraView> list() {
        return service.list();
    }

    @PutMapping
    public List<ApiViews.CameraView> replace(@Valid @RequestBody CameraRequests.ReplaceCamerasRequest request) {
        return service.replace(request.cameras());
    }
}
