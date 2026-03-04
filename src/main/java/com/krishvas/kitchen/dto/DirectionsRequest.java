package com.krishvas.kitchen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DirectionsRequest {

    @NotNull
    @JsonAlias("originLat")
    private Double fromLat;

    @NotNull
    @JsonAlias("originLng")
    private Double fromLng;

    @NotNull
    @JsonAlias("destinationLat")
    private Double toLat;

    @NotNull
    @JsonAlias("destinationLng")
    private Double toLng;
}
