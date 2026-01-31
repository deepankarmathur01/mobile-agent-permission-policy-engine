package com.mobileagent.policy.audit

import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Decision

/**
 * Manages audit logging for skill executions.
 */
class AuditLogger(private val sink: AuditSink) {

    /**
     * Log a skill execution attempt.
     */
    suspend fun logExecution(
        skillId: String,
        skillVersion: String,
        decision: Decision,
        requestedCapabilities: Set<Capability>,
        inputSize: Int,
        inputType: String,
        executionBlock: (suspend () -> Unit)? = null
    ) {
        val event = AuditEvent.create(
            skillId = skillId,
            skillVersion = skillVersion,
            decision = decision,
            requestedCapabilities = requestedCapabilities,
            inputSize = inputSize,
            inputType = inputType
        )

        if (executionBlock != null && decision.allowed) {
            val started = System.currentTimeMillis()
            var success = false
            var error: String? = null

            try {
                executionBlock()
                success = true
            } catch (e: Exception) {
                success = false
                error = e.message
                throw e
            } finally {
                val ended = System.currentTimeMillis()
                sink.emit(event.withExecution(started, ended, success, error))
            }
        } else {
            sink.emit(event)
        }
    }

    suspend fun flush() {
        sink.flush()
    }
}
