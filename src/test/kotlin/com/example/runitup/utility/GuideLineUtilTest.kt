package com.example.runitup.utility

import com.example.runitup.mobile.utility.GuideLineUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GuideLineUtilTest {

    @Test
    fun `provideGuideLineList returns expected ordered list of guidelines`() {
        val guidelines = GuideLineUtil.provideGuideLineList()

        // Basic sanity checks
        assertThat(guidelines).isNotNull
        assertThat(guidelines).hasSize(7)

        // Titles in order
        assertThat(guidelines.map { it.title }).containsExactly(
            "Respect All Players",
            "Show Up or Cancel Early",
            "No Discrimination or Hate Speech",
            "Maintain Cleanliness",
            "Report Issues",
            "Be Honest in Your Skill Level",
            "Encourage Others"
        )

        // Descriptions are non-blank
        guidelines.forEach { g ->
            assertThat(g.description).isNotBlank
        }

        // Images match expected SF Symbol identifiers in order
        assertThat(guidelines.map { it.image }).containsExactly(
            "hand.raised.fill",
            "calendar.badge.clock",
            "hand.raised.slash.fill",
            "sparkles",
            "exclamationmark.bubble.fill",
            "figure.basketball",
            "hands.sparkles.fill"
        )
    }
}