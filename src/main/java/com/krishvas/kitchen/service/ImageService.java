package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.ImageResponse;
import com.krishvas.kitchen.entity.Image;
import com.krishvas.kitchen.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final ImageRepository imageRepository;
    private final ImageUrlService imageUrlService;

    @Value("${app.image.max-size-bytes:10485760}")
    private long maxImageSizeBytes;

    @Transactional
    public ImageResponse upload(MultipartFile file, String referenceType, Long referenceId) {
        validateFile(file);
        Image image = new Image();
        image.setFileName(resolveFileName(file));
        image.setContentType(normalizedContentType(file));
        image.setSize(file.getSize());
        image.setReferenceType(referenceType == null || referenceType.isBlank() ? "MENU_ITEM" : referenceType.trim().toUpperCase(Locale.ROOT));
        image.setReferenceId(referenceId == null ? 0L : referenceId);
        image.setImageData(toBlob(file));
        Image saved = imageRepository.save(image);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long id) {
        Image image = imageRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Image not found"));
        try {
            Blob blob = image.getImageData();
            long length = blob.length();
            InputStream inputStream = blob.getBinaryStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            headers.setContentDisposition(ContentDisposition.inline().filename(image.getFileName()).build());
            headers.setContentLength(length);
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read image data");
        }
    }

    @Transactional(readOnly = true)
    public ImageResponse metadata(Long id) {
        Image image = imageRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Image not found"));
        return toResponse(image);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new IllegalArgumentException("File exceeds max size");
        }
        String contentType = normalizedContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type");
        }
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private String resolveFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        return (originalName == null || originalName.isBlank()) ? "image" : originalName;
    }

    private Blob toBlob(MultipartFile file) {
        try {
            return new SerialBlob(file.getBytes());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process image");
        }
    }

    private ImageResponse toResponse(Image image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setFileName(image.getFileName());
        response.setContentType(image.getContentType());
        response.setSize(image.getSize());
        response.setReferenceType(image.getReferenceType());
        response.setReferenceId(image.getReferenceId());
        response.setImageUrl(imageUrlService.toImageUrl(image.getId()));
        response.setCreatedAt(image.getCreatedAt());
        response.setUpdatedAt(image.getUpdatedAt());
        return response;
    }
}
