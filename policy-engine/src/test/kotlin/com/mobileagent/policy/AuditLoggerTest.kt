package com.mobileagent.policy

import com.mobileagent.policy.audit.AuditLogger
import com.mobileagent.policy.audit.InMemoryAuditSink
import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Decision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuditLoggerTest {

    private lateinit var sink: InMemoryAuditSink
    private lateinit var logger: AuditLogger

    @Before
    fun setup() {
        sink = InMemoryAuditSink()
        logger = AuditLogger(sink)
    }

    @Test
    fun `audit event is logged for denied execution`() = runBlocking {
        val decision = Decision.deny("Test denial", listOf(Capability.ReadCalendar))

        logger.logExecution(
            skillId = "test.skill",
            skillVersion = "1.0",
            decision = decision,
            requestedCapabilities = setOf(Capability.ReadCalendar),
            inputSize = 100,
            inputType = "TestInput"
        )

        val events = sink.getEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals("test.skill", event.skillId)
        assertFalse(event.decision.allowed)
        assertEquals("Test denial", event.decision.reason)
        assertNull(event.execution)
    }

    @Test
    fun `audit event includes execution record for allowed execution`() = runBlocking {
        val decision = Decision.allow(
            "Test approval",
            listOf("rule1"),
            setOf(Capability.ReadCalendar)
        )

        logger.logExecution(
            skillId = "test.skill",
            skillVersion = "1.0",
            decision = decision,
            requestedCapabilities = setOf(Capability.ReadCalendar),
            inputSize = 100,
            inputType = "TestInput"
        ) {
            // Simulate execution
            Thread.sleep(10)
        }

        val events = sink.getEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertTrue(event.decision.allowed)
        assertNotNull(event.execution)
        assertTrue(event.execution!!.durationMs >= 10)
        assertTrue(event.execution!!.success)
    }

    @Test
    fun `audit event captures execution error`() = runBlocking {
        val decision = Decision.allow(
            "Test approval",
            listOf("rule1"),
            setOf(Capability.ReadCalendar)
        )

        try {
            logger.logExecution(
                skillId = "test.skill",
                skillVersion = "1.0",
                decision = decision,
                requestedCapabilities = setOf(Capability.ReadCalendar),
                inputSize = 100,
                inputType = "TestInput"
            ) {
                throw RuntimeException("Test error")
            }
            fail("Expected exception")
        } catch (e: RuntimeException) {
            // Expected
        }

        val events = sink.getEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertNotNull(event.execution)
        assertFalse(event.execution!!.success)
        assertEquals("Test error", event.execution!!.error)
    }
}
