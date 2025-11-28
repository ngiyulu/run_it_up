package  com.example.runitup.mobile.service
import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.web.dto.Role
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.DeleteParticipantFromConversationModel
import com.stripe.param.PaymentIntentCancelParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class LeaveSessionService(
    private val runSessionService: RunSessionService,
    private val paymentService: PaymentService,
    private val waitListPaymentService: WaitListPaymentService,
    private val bookingDbService: BookingDbService,
    private val cacheManager: MyCacheManager,
    private val textService: TextService,
    private val messagingService: MessagingService,
    private val bookingPricingAdjuster: BookingPricingAdjuster,
    private val appScope: CoroutineScope,
    private val queueService: LightSqsService
) {

    private val logger = myLogger()

    fun cancelBooking(user: User, sessionId: String, admin: AdminUser? = null): Pair<Booking, RunSession> {
        val locale = LocaleContextHolder.getLocale().toString()
        val run = cacheManager.getRunSession(sessionId)
            ?: throw ApiRequestException(textService.getText("invalid_session_id", locale))

        if (!run.isDeletable()) {
            throw ApiRequestException(textService.getText("invalid_session_cancel", locale))
        }

        if (admin != null) {
            if (admin.role != Role.SUPER_ADMIN && run.hostedBy != admin.id) {
                throw ApiRequestException(textService.getText("unauthorized_user", locale))
            }
        }

        val booking: Booking = bookingDbService.getBooking(user.id.orEmpty(), run.id.orEmpty())
            ?: throw ApiRequestException(textService.getText("invalid_params", locale))

        if (booking.isLocked) {
            throw ApiRequestException(textService.getText("try-again", locale))
        }

        // Need to cancel hold payment?
        if (!run.isSessionFree()) {
            if (run.status != RunStatus.PENDING) {
                val res = bookingPricingAdjuster.cancelAuthorizationAndUpdate(
                    bookingId = booking.id.orEmpty(),
                    userId = user.id.orEmpty(),
                    customerId = user.stripeId.orEmpty(),
                    currency = "us",
                    paymentIntentId = booking.paymentId.orEmpty(),
                    reason = PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER
                )
                if (!res.ok) {
                    throw ApiRequestException("payment_error")
                }
                cancelWaitListPayment(booking)
            }
        }

        // Remove booking everywhere
        run.bookingList.removeAll { it.userId == user.id }
        run.bookings.removeAll { it.userId == user.id }
        run.waitList.removeAll { it.userId == user.id }

        val previousStatus = booking.status
        logger.info("booking status = $previousStatus")

        booking.status = BookingStatus.CANCELLED
        booking.cancelledAt = Instant.now()
        booking.cancelledBy = admin?.id
        bookingDbService.bookingRepository.save(booking)

        messagingService
            .removeParticipant(DeleteParticipantFromConversationModel(user.id.orEmpty(), run.id.orEmpty()))
            .block()

        completeFlow(previousStatus, run)

        return Pair(booking, run)
    }

    private fun cancelWaitListPayment(booking: Booking) {
        waitListPaymentService.cancelWaitlistSetupIntent(booking.setupIntentId.orEmpty())
    }

    private fun completeFlow(bookingStatus: BookingStatus, runSession: RunSession) {
        if (bookingStatus == BookingStatus.JOINED) {
            val job = JobEnvelope(
                jobId = UUID.randomUUID().toString(),
                taskType = "RAW_STRING",
                payload = runSession.id.orEmpty(),
                traceId = UUID.randomUUID().toString(),
                createdAtMs = Instant.now()
            )

            appScope.launch {
                queueService.sendJob(QueueNames.WAIT_LIST_JOB, job)
            }
        }
    }
}
