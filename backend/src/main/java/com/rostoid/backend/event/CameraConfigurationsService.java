package com.rostoid.backend.event;

import com.rostoid.backend.common.ApiViews;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CameraConfigurationsService {
    private final CameraConfigurationRepository cameras;

    public CameraConfigurationsService(CameraConfigurationRepository cameras) {
        this.cameras = cameras;
    }

    @Transactional(readOnly = true)
    public List<ApiViews.CameraView> list() {
        return cameras.findAllByOrderByIdAsc().stream().map(ApiViews.CameraView::from).toList();
    }

    @Transactional
    public List<ApiViews.CameraView> replace(List<CameraRequests.CameraRequest> requested) {
        validate(requested);
        Instant updatedAt = Instant.now();
        List<CameraConfiguration> existing = cameras.findAll();
        Map<String, CameraConfiguration> byDeviceId = new HashMap<>();
        existing.forEach(camera -> byDeviceId.put(camera.getDeviceId(), camera));

        List<CameraConfiguration> retained = new ArrayList<>();
        Set<String> retainedDeviceIds = new HashSet<>();
        for (CameraRequests.CameraRequest camera : requested) {
            CameraConfiguration configuration = byDeviceId.get(camera.deviceId());
            if (configuration == null) {
                configuration = new CameraConfiguration(camera.deviceId(), camera.label(), camera.direction(), updatedAt);
            } else {
                configuration.update(camera.label(), camera.direction(), updatedAt);
            }
            retained.add(configuration);
            retainedDeviceIds.add(camera.deviceId());
        }

        // Configuration updates preserve IDs; only omitted devices are removed. Event rows
        // keep their camera label/direction snapshots and have no FK to these configurations.
        List<CameraConfiguration> removed = existing.stream()
                .filter(camera -> !retainedDeviceIds.contains(camera.getDeviceId())).toList();
        if (!removed.isEmpty()) cameras.deleteAll(removed);
        return cameras.saveAllAndFlush(retained).stream().map(ApiViews.CameraView::from).toList();
    }

    @Transactional(readOnly = true)
    public CameraConfiguration requireConfigured(Long cameraId) {
        if (cameraId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cameraId é obrigatório");
        }
        return cameras.findById(cameraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Câmera não configurada"));
    }

    private static void validate(List<CameraRequests.CameraRequest> requested) {
        if (requested == null || requested.size() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure no máximo duas câmeras");
        }
        Set<String> deviceIds = new HashSet<>();
        Set<CameraDirection> directions = new HashSet<>();
        for (CameraRequests.CameraRequest camera : requested) {
            if (camera == null || camera.deviceId() == null || camera.deviceId().isBlank()
                    || camera.deviceId().length() > 512 || camera.label() == null || camera.label().isBlank()
                    || camera.label().length() > 255 || camera.direction() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuração de câmera inválida");
            }
            if (!deviceIds.add(camera.deviceId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId deve ser distinto");
            }
            directions.add(camera.direction());
        }
        if (requested.size() == 2 && directions.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duas câmeras devem ter sentidos ENTRY e EXIT distintos");
        }
    }
}
