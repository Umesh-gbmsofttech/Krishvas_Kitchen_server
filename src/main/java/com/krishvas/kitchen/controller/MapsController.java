package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.DirectionsRequest;
import com.krishvas.kitchen.dto.DirectionsResponse;
import com.krishvas.kitchen.dto.MapConfigResponse;
import com.krishvas.kitchen.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
public class MapsController {

    private final RoutingService routingService;

    @Value("${api.keys.map.tiler:}")
    private String mapTilerKey;

    public MapsController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/config")
    public ResponseEntity<MapConfigResponse> config() {
        if (mapTilerKey == null || mapTilerKey.isBlank()) {
            return ResponseEntity.ok(new MapConfigResponse(null));
        }
        String template = "https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=" + mapTilerKey;
        return ResponseEntity.ok(new MapConfigResponse(template));
    }

    @PostMapping("/directions")
    public ResponseEntity<DirectionsResponse> directions(@Valid @RequestBody DirectionsRequest request) {
        return ResponseEntity.ok(routingService.getDirections(request));
    }
}
