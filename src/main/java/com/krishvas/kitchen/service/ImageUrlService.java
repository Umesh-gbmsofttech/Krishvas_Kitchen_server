package com.krishvas.kitchen.service;

import org.springframework.stereotype.Service;

@Service
public class ImageUrlService {
    public String toImageUrl(Long imageId) {
        if (imageId == null) return null;
        return "/api/images/" + imageId;
    }
}
