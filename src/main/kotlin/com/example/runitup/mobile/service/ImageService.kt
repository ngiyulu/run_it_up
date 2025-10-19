package com.example.runitup.mobile.service


import net.coobird.thumbnailator.Thumbnails
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream

@Service
class ImageService {

    private val allowed = setOf(  MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        "image/webp",
        "image/heic"
    ) // keep this tight; HEIC/WEBP need extra plugins

    fun validate(file: MultipartFile) {
        require(!file.isEmpty) { "No file uploaded" }
        require(file.contentType in allowed) { "Only JPEG or PNG images are allowed" }
        // Optionally also sniff magic bytes if you want to be stricter
        require(file.size <= 10 * 1024 * 1024) { "File too large (max 10MB)" }
    }

    /**
     * Resize to a square thumbnail with max side = [maxSize].
     * Output: JPEG with [quality] (0.0–1.0).
     */
    fun resizeToAvatarJpeg(file: MultipartFile, maxSize: Int = 512, quality: Double = 0.85): ByteArray {
        val baos = ByteArrayOutputStream()
        Thumbnails.of(file.inputStream)
            .useExifOrientation(true)   // auto-rotate based on EXIF for JPEGs
            .size(maxSize, maxSize)     // keeps aspect ratio, fits within maxSize box
            .outputFormat("jpg")
            .outputQuality(quality)
            .toOutputStream(baos)
        return baos.toByteArray()
    }
}
