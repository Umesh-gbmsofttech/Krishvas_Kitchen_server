package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.ImageResponse;
import com.krishvas.kitchen.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImageResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "referenceType", required = false) String referenceType,
        @RequestParam(value = "referenceId", required = false) Long referenceId
    ) {
        return ResponseEntity.ok(imageService.upload(file, referenceType, referenceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return imageService.download(id);
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<ImageResponse> metadata(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.metadata(id));
    }
}
