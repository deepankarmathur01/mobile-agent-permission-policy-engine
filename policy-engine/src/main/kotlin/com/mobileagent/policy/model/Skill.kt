package com.mobileagent.policy.model

/**
 * Represents a skill that can be executed by an agent.
 * Skills are the basic unit of functionality that require capabilities.
 */
interface Skill<I, O> {
    val id: String
    val version: String
    val description: String
    val requiredCapabilities: Set<Capability>

    /**
     * Execute the skill with the given input and context.
     *
     * @param input The input data for the skill
     * @param context The execution context providing access to capabilities
     * @return The output of the skill execution
     */
    suspend fun execute(input: I, context: SkillContext): O
}

/**
 * Context provided to a skill during execution.
 * Provides access to validated capabilities and audit logging.
 */
data class SkillContext(
    val allowedCapabilities: Set<Capability>,
    val metadata: Map<String, Any> = emptyMap()
)
