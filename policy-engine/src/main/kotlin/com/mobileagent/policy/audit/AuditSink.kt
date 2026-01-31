package com.mobileagent.policy.audit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Interface for audit event sinks.
 */
interface AuditSink {
    suspend fun emit(event: AuditEvent)
    suspend fun flush()
}

/**
 * In-memory audit sink for testing and demos.
 */
class InMemoryAuditSink : AuditSink {
    private val events = mutableListOf<AuditEvent>()
    private val mutex = Mutex()

    override suspend fun emit(event: AuditEvent) {
        mutex.withLock {
            events.add(event)
        }
    }

    override suspend fun flush() {
        // No-op for in-memory sink
    }

    suspend fun getEvents(): List<AuditEvent> {
        return mutex.withLock {
            events.toList()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            events.clear()
        }
    }
}

/**
 * File-based audit sink that writes JSON lines to a file.
 */
class FileAuditSink(private val file: File) : AuditSink {
    private val json = Json { prettyPrint = false }
    private val mutex = Mutex()

    init {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    override suspend fun emit(event: AuditEvent) {
        mutex.withLock {
            val jsonLine = json.encodeToString(event)
            file.appendText("$jsonLine\n")
        }
    }

    override suspend fun flush() {
        // File writes are flushed immediately
    }
}

/**
 * Composite sink that emits to multiple sinks.
 */
class CompositeAuditSink(private val sinks: List<AuditSink>) : AuditSink {
    override suspend fun emit(event: AuditEvent) {
        sinks.forEach { it.emit(event) }
    }

    override suspend fun flush() {
        sinks.forEach { it.flush() }
    }
}
