// src/test/kotlin/com/example/runitup/mobile/service/ImageServiceTest.kt
package com.example.runitup.mobile.service

import io.mockk.*
import net.coobird.thumbnailator.Thumbnails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.abs

class ImageServiceTest {

    private val service = ImageService()

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // ---- validate() ----

    @Test
    fun `validate passes for JPEG under 10MB`() {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns false
        every { file.contentType } returns MediaType.IMAGE_JPEG_VALUE
        every { file.size } returns 1024 * 500 // 500 KB

        service.validate(file)
    }

    @Test
    fun `validate rejects empty file`() {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns true
        every { file.contentType } returns MediaType.IMAGE_JPEG_VALUE
        every { file.size } returns 1

        val ex = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.validate(file)
        }
        assertThat(ex.message).contains("No file uploaded")
    }

    @Test
    fun `validate rejects invalid content type`() {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns false
        every { file.contentType } returns "application/pdf"
        every { file.size } returns 1000

        val ex = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.validate(file)
        }
        assertThat(ex.message).contains("Only JPEG or PNG images are allowed")
    }

    @Test
    fun `validate rejects too large file`() {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns false
        every { file.contentType } returns MediaType.IMAGE_PNG_VALUE
        every { file.size } returns 11L * 1024 * 1024 // 11 MB

        val ex = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.validate(file)
        }
        assertThat(ex.message).contains("File too large")
    }

    // ---- resizeToAvatarJpeg() ----
    @Test
    fun `resizeToAvatarJpeg delegates to Thumbnailator and returns bytes`() {
        // Fake image stream
        val file = mockk<MultipartFile>()
        every { file.inputStream } returns ByteArrayInputStream(ByteArray(10))

        mockkStatic(Thumbnails::class)

        // Use the correct generic builder type for InputStream
        val builder = mockk<Thumbnails.Builder<InputStream>>()

        // Return our builder from the static factory
        every { Thumbnails.of(any<InputStream>()) } returns builder

        // Stub each method in the chain to return the SAME builder so calls are recorded individually
        every { builder.useExifOrientation(true) } returns builder
        every { builder.size(256, 256) } returns builder
        every { builder.outputFormat("jpg") } returns builder
        every { builder.outputQuality(any()) } returns builder

        // Capture the output stream and write fake bytes
        val baosSlot = slot<ByteArrayOutputStream>()
        every { builder.toOutputStream(capture(baosSlot)) } answers {
            baosSlot.captured.write("FAKEJPEG".toByteArray())
            Unit
        }

        val result = service.resizeToAvatarJpeg(file, maxSize = 256, quality = 0.9)

        // Assertions
        assertThat(String(result)).isEqualTo("FAKEJPEG")

        // Verify each method called with the expected args
        verify(exactly = 1) { Thumbnails.of(any<InputStream>()) }
        verify(exactly = 1) { builder.useExifOrientation(true) }
        verify(exactly = 1) { builder.size(256, 256) }
        verify(exactly = 1) { builder.outputFormat("jpg") }
        verify(exactly = 1) { builder.outputQuality(match { abs(it - 0.9) < 1e-9 }) }
        verify(exactly = 1) { builder.toOutputStream(any()) }

        unmockkStatic(Thumbnails::class)
    }
}
