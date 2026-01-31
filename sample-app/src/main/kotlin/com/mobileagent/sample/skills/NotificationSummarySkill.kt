package com.mobileagent.sample.skills

import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Skill
import com.mobileagent.policy.model.SkillContext

/**
 * Skill that summarizes notifications (mocked implementation).
 */
class NotificationSummarySkill : Skill<Unit, NotificationSummary> {
    override val id: String = "notification.summary"
    override val version: String = "1.0.0"
    override val description: String = "Summarize recent notifications"
    override val requiredCapabilities: Set<Capability> = setOf(
        Capability.ReadNotifications()
    )

    override suspend fun execute(input: Unit, context: SkillContext): NotificationSummary {
        // Mocked implementation - returns static summary
        return NotificationSummary(
            total = 12,
            byApp = mapOf(
                "Email" to 5,
                "Messages" to 4,
                "Social" to 3
            ),
            summary = "You have 12 notifications: 5 emails, 4 messages, and 3 social updates."
        )
    }
}

data class NotificationSummary(
    val total: Int,
    val byApp: Map<String, Int>,
    val summary: String
)
