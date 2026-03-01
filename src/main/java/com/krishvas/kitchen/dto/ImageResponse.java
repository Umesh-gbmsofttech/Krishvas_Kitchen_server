package com.krishvas.kitchen.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ImageResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private Long size;
    private String referenceType;
    private Long referenceId;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
