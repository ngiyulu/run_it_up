import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

@Configuration
@Order(1)
class RequestHeaderLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Log request headers (mask sensitive)
        val reqHeaders = Collections.list(request.headerNames).associateWith { name ->
            val values = Collections.list(request.getHeaders(name))
            when (name.lowercase()) {
                "authorization", "cookie", "set-cookie" -> "***"
                else -> values.joinToString(", ")
            }
        }
        log.info("→ {} {} headers={}", request.method, request.requestURI, reqHeaders)

        filterChain.doFilter(request, response)

    }
}
