package com.mobileagent.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileagent.policy.audit.AuditEvent
import com.mobileagent.policy.audit.AuditLogger
import com.mobileagent.policy.audit.InMemoryAuditSink
import com.mobileagent.policy.engine.PolicyEngine
import com.mobileagent.policy.model.Decision
import com.mobileagent.policy.model.Policy
import com.mobileagent.policy.model.SkillContext
import com.mobileagent.policy.parser.PolicyParser
import com.mobileagent.sample.skills.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PolicyEngineDemo(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyEngineDemo(activity: ComponentActivity) {
    var selectedPolicy by remember { mutableStateOf("policy_permissive.yaml") }
    var currentPolicy by remember { mutableStateOf<Policy?>(null) }
    var lastDecision by remember { mutableStateOf<Decision?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var auditEvents by remember { mutableStateOf<List<AuditEvent>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val auditSink = remember { InMemoryAuditSink() }
    val auditLogger = remember { AuditLogger(auditSink) }
    val scope = rememberCoroutineScope()

    // Load policy when selection changes
    LaunchedEffect(selectedPolicy) {
        try {
            val policyText = activity.assets.open(selectedPolicy).bufferedReader().use { it.readText() }
            val parser = PolicyParser()
            currentPolicy = parser.parseYaml(policyText)
            parser.validate(currentPolicy!!)
        } catch (e: Exception) {
            lastResult = "Error loading policy: ${e.message}"
        }
    }

    // Update audit events
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            auditEvents = auditSink.getEvents()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Mobile Agent Policy Engine Demo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Policy selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPolicy,
                onValueChange = {},
                readOnly = true,
                label = { Text("Policy") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("policy_restrictive.yaml") },
                    onClick = {
                        selectedPolicy = "policy_restrictive.yaml"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("policy_permissive.yaml") },
                    onClick = {
                        selectedPolicy = "policy_permissive.yaml"
                        expanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skill execution buttons
        Text("Execute Skills:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    executeSkill(
                        skill = CalendarReadSkill(),
                        input = Unit,
                        policy = currentPolicy,
                        auditLogger = auditLogger,
                        onDecision = { lastDecision = it },
                        onResult = { lastResult = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Calendar Read Skill")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    executeSkill(
                        skill = NotificationSummarySkill(),
                        input = Unit,
                        policy = currentPolicy,
                        auditLogger = auditLogger,
                        onDecision = { lastDecision = it },
                        onResult = { lastResult = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Notification Summary Skill")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    executeSkill(
                        skill = WebhookCallSkill(),
                        input = WebhookInput(
                            url = "https://httpbin.org/post",
                            method = "POST",
                            payload = """{"test": "data"}"""
                        ),
                        policy = currentPolicy,
                        auditLogger = auditLogger,
                        onDecision = { lastDecision = it },
                        onResult = { lastResult = it }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Webhook Call Skill")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Decision display
        lastDecision?.let { decision ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (decision.allowed)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Decision: ${if (decision.allowed) "ALLOWED" else "DENIED"}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        decision.reason,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (decision.matchedRuleIds.isNotEmpty()) {
                        Text(
                            "Rules: ${decision.matchedRuleIds.joinToString()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Result display
        lastResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Result:", style = MaterialTheme.typography.titleSmall)
                    Text(result, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audit log
        Text(
            "Audit Log (${auditEvents.size} events):",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(auditEvents.reversed()) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "${event.skillId} v${event.skillVersion}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Decision: ${if (event.decision.allowed) "ALLOWED" else "DENIED"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            event.decision.reason,
                            style = MaterialTheme.typography.bodySmall
                        )
                        event.execution?.let { exec ->
                            Text(
                                "Executed in ${exec.durationMs}ms (${if (exec.success) "success" else "failed"})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun <I, O> executeSkill(
    skill: com.mobileagent.policy.model.Skill<I, O>,
    input: I,
    policy: Policy?,
    auditLogger: AuditLogger,
    onDecision: (Decision) -> Unit,
    onResult: (String) -> Unit
) {
    if (policy == null) {
        onResult("No policy loaded")
        return
    }

    try {
        val engine = PolicyEngine(policy)
        val decision = engine.evaluate(skill)
        onDecision(decision)

        val inputStr = input.toString()
        auditLogger.logExecution(
            skillId = skill.id,
            skillVersion = skill.version,
            decision = decision,
            requestedCapabilities = skill.requiredCapabilities,
            inputSize = inputStr.length,
            inputType = input?.let { it::class.simpleName } ?: "Unit"
        ) {
            if (decision.allowed) {
                val context = SkillContext(decision.allowedCapabilities)
                val result = skill.execute(input, context)
                onResult("Success: $result")
            } else {
                onResult("Execution blocked by policy")
            }
        }
    } catch (e: Exception) {
        onResult("Error: ${e.message}")
    }
}
