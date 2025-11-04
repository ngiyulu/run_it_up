// src/test/kotlin/com/example/runitup/mobile/service/GcsImageServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.dto.GcsProps
import com.google.cloud.WriteChannel
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class GcsImageServiceTest {

    private val storage = mockk<Storage>(relaxed = true)
    private val props = GcsProps(
        projectId = "proj",
        bucket = "test-bucket",
        credentialsPath = null
    )

    private val service = GcsImageService(storage, props).apply {
        setPrivate("bucketName", "test-bucket")
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // ---- uploadProfileImage ----
    @Test
    fun `uploadProfileImage uploads bytes and returns public URL`() {
        val user = User(id = "u1")
        val bytes = "jpegdata".toByteArray()

        val blobSlot = slot<BlobInfo>()
        every { storage.create(capture(blobSlot), bytes) } returns mockk<Blob>(relaxed = true)

        val res = service.uploadProfileImage(user, bytes)

        assertThat(res).isNotNull()
        assertThat(res!!.objectName).isEqualTo("uploads/profile/u1.jpeg")
        assertThat(res.url).isEqualTo("https://storage.googleapis.com/test-bucket/uploads/profile/u1.jpeg")
        assertThat(res.contentType).isEqualTo(MediaType.IMAGE_JPEG_VALUE)

        val info = blobSlot.captured
        assertThat(info.blobId.bucket).isEqualTo("test-bucket")
        assertThat(info.blobId.name).isEqualTo("uploads/profile/u1.jpeg")
        assertThat(info.contentType).isEqualTo(MediaType.IMAGE_JPEG_VALUE)

        verify(exactly = 1) { storage.create(any<BlobInfo>(), bytes) }
    }

    @Test
    fun `uploadProfileImage returns null on exception`() {
        val user = User(id = "u2")
        val bytes = ByteArray(4)
        every { storage.create(any<BlobInfo>(), any<ByteArray>()) } throws RuntimeException("boom")

        val res = service.uploadProfileImage(user, bytes)
        assertThat(res).isNull()
    }

    // ---- delete ----
    @Test
    fun `delete calls storage with props bucket`() {
        val objectName = "some/file.jpeg"
        val idSlot = slot<BlobId>()

        every { storage.delete(capture(idSlot)) } returns true
        val ok = service.delete(objectName)
        assertThat(ok).isTrue()

        val bid = idSlot.captured
        assertThat(bid.bucket).isEqualTo("test-bucket")
        assertThat(bid.name).isEqualTo(objectName)
    }

    // ---- uploadStreaming ----
    @Test
    fun `uploadStreaming signed true - streams to writer and returns signed URL`() {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "image/png"
        every { file.inputStream } returns ByteArrayInputStream("PNG".toByteArray())

        val channel = mockk<WriteChannel>(relaxed = true)
        every { channel.write(any()) } answers {
            val buf = firstArg<ByteBuffer>()
            val n = buf.remaining()
            buf.position(buf.limit())
            n
        }
        every { channel.isOpen } returns true
        every { channel.close() } just Runs
        every { storage.writer(any<BlobInfo>()) } returns channel

        val infoSlot = slot<BlobInfo>()
        every {
            storage.signUrl(capture(infoSlot), 15, TimeUnit.MINUTES, any<Storage.SignUrlOption>())
        } returns URL("https://signed.example.com/obj")

        val res = service.uploadStreaming(file, signed = true, ttlMinutes = 15)

        assertThat(res.objectName).isNotBlank()
        assertThat(res.url).isEqualTo("https://signed.example.com/obj")
        assertThat(res.contentType).isEqualTo("image/png")

        val info = infoSlot.captured
        assertThat(info.blobId.bucket).isEqualTo("test-bucket")
        assertThat(info.blobId.name).startsWith("uploads/images/")
        assertThat(info.contentType).isEqualTo("image/png")

        verify(exactly = 1) { storage.writer(any<BlobInfo>()) }
        verify(exactly = 1) { storage.signUrl(any<BlobInfo>(), 15, TimeUnit.MINUTES, any()) }
        verify(atLeast = 1) { channel.close() }  // allow both stream + channel closes
    }

    @Test
    fun `uploadStreaming signed false - returns public URL`() {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "image/png"
        every { file.inputStream } returns ByteArrayInputStream("PNG".toByteArray())

        val channel = mockk<WriteChannel>(relaxed = true)
        every { channel.write(any()) } answers {
            val buf = firstArg<ByteBuffer>()
            val n = buf.remaining()
            buf.position(buf.limit())
            n
        }
        every { channel.isOpen } returns true
        every { channel.close() } just Runs
        val infoSlotForWriter = slot<BlobInfo>()
        every { storage.writer(capture(infoSlotForWriter)) } returns channel

        val res = service.uploadStreaming(file, signed = false, ttlMinutes = 10)

        val info = infoSlotForWriter.captured
        val expectedUrl = "https://storage.googleapis.com/${info.blobId.bucket}/${info.blobId.name}"

        assertThat(res.contentType).isEqualTo("image/png")
        assertThat(res.url).isEqualTo(expectedUrl)
        assertThat(info.blobId.bucket).isEqualTo("test-bucket")
        assertThat(info.blobId.name).startsWith("uploads/images/")
        assertThat(info.contentType).isEqualTo("image/png")

        verify(exactly = 0) { storage.signUrl(any<BlobInfo>(), any(), any(), any<Storage.SignUrlOption>()) }
        verify(atLeast = 1) { channel.close() }  // tolerate double close
    }

    // ---- uploadUserWaiverPdf ----
    @Test
    fun `uploadUserWaiverPdf uploads, sets headers, and returns signed URL`() {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns MediaType.APPLICATION_PDF_VALUE
        every { file.inputStream } returns ByteArrayInputStream("%PDF-1.7".toByteArray())

        val channel = mockk<WriteChannel>(relaxed = true)
        every { channel.write(any()) } answers {
            val buf = firstArg<ByteBuffer>()
            val n = buf.remaining()
            buf.position(buf.limit())
            n
        }
        every { channel.isOpen } returns true
        every { channel.close() } just Runs
        every { storage.writer(any<BlobInfo>()) } returns channel

        val infoSlot = slot<BlobInfo>()
        every {
            storage.signUrl(capture(infoSlot), 60, TimeUnit.MINUTES, any<Storage.SignUrlOption>())
        } returns URL("https://signed.example.com/waiver")

        val user = User(id = "user-42")
        val res = service.uploadUserWaiverPdf(user, file, signed = true, ttlMinutes = 60)

        assertThat(res).isNotNull()
        assertThat(res!!.objectName).isEqualTo("uploads/waivers/user-42/waiver.pdf")
        assertThat(res.url).isEqualTo("https://signed.example.com/waiver")
        assertThat(res.contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)

        val info = infoSlot.captured
        assertThat(info.blobId.bucket).isEqualTo("test-bucket")
        assertThat(info.blobId.name).isEqualTo("uploads/waivers/user-42/waiver.pdf")
        assertThat(info.contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(info.contentDisposition).contains("inline")
        assertThat(info.contentDisposition).contains("waiver-user-42.pdf")

        verify(exactly = 1) { storage.writer(any<BlobInfo>()) }
        verify(exactly = 1) { storage.signUrl(any<BlobInfo>(), 60, TimeUnit.MINUTES, any()) }
        verify(atLeast = 1) { channel.close() } // tolerate double close
    }

    @Test
    fun `uploadUserWaiverPdf rejects non-PDF`() {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "image/png"

        val user = User(id = "u9")

        val ex = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            service.uploadUserWaiverPdf(user, file, signed = false)
        }
        assertThat(ex.message).contains("Only PDF files are allowed")
        verify(exactly = 0) { storage.writer(any<BlobInfo>()) }
        verify(exactly = 0) { storage.signUrl(any<BlobInfo>(), any(), any(), any<Storage.SignUrlOption>()) }
    }

    // --- helper to set private field ---
    private fun Any.setPrivate(field: String, value: Any?) {
        val f = this.javaClass.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }
}
