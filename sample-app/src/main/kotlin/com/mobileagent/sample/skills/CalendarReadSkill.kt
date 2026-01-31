package com.mobileagent.sample.skills

import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Skill
import com.mobileagent.policy.model.SkillContext

/**
 * Skill that reads calendar events (mocked implementation).
 */
class CalendarReadSkill : Skill<Unit, List<CalendarEvent>> {
    override val id: String = "calendar.read"
    override val version: String = "1.0.0"
    override val description: String = "Read calendar events"
    override val requiredCapabilities: Set<Capability> = setOf(Capability.ReadCalendar)

    override suspend fun execute(input: Unit, context: SkillContext): List<CalendarEvent> {
        // Mocked implementation - returns static data
        return listOf(
            CalendarEvent("Team Meeting", "2026-02-01 10:00"),
            CalendarEvent("Lunch with Sarah", "2026-02-01 12:30"),
            CalendarEvent("Code Review", "2026-02-01 15:00")
        )
    }
}

data class CalendarEvent(
    val title: String,
    val time: String
)
