data class ServiceError(
    val status: Int,
    val source: String,
    val message: String,
    val body: String? = null
)

data class ServiceResult<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: ServiceError? = null
) {
    companion object {
        fun <T> ok(value: T) = ServiceResult(ok = true, data = value)
        fun <T> disable() = ServiceResult<T>(ok = true, data = null)
        fun <T> err(status: Int, source: String, message: String, body: String? = null) =
            ServiceResult<T>(ok = false, error = ServiceError(status, source, message, body))
    }
}