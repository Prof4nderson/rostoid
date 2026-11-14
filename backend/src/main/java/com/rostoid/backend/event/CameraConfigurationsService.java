package com.rostoid.backend.event;
import com.rostoid.backend.common.ApiViews;
import com.rostoid.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@Service
public class CameraConfigurationsService {
    private final CameraConfigurationRepository cameras;
    public CameraConfigurationsService(CameraConfigurationRepository cameras) { this.cameras = cameras; }
    private String owner() { return AuthenticatedUser.username(); }
    @Transactional(readOnly = true) public List<ApiViews.CameraView> list() { return (owner() == null ? cameras.findAll() : cameras.findAllByOwnerUsernameOrderByIdAsc(owner())).stream().map(ApiViews.CameraView::from).toList(); }
    @Transactional public List<ApiViews.CameraView> replace(List<CameraRequests.CameraRequest> requested) {
        validate(requested); String user = owner(); Instant now = Instant.now(); List<CameraConfiguration> existing = user == null ? cameras.findAll() : cameras.findAllByOwnerUsername(user);
        Map<String, CameraConfiguration> byDevice = new HashMap<>(); existing.forEach(c -> byDevice.put(c.getDeviceId(), c));
        List<CameraConfiguration> retained = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (CameraRequests.CameraRequest input : requested) { CameraConfiguration c = byDevice.get(input.deviceId()); if (c == null) c = new CameraConfiguration(input.deviceId(), input.label(), input.direction(), now, user); else c.update(input.label(), input.direction(), now); retained.add(c); ids.add(input.deviceId()); }
        List<CameraConfiguration> removed = existing.stream().filter(c -> !ids.contains(c.getDeviceId())).toList(); if (!removed.isEmpty()) cameras.deleteAll(removed);
        return cameras.saveAllAndFlush(retained).stream().map(ApiViews.CameraView::from).toList();
    }
    @Transactional(readOnly = true) public CameraConfiguration requireConfigured(Long cameraId) { if (cameraId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cameraId é obrigatório"); return (owner() == null ? cameras.findById(cameraId) : cameras.findByIdAndOwnerUsername(cameraId, owner())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Câmera não configurada para este usuário")); }
    private static void validate(List<CameraRequests.CameraRequest> requested) { if (requested == null || requested.size() > 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure no máximo duas câmeras"); Set<String> deviceIds=new HashSet<>(); Set<CameraDirection> directions=new HashSet<>(); for (CameraRequests.CameraRequest c: requested) { if(c==null||c.deviceId()==null||c.deviceId().isBlank()||c.deviceId().length()>512||c.label()==null||c.label().isBlank()||c.label().length()>255||c.direction()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Configuração de câmera inválida"); if(!deviceIds.add(c.deviceId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"deviceId deve ser distinto"); directions.add(c.direction()); } if(requested.size()==2&&directions.size()!=2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duas câmeras devem ter sentidos ENTRY e EXIT distintos"); }
}
