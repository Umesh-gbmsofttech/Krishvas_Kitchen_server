package com.krishvas.kitchen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishvas.kitchen.dto.DirectionsRequest;
import com.krishvas.kitchen.dto.DirectionsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RoutingService {

    private static final String PUBLIC_OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    @Value("${api.keys.map.ors:}")
    private String orsKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RoutingService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(6))
            .setReadTimeout(Duration.ofSeconds(12))
            .build();
        this.objectMapper = objectMapper;
    }

    public DirectionsResponse getDirections(DirectionsRequest request) {
        validateRequest(request);
        if (orsKey != null && !orsKey.isBlank()) {
            try {
                return fetchRouteFromORS(request);
            } catch (Exception ignored) {
                // fallback
            }
        }
        return fetchRouteFromPublicOsrm(request);
    }

    private DirectionsResponse fetchRouteFromORS(DirectionsRequest request) {
        String url = String.format(
            Locale.US,
            "%s?api_key=%s&start=%.6f,%.6f&end=%.6f,%.6f",
            ORS_URL,
            orsKey,
            request.getFromLng(),
            request.getFromLat(),
            request.getToLng(),
            request.getToLat()
        );
        String body = restTemplate.getForObject(url, String.class);
        return parseOrsDirections(body);
    }

    private DirectionsResponse parseOrsDirections(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) throw new IllegalArgumentException("No routes found");

            JsonNode feature = features.path(0);
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(feature.path("geometry").path("coordinates"));

            JsonNode summary = feature.path("properties").path("summary");
            double distanceMeters = summary.path("distance").asDouble(0);
            double durationSeconds = summary.path("duration").asDouble(0);

            List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
            JsonNode segments = feature.path("properties").path("segments");
            if (segments.isArray() && !segments.isEmpty()) {
                JsonNode stepsNode = segments.path(0).path("steps");
                if (stepsNode.isArray()) {
                    for (JsonNode step : stepsNode) {
                        steps.add(new DirectionsResponse.RouteStep(
                            step.path("instruction").asText("Continue"),
                            step.path("distance").asDouble(0),
                            step.path("duration").asDouble(0),
                            step.path("type").asInt()
                        ));
                    }
                }
            }

            return toResponse(routePoints, distanceMeters, durationSeconds, steps);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse route response");
        }
    }

    private DirectionsResponse fetchRouteFromPublicOsrm(DirectionsRequest request) {
        String start = String.format(Locale.US, "%.6f,%.6f", request.getFromLng(), request.getFromLat());
        String end = String.format(Locale.US, "%.6f,%.6f", request.getToLng(), request.getToLat());
        String url = PUBLIC_OSRM_URL + start + ";" + end + "?overview=full&geometries=geojson&steps=true";
        String body = restTemplate.getForObject(url, String.class);
        return parseOsrmDirections(body);
    }

    private DirectionsResponse parseOsrmDirections(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) throw new IllegalArgumentException("No routes found");

            JsonNode firstRoute = routes.path(0);
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(firstRoute.path("geometry").path("coordinates"));
            if (routePoints.size() < 2) throw new IllegalArgumentException("Route geometry unavailable");

            JsonNode firstLeg = firstRoute.path("legs").isArray() && !firstRoute.path("legs").isEmpty()
                ? firstRoute.path("legs").path(0) : null;

            double distanceMeters = firstLeg != null
                ? firstLeg.path("distance").asDouble(firstRoute.path("distance").asDouble(0))
                : firstRoute.path("distance").asDouble(0);
            double durationSeconds = firstLeg != null
                ? firstLeg.path("duration").asDouble(firstRoute.path("duration").asDouble(0))
                : firstRoute.path("duration").asDouble(0);

            List<DirectionsResponse.RouteStep> steps = parseOsrmSteps(firstLeg == null ? null : firstLeg.path("steps"));
            return toResponse(routePoints, distanceMeters, durationSeconds, steps);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Routing provider unavailable");
        }
    }

    private List<DirectionsResponse.RoutePoint> parseCoordinates(JsonNode coords) {
        List<DirectionsResponse.RoutePoint> route = new ArrayList<>();
        if (!coords.isArray()) return route;
        for (JsonNode coordinate : coords) {
            if (!coordinate.isArray() || coordinate.size() < 2) continue;
            route.add(new DirectionsResponse.RoutePoint(
                coordinate.path(1).asDouble(),
                coordinate.path(0).asDouble()
            ));
        }
        return route;
    }

    private List<DirectionsResponse.RouteStep> parseOsrmSteps(JsonNode stepsNode) {
        List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
        if (stepsNode == null || !stepsNode.isArray()) return steps;
        for (JsonNode step : stepsNode) {
            JsonNode maneuver = step.path("maneuver");
            String instruction = maneuver.path("instruction").asText(null);
            if (instruction == null || instruction.isBlank()) {
                instruction = step.path("name").asText("Continue");
            }
            steps.add(new DirectionsResponse.RouteStep(
                instruction,
                step.path("distance").asDouble(0),
                step.path("duration").asDouble(0),
                null
            ));
        }
        return steps;
    }

    private DirectionsResponse toResponse(
        List<DirectionsResponse.RoutePoint> route,
        double distanceMeters,
        double durationSeconds,
        List<DirectionsResponse.RouteStep> steps
    ) {
        long etaSeconds = Math.round(durationSeconds);
        return new DirectionsResponse(route, distanceMeters, durationSeconds, etaSeconds, formatEta(etaSeconds), steps);
    }

    private String formatEta(long etaSeconds) {
        long mins = Math.max(1, Math.round(etaSeconds / 60.0));
        if (mins < 60) return mins + " min";
        long hours = mins / 60;
        long remMins = mins % 60;
        if (remMins == 0) return hours + " hr";
        return hours + " hr " + remMins + " min";
    }

    private void validateRequest(DirectionsRequest request) {
        if (request == null
            || request.getFromLat() == null
            || request.getFromLng() == null
            || request.getToLat() == null
            || request.getToLng() == null) {
            throw new IllegalArgumentException("Origin and destination coordinates are required.");
        }
    }
}
