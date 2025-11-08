package com.example.runitup.mobile.utility

import com.example.runitup.mobile.rest.v1.dto.GuideLine

object GuideLineUtil {

    fun provideGuideLineList():List<GuideLine>{
        val guidelines = listOf(
            GuideLine(
                title = "Respect All Players",
                description = "Treat everyone with respect — regardless of skill level, gender, or background. Trash talk is part of the game, but disrespect or harassment is never tolerated.",
                image = "hand.raised.fill"
            ),
            GuideLine(
                title = "Show Up or Cancel Early",
                description = "If you’ve joined a run, show up on time. Can’t make it? Cancel early so someone else can take your spot — no-shows hurt the whole community.",
                image = "calendar.badge.clock"
            ),
            GuideLine(
                title = "No Discrimination or Hate Speech",
                description = "RunItUp is a diverse community. Discriminatory or hateful language or actions will result in immediate suspension.",
                image = "hand.raised.slash.fill"
            ),
            GuideLine(
                title = "Maintain Cleanliness",
                description = "Respect the space. Pick up after yourself, keep the court clean, and leave locker rooms as you found them.",
                image = "sparkles"
            ),
            GuideLine(
                title = "Report Issues",
                description = "If something feels wrong — from unsafe play to harassment — report it through the app. Your feedback helps keep the community safe.",
                image = "exclamationmark.bubble.fill"
            ),
            GuideLine(
                title = "Be Honest in Your Skill Level",
                description = "Choose your skill level accurately when joining runs. It helps match players fairly and keeps games competitive yet fun.",
                image = "figure.basketball"
            ),
            GuideLine(
                title = "Encourage Others",
                description = "Support newer players, celebrate good plays, and help build a positive environment that keeps everyone coming back.",
                image = "hands.sparkles.fill"
            )
        )
        return  guidelines
    }
}