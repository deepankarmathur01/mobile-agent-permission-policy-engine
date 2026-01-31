package com.mobileagent.policy

import com.mobileagent.policy.engine.PolicyEngine
import com.mobileagent.policy.model.*
import org.junit.Assert.*
import org.junit.Test

class PolicyEngineTest {

    @Test
    fun `deny by default when no rules`() {
        val policy = Policy(
            version = 1,
            default = PolicyEffect.DENY,
            rules = emptyList()
        )

        val engine = PolicyEngine(policy)

        val skill = TestSkill(
            id = "test.skill",
            version = "1.0",
            requiredCapabilities = setOf(Capability.ReadCalendar)
        )

        val decision = engine.evaluate(skill)

        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("No policy rules defined"))
        assertEquals(1, decision.deniedCapabilities.size)
    }

    @Test
    fun `allow when matching rule and capability satisfied`() {
        val policy = Policy(
            version = 1,
            default = PolicyEffect.DENY,
            rules = listOf(
                PolicyRule(
                    id = "allow_calendar",
                    effect = PolicyEffect.ALLOW,
                    match = MatchCriteria(skillId = "test.skill"),
                    constraints = CapabilityConstraints(readCalendar = true)
                )
            )
        )

        val engine = PolicyEngine(policy)

        val skill = TestSkill(
            id = "test.skill",
            version = "1.0",
            requiredCapabilities = setOf(Capability.ReadCalendar)
        )

        val decision = engine.evaluate(skill)

        assertTrue(decision.allowed)
        assertEquals(listOf("allow_calendar"), decision.matchedRuleIds)
        assertTrue(decision.deniedCapabilities.isEmpty())
    }

    @Test
    fun `deny when capability not in constraints`() {
        val policy = Policy(
            version = 1,
            default = PolicyEffect.DENY,
            rules = listOf(
                PolicyRule(
                    id = "allow_network_only",
                    effect = PolicyEffect.ALLOW,
                    match = MatchCriteria(skillId = "test.skill"),
                    constraints = CapabilityConstraints(
                        network = NetworkConstraints(hosts = listOf("example.com"))
                    )
                )
            )
        )

        val engine = PolicyEngine(policy)

        val skill = TestSkill(
            id = "test.skill",
            version = "1.0",
            requiredCapabilities = setOf(Capability.ReadCalendar)
        )

        val decision = engine.evaluate(skill)

        assertFalse(decision.allowed)
        assertEquals(1, decision.deniedCapabilities.size)
        assertTrue(decision.reason.contains("Capabilities denied"))
    }

    @Test
    fun `match skill by pattern`() {
        val policy = Policy(
            version = 1,
            default = PolicyEffect.DENY,
            rules = listOf(
                PolicyRule(
                    id = "allow_all_webhooks",
                    effect = PolicyEffect.ALLOW,
                    match = MatchCriteria(skillIdPattern = "webhook\\..*"),
                    constraints = CapabilityConstraints(
                        network = NetworkConstraints(hosts = listOf("*"))
                    )
                )
            )
        )

        val engine = PolicyEngine(policy)

        val skill = TestSkill(
            id = "webhook.call",
            version = "1.0",
            requiredCapabilities = setOf(Capability.Network())
        )

        val decision = engine.evaluate(skill)

        assertTrue(decision.allowed)
        assertEquals(listOf("allow_all_webhooks"), decision.matchedRuleIds)
    }

    @Test
    fun `deny when no matching rules`() {
        val policy = Policy(
            version = 1,
            default = PolicyEffect.DENY,
            rules = listOf(
                PolicyRule(
                    id = "allow_other_skill",
                    effect = PolicyEffect.ALLOW,
                    match = MatchCriteria(skillId = "other.skill"),
                    constraints = CapabilityConstraints(readCalendar = true)
                )
            )
        )

        val engine = PolicyEngine(policy)

        val skill = TestSkill(
            id = "test.skill",
            version = "1.0",
            requiredCapabilities = setOf(Capability.ReadCalendar)
        )

        val decision = engine.evaluate(skill)

        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("No matching allow rules"))
    }

    // Test skill implementation
    private class TestSkill(
        override val id: String,
        override val version: String,
        override val requiredCapabilities: Set<Capability>
    ) : Skill<Unit, Unit> {
        override val description: String = "Test skill"

        override suspend fun execute(input: Unit, context: SkillContext) {
            // No-op
        }
    }
}
