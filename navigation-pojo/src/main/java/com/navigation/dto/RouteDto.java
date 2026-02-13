package com.navigation.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteDto {
    private Integer startScenicId;
    private Integer endScenicId;
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;
    private Integer userId;
    private List<Waypoint> waypoints; // 途经点

    @Data
    public static class Waypoint {
        private Double lat;
        private Double lng;
    }
}