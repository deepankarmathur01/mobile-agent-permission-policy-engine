# Design Document

## Overview

The Mobile Agent Permission & Policy Engine is designed to provide fine-grained capability control for AI agent skills running on mobile devices. This document describes the core abstractions, architecture decisions, and extension points.

## Core Abstractions

### 1. Capability

Capabilities represent **what** a skill is allowed to do. They are the fundamental unit of permission in the system.

**Design Principles:**
- Sealed class hierarchy for type safety
- Capabilities can carry constraints (e.g., network to specific hosts only)
- Extensible via `Capability.Custom`

**Built-in Capabilities:**
- `Capability.Network(hosts, methods)` - Network access with host/method constraints
- `Capability.ReadCalendar` - Read calendar events
- `Capability.ReadNotifications(apps)` - Read notifications, optionally filtered by app
- `Capability.WriteLocalStorage` - Write to local storage
- `Capability.Custom(name)` - Custom capabilities for extension

### 2. Skill

Skills are units of agent functionality that require capabilities to execute.

```kotlin
interface Skill<I, O> {
    val id: String
    val version: String
    val description: String
    val requiredCapabilities: Set<Capability>
    suspend fun execute(input: I, context: SkillContext): O
}
```

**Design Principles:**
- Skills declare their required capabilities upfront
- Skills receive a `SkillContext` with only allowed capabilities
- Skills are responsible for respecting capability constraints

### 3. Policy

Policies are deny-by-default documents that define which skills can execute and under what constraints.

```kotlin
data class Policy(
    val version: Int,
    val default: PolicyEffect, // DENY or ALLOW
    val rules: List<PolicyRule>
)
```

**Policy Rules:**
- Match skills by exact ID or regex pattern
- Define capability constraints
- Support metadata for documentation

**Design Decisions:**
- Deny-by-default: safer fallback
- Explicit allow rules: clear intent
- Rule IDs must be unique: easier debugging
- Strict validation: fail fast on errors

### 4. Policy Engine

The policy engine evaluates skill execution requests against loaded policies.

**Algorithm:**
1. If no rules and default is DENY → deny all capabilities
2. Find all matching allow rules for the skill
3. For each required capability:
   - Check if any matching rule satisfies it
   - If yes, add constrained capability to allowed set
   - If no, add to denied list
4. If all capabilities satisfied → allow
5. Otherwise → deny

**Constraint Application:**
- Network: hosts and methods from rule constraints
- ReadNotifications: apps from rule constraints
- Other capabilities: boolean presence check

### 5. Audit Logging

Every skill execution attempt is logged, regardless of outcome.

**Audit Event Structure:**
```json
{
  "timestamp": 1706745600000,
  "skillId": "webhook.call",
  "skillVersion": "1.0.0",
  "decision": {
    "allowed": true,
    "reason": "All required capabilities satisfied",
    "matchedRuleIds": ["allow_webhook"],
    "deniedCapabilities": []
  },
  "requestedCapabilities": ["Network(hosts=[example.com])"],
  "inputMetadata": {
    "size": 100,
    "type": "WebhookInput"
  },
  "execution": {
    "started": 1706745600000,
    "ended": 1706745601234,
    "durationMs": 1234,
    "success": true
  }
}
```

**Audit Sinks:**
- `InMemoryAuditSink` - For testing and demos
- `FileAuditSink` - JSON lines to file
- `CompositeAuditSink` - Fan out to multiple sinks
- Custom sinks: implement `AuditSink` interface

## Policy DSL

### YAML Format

```yaml
version: 1
default: deny
rules:
  - id: rule_identifier
    effect: allow
    match:
      skillId: "exact.skill.id"
      # OR
      skillIdPattern: "regex.*pattern"
      skillVersion: "1.0.0" # optional
    constraints:
      network:
        hosts: ["example.com", "*.safe-domain.com"]
        methods: ["POST", "GET"]
      readCalendar: true
      readNotifications:
        apps: ["com.example.app"]
      writeLocalStorage: true
    metadata:
      description: "Human-readable description"
```

