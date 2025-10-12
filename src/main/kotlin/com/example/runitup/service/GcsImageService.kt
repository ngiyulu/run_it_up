package com.example.runitup.service

import com.example.runitup.model.Gym
import com.example.runitup.model.User
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class GcsImageService(
    private val storage: Storage,
    private val props: com.example.runitup.web.rest.v1.dto.GcsProps
) {
    private val allowed = setOf(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        "image/webp",
        "image/heic"
    )

    data class UploadResult(
        val objectName: String,
        val url: String,
        val contentType: String
    )
    @Value("\${gcs.bucket}") private val bucketName: String = ""

    fun uploadProfileImage(
        user: User,
        jpegBytes: ByteArray
    ): UploadResult? {
        val objectName = "uploads/profile/${user.id}.jpeg"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
            .setContentType(MediaType.IMAGE_JPEG_VALUE)
            .build()

        return try {
            // Simple upload (fine up to ~10–20 MB). For larger, see streaming version below.
            storage.create(blobInfo, jpegBytes)

            val url = "https://storage.googleapis.com/${bucketName}/$objectName"
             UploadResult(objectName, url, MediaType.IMAGE_JPEG_VALUE)
        }
        catch (ex: Exception){
            println(ex)
            null
        }

    }

    fun delete(objectName: String): Boolean {
        return storage.delete(BlobId.of(props.bucket, objectName))
    }

    // --- Optional: streaming upload for very large files ---
     fun uploadStreaming(file: MultipartFile, signed: Boolean = true, ttlMinutes: Long = 15): UploadResult {
         val contentType = file.contentType ?: error("Missing content type")
         val objectName = "uploads/images/${UUID.randomUUID()}"
         val blobInfo = BlobInfo.newBuilder(BlobId.of(props.bucket, objectName))
             .setContentType(contentType).build()
         storage.writer(blobInfo).use { writer ->
             java.nio.channels.Channels.newOutputStream(writer).use { out ->
                 file.inputStream.use { it.copyTo(out) }
             }
         }
         val url = if (signed) {
             storage.signUrl(blobInfo, ttlMinutes, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature()).toString()
         } else {
             "https://storage.googleapis.com/${props.bucket}/$objectName"
         }
         return UploadResult(objectName, url, contentType)
     }

    fun uploadGymImage(
        gym: Gym,
        jpegBytes: ByteArray
    ): UploadResult? {
        val objectName = "gym/profile/${gym.id}.jpeg"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
            .setContentType(MediaType.IMAGE_JPEG_VALUE)
            .build()

        return try {
            storage.create(blobInfo, jpegBytes)
            val url = "https://storage.googleapis.com/${bucketName}/$objectName"
            UploadResult(objectName, url, MediaType.IMAGE_JPEG_VALUE)
        }
        catch (ex: Exception){
            println(ex)
            null
        }

    }



}