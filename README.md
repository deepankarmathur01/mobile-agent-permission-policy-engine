# Mobile Agent Permission & Policy Engine

[![CI](https://github.com/your-org/mobile-agent-permission-policy-engine/workflows/CI/badge.svg)](https://github.com/your-org/mobile-agent-permission-policy-engine/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An Android-first policy engine for controlling AI agent "skills" with deny-by-default permissions, capability constraints, and auditable execution logs.

## What is this?

This library provides a **security layer** for AI agents running on mobile devices. Instead of giving agents carte blanche access to device capabilities, you define **explicit policies** that control what each agent skill can do.

Think of it as:
- **Sandboxing for AI agents** - deny-by-default permissions
- **Fine-grained capability control** - network access only to specific hosts, calendar read-only, etc.
- **Audit logging** - structured JSON logs of every skill execution attempt
- **Developer-friendly** - simple DSL for policies (YAML/JSON), clear error messages

## Why this exists

AI agents on mobile devices need access to capabilities like:
- Reading calendar events
- Summarizing notifications
- Making network requests
- Accessing local storage

Without a policy layer, agents have unrestricted access to whatever the app is granted. This library enforces **least-privilege** for agent skills.

**This is NOT UI automation.** This is a policy enforcement layer for programmatic agent tools/skills.

## Features

- ✅ **Deny-by-default policy model** with explicit allow rules
- ✅ **Capability-based permissions** (Network, Calendar, Notifications, Storage, Custom)
- ✅ **YAML and JSON policy DSL** with strict validation
- ✅ **Constraint enforcement** (e.g., network calls only to specific hosts)
- ✅ **Audit logging** with structured JSON output
- ✅ **Extensible architecture** for custom capabilities and sinks
- ✅ **Android library** (Kotlin) with no dangerous permission requirements
- ✅ **Sample app** demonstrating 3 skills with different policies

## Quickstart

### 1. Add dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.mobileagent.policy:policy-engine:0.1.0")
}
```

### 2. Define a policy

Create `policy.yaml`:

```yaml
version: 1
default: deny
rules:
  - id: allow_calendar_read
    effect: allow
    match:
      skillId: "calendar.read"
    constraints:
      readCalendar: true

  - id: allow_webhook_to_safe_hosts
    effect: allow
    match:
      skillId: "webhook.call"
    constraints:
      network:
        hosts: ["example.com", "webhook.site"]
        methods: ["POST"]
```

### 3. Load policy and evaluate a skill

```kotlin
import com.mobileagent.policy.parser.PolicyParser
import com.mobileagent.policy.engine.PolicyEngine
import com.mobileagent.policy.audit.AuditLogger
import com.mobileagent.policy.audit.InMemoryAuditSink

// Parse policy
val parser = PolicyParser()
val policy = parser.parseYaml(policyYaml)
parser.validate(policy)

// Create engine and audit logger
val engine = PolicyEngine(policy)
val auditLogger = AuditLogger(InMemoryAuditSink())

// Evaluate a skill
val skill = CalendarReadSkill()
val decision = engine.evaluate(skill)

if (decision.allowed) {
    val context = SkillContext(decision.allowedCapabilities)
    val result = skill.execute(Unit, context)
    // Use result
} else {
    println("Denied: ${decision.reason}")
}

// Audit the execution
auditLogger.logExecution(
    skillId = skill.id,
    skillVersion = skill.version,
    decision = decision,
    requestedCapabilities = skill.requiredCapabilities,
    inputSize = 0,
    inputType = "Unit"
)
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Your App                           │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Agent Orchestrator                                │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│               ▼                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Policy Engine                                     │ │
│  │  - Load Policy (YAML/JSON)                         │ │
│  │  - Evaluate Skill Execution Request                │ │
│  │  - Return Decision (Allow/Deny + Constraints)      │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│               ▼                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Skill Execution                                   │ │
│  │  - CalendarReadSkill                               │ │
│  │  - NotificationSummarySkill                        │ │
│  │  - WebhookCallSkill                                │ │
│  │  - Custom Skills...                                │ │
│  └────────────┬───────────────────────────────────────┘ │
│               │                                          │
│               ▼                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Audit Logger                                      │ │
│  │  - Log every execution attempt                     │ │
│  │  - Write to sink (in-memory, file, custom)         │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Policy Examples

### Restrictive (deny everything except calendar read)

```yaml
version: 1
default: deny
rules:
  - id: allow_calendar_read
    effect: allow
    match:
      skillId: "calendar.read"
    constraints:
      readCalendar: true
```

### Permissive (allow multiple skills with constraints)

```yaml
version: 1
default: deny
rules:
  - id: allow_calendar_read
    effect: allow
    match:
      skillId: "calendar.read"
    constraints:
      readCalendar: true

  - id: allow_all_webhooks
    effect: allow
    match:
      skillIdPattern: "webhook\\..*"
    constraints:
      network:
        hosts: ["*"]
        methods: ["POST", "GET"]

  - id: allow_notification_summary
    effect: allow
    match:
      skillId: "notification.summary"
    constraints:
      readNotifications:
        apps: ["com.example.app1", "com.example.app2"]
```

## Documentation

- [Design Document](docs/DESIGN.md) - Core abstractions and architecture
- [Threat Model](docs/THREAT_MODEL.md) - Security assumptions and mitigations
- [Security Policy](SECURITY.md) - Responsible disclosure process
- [Contributing Guide](CONTRIBUTING.md) - How to contribute

## Roadmap

- [x] Core policy engine with deny-by-default
- [x] YAML/JSON policy parsing
- [x] Audit logging with JSON sinks
- [x] Sample Android app with 3 demo skills
- [x] Unit tests for parsing and evaluation
- [x] GitHub Actions CI
- [ ] Rate limiting per skill
- [ ] Time-based constraints (allow only during business hours)
- [ ] Policy versioning and migration
- [ ] Remote policy fetching with signature verification
- [ ] Kotlin Multiplatform support (iOS, Desktop)
- [ ] Policy editor UI
- [ ] Integration with popular agent frameworks

## Sample App

The included sample app demonstrates:
- **3 example skills**: CalendarRead, NotificationSummary, WebhookCall
- **2 policies**: restrictive and permissive
- **Live audit log viewer** in the UI
- **No dangerous permissions** required (all mocked/stubbed)

To run the sample app:

```bash
./gradlew :sample-app:installDebug
adb shell am start -n com.mobileagent.sample/.MainActivity
```

Or open the project in Android Studio and run the `sample-app` configuration.

## Testing

Run all tests:

```bash
./gradlew test
```

Run linting:

```bash
./gradlew detekt
```

## Security

**IMPORTANT:** This library is a policy enforcement layer, NOT an OS-level security boundary. It relies on:
- Developer discipline to check decisions before executing skills
- Skills voluntarily respecting capability constraints
- App-level sandboxing (Android permissions model)

See [THREAT_MODEL.md](docs/THREAT_MODEL.md) for detailed security analysis.

**Do NOT use this library for:**
- Bypassing OS security
- Capturing OTPs, passwords, or keystrokes
- UI automation or accessibility service abuse

Report security issues to: [SECURITY.md](SECURITY.md)

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

## FAQ

**Q: Is this a runtime permission system like Android's?**
A: No. This is an application-level policy layer for AI agent skills. It doesn't replace Android permissions.

**Q: Can a malicious skill bypass the policy?**
A: Yes, if the skill is implemented maliciously within your app. This library trusts the skill implementations to respect the `SkillContext` capabilities. See threat model.

**Q: Does this work on iOS?**
A: Not yet. Currently Android-only, but Kotlin Multiplatform support is planned.

**Q: Can I use this for non-AI use cases?**
A: Yes! Any scenario where you need fine-grained capability control with audit logging.

**Q: How do I add custom capabilities?**
A: Extend `Capability.Custom(name: String)` and implement custom evaluators. See [DESIGN.md](docs/DESIGN.md).

---

Built with ❤️ for secure mobile AI agents.