### JSON Format

JSON mirrors the YAML structure using kotlinx.serialization.

### Validation

Policies are validated on parse:
- Version must be >= 1
- Rule IDs must be unique
- Rules must specify skillId OR skillIdPattern (not both)
- Regex patterns must be valid
- No blank rule IDs

## Extension Points

### Custom Capabilities

1. Define a new capability:
```kotlin
@Serializable
@SerialName("send_sms")
data class SendSms(val numbers: List<String>?) : Capability()
```

2. Extend policy constraints:
```kotlin
data class CapabilityConstraints(
    // ... existing
    val sendSms: SmsConstraints? = null
)
```

3. Update `PolicyEngine.isCapabilitySatisfied()`

### Custom Audit Sinks

Implement `AuditSink` interface:
```kotlin
class RemoteAuditSink(val endpoint: String) : AuditSink {
    override suspend fun emit(event: AuditEvent) {
        // Send to remote logging service
    }
    override suspend fun flush() {
        // Ensure all events sent
    }
}
```

### Custom Skill Implementations

Simply implement the `Skill<I, O>` interface:
```kotlin
class CustomSkill : Skill<CustomInput, CustomOutput> {
    override val id = "custom.skill"
    override val version = "1.0.0"
    override val description = "My custom skill"
    override val requiredCapabilities = setOf(
        Capability.Custom("my_capability")
    )

    override suspend fun execute(input: CustomInput, context: SkillContext): CustomOutput {
        // Implementation
    }
}
```

## Security Considerations

### Trust Boundaries

1. **Policy files** - Trusted input (developer-provided)
2. **Skill implementations** - Trusted (part of app)
3. **Skill inputs** - Untrusted (user/agent-provided)
4. **Network responses** - Untrusted

### Threat Model Summary

**In Scope:**
- Policy enforcement for well-behaved skills
- Audit logging for accountability
- Input validation in policy parsing

**Out of Scope:**
- Malicious skill implementations
- OS-level permission bypass
- Runtime policy tampering (app process compromise)

See [THREAT_MODEL.md](THREAT_MODEL.md) for detailed analysis.

## Performance Considerations

### Policy Evaluation

- O(n) rule matching where n = number of rules
- Regex compilation cached by JVM
- No network I/O during evaluation
- Suitable for real-time agent execution

### Audit Logging

- Async emission to sinks
- In-memory sink: O(1) append
- File sink: buffered writes
- Recommend batch flushing for high-volume scenarios

## Future Enhancements

### Rate Limiting

Add per-skill rate limits to policy:
```yaml
rules:
  - id: rate_limited_webhook
    effect: allow
    match:
      skillId: "webhook.call"
    constraints:
      network: { ... }
    rateLimit:
      maxRequests: 10
      windowSeconds: 60
```

### Time-Based Constraints

Allow/deny based on time:
```yaml
rules:
  - id: business_hours_only
    effect: allow
    match:
      skillId: "work.skill"
    timeConstraints:
      days: ["MON", "TUE", "WED", "THU", "FRI"]
      hours: "09:00-17:00"
```

### Policy Composition

Load multiple policy files and merge:
```kotlin
val policy = PolicyComposer()
    .add(basePolicy)
    .add(overridePolicy)
    .build()
```

### Remote Policy Fetching

Fetch policies from a server with signature verification:
```kotlin
val policy = RemotePolicyLoader(url, publicKey)
    .fetch()
    .verify()
    .parse()
```

## Testing Strategy

### Unit Tests
- Policy parsing (YAML, JSON, validation errors)
- Policy engine evaluation (allow, deny, constraints)
- Audit event generation

### Integration Tests
- End-to-end skill execution with policies
- Audit sink implementations
- Error handling paths

### Property-Based Tests
- Policy rule combinations
- Constraint edge cases
- Malformed inputs

---

This design balances **security**, **usability**, and **extensibility** for production mobile AI agents.
