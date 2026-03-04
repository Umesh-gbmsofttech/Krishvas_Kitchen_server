package com.krishvas.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class DirectionsResponse {

    private List<RoutePoint> route;
    private List<List<Double>> geometry;
    private Map<String, Object> geoJson;
    private double distanceMeters;
    private double durationSeconds;
    private long etaSeconds;
    private String etaText;
    private List<RouteStep> steps;

    public DirectionsResponse(List<RoutePoint> route, double distanceMeters, double durationSeconds, long etaSeconds,
            String etaText, List<RouteStep> steps) {
        this.route = route;
        this.geometry = toGeometry(route);
        this.geoJson = Map.of("type", "LineString", "coordinates", this.geometry);
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.etaSeconds = etaSeconds;
        this.etaText = etaText;
        this.steps = steps;
    }

    private static List<List<Double>> toGeometry(List<RoutePoint> route) {
        List<List<Double>> points = new ArrayList<>();
        if (route == null) {
            return points;
        }
        for (RoutePoint point : route) {
            points.add(List.of(point.getLongitude(), point.getLatitude()));
        }
        return points;
    }

    @Data
    @AllArgsConstructor
    public static class RoutePoint {
        private double latitude;
        private double longitude;
    }

    @Data
    @AllArgsConstructor
    public static class RouteStep {
        private String instruction;
        private double distanceMeters;
        private double durationSeconds;
        private Integer maneuverType;
    }
}
