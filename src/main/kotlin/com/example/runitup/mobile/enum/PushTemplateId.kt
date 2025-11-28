package com.example.runitup.mobile.enum

enum class PushTemplateId(val id: String) {
    RUN_CONFIRMED("run.confirmed"),
    RUN_CANCELLED("run.cancelled"),
    RUN_USER_JOINED("run.user_joined"),
    RUN_USER_JOINED_WAITLIST("run.user_joined_waitlist"),
    RUN_USER_PROMOTED("run.user_promoted"),
    RUN_BOOKING_CANCELLED("run.booking.cancelled"),
    RUN_CREATED("run.created"),
    RUN_BOOKING_START("run.booking.start_notification"),
    RUN_USER_JOINED_WAITLIST_ADMIN("run.user_joinedwaitlist_admin"),
    RUN_BOOKING_START_NOTIFICATION("run.booking_start_notification"),
    RUN_BOOKING_CANCELLED_USER("run.booking_cancelled_user")
}