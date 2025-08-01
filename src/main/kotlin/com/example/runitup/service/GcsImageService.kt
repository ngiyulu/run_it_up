import com.example.runitup.dto.GcsProps
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class GcsImageService(
    private val storage: Storage,
    private val props: GcsProps
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
        val contentType: String,
        val sizeBytes: Long
    )

    fun upload(
        file: MultipartFile,
        signed: Boolean = true,
        ttlMinutes: Long = 15
    ): UploadResult {
        require(!file.isEmpty) { "Empty file" }
        val contentType = file.contentType ?: error("Missing content type")
        require(contentType in allowed) { "Unsupported content type: $contentType" }

        val ext = when (contentType) {
            MediaType.IMAGE_PNG_VALUE -> ".png"
            MediaType.IMAGE_JPEG_VALUE -> ".jpg"
            "image/webp" -> ".webp"
            "image/heic" -> ".heic"
            else -> ""
        }

        val objectName = "uploads/images/${UUID.randomUUID()}$ext"
        val blobInfo = BlobInfo.newBuilder(BlobId.of(props.bucket, objectName))
            .setContentType(contentType)
            .build()

        // Simple upload (fine up to ~10–20 MB). For larger, see streaming version below.
        storage.create(blobInfo, file.bytes)

        val url = if (signed) {
            storage.signUrl(
                blobInfo,
                ttlMinutes,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
            ).toString()
        } else {
            "https://storage.googleapis.com/${props.bucket}/$objectName"
        }

        return UploadResult(objectName, url, contentType, file.size)
    }

    fun delete(objectName: String): Boolean {
        return storage.delete(BlobId.of(props.bucket, objectName))
    }

    // --- Optional: streaming upload for very large files ---
    // fun uploadStreaming(file: MultipartFile, signed: Boolean = true, ttlMinutes: Long = 15): UploadResult {
    //     val contentType = file.contentType ?: error("Missing content type")
    //     val objectName = "uploads/images/${UUID.randomUUID()}"
    //     val blobInfo = BlobInfo.newBuilder(BlobId.of(props.bucket, objectName))
    //         .setContentType(contentType).build()
    //     storage.writer(blobInfo).use { writer ->
    //         java.nio.channels.Channels.newOutputStream(writer).use { out ->
    //             file.inputStream.use { it.copyTo(out) }
    //         }
    //     }
    //     val url = if (signed) {
    //         storage.signUrl(blobInfo, ttlMinutes, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature()).toString()
    //     } else {
    //         "https://storage.googleapis.com/${props.bucket}/$objectName"
    //     }
    //     return UploadResult(objectName, url, contentType, file.size)
    // }
}