package com.mobileagent.policy.engine

import com.mobileagent.policy.model.*

/**
 * Core policy evaluation engine.
 * Evaluates skill execution requests against loaded policies.
 */
class PolicyEngine(private val policy: Policy) {

    /**
     * Evaluate a skill execution request.
     *
     * @param skill The skill to evaluate
     * @return A decision indicating whether the skill is allowed to execute
     */
    fun <I, O> evaluate(skill: Skill<I, O>): Decision {
        // Deny by default
        if (policy.default == PolicyEffect.DENY && policy.rules.isEmpty()) {
            return Decision.deny(
                "No policy rules defined and default is deny",
                skill.requiredCapabilities.toList()
            )
        }

        // Find matching rules
        val matchingRules = policy.rules.filter { rule ->
            rule.effect == PolicyEffect.ALLOW &&
                    rule.match.matches(skill.id, skill.version)
        }

        if (matchingRules.isEmpty()) {
            return Decision.deny(
                "No matching allow rules for skill ${skill.id}",
                skill.requiredCapabilities.toList()
            )
        }

        // Check if all required capabilities are satisfied
        val deniedCapabilities = mutableListOf<Capability>()
        val allowedCapabilities = mutableSetOf<Capability>()

        for (requiredCap in skill.requiredCapabilities) {
            var capabilityAllowed = false

            for (rule in matchingRules) {
                if (isCapabilitySatisfied(requiredCap, rule.constraints)) {
                    capabilityAllowed = true
                    // Add constrained capability
                    allowedCapabilities.add(constrainCapability(requiredCap, rule.constraints))
                    break
                }
            }

            if (!capabilityAllowed) {
                deniedCapabilities.add(requiredCap)
            }
        }

        return if (deniedCapabilities.isEmpty()) {
            Decision.allow(
                "All required capabilities satisfied",
                matchingRules.map { it.id },
                allowedCapabilities
            )
        } else {
            Decision.deny(
                "Capabilities denied: ${deniedCapabilities.joinToString { it.toString() }}",
                deniedCapabilities
            )
        }
    }

    /**
     * Check if a capability is satisfied by the constraints.
     */
    private fun isCapabilitySatisfied(
        capability: Capability,
        constraints: CapabilityConstraints?
    ): Boolean {
        if (constraints == null) {
            return false
        }

        return when (capability) {
            is Capability.Network -> constraints.network != null
            is Capability.ReadCalendar -> constraints.readCalendar == true
            is Capability.ReadNotifications -> constraints.readNotifications != null
            is Capability.WriteLocalStorage -> constraints.writeLocalStorage == true
            is Capability.Custom -> false // Custom capabilities not supported without explicit evaluator
        }
    }

    /**
     * Apply constraints to a capability to create a constrained version.
     */
    private fun constrainCapability(
        capability: Capability,
        constraints: CapabilityConstraints?
    ): Capability {
        if (constraints == null) {
            return capability
        }

        return when (capability) {
            is Capability.Network -> {
                val networkConstraints = constraints.network
                Capability.Network(
                    hosts = networkConstraints?.hosts,
                    methods = networkConstraints?.methods
                )
            }

            is Capability.ReadNotifications -> {
                val notifConstraints = constraints.readNotifications
                Capability.ReadNotifications(
                    apps = notifConstraints?.apps
                )
            }

            else -> capability
        }
    }
}
