package com.mobileagent.policy.parser

import com.mobileagent.policy.model.Policy
import kotlinx.serialization.json.Json
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

/**
 * Parser for policy documents supporting YAML and JSON formats.
 */
class PolicyParser {

    private val json = Json {
        ignoreUnknownKeys = false // Strict mode: fail on unknown fields
        isLenient = false
        prettyPrint = true
    }

    /**
     * Parse a policy from JSON string.
     *
     * @param jsonString The JSON policy string
     * @return Parsed policy
     * @throws PolicyParseException if parsing fails
     */
    fun parseJson(jsonString: String): Policy {
        return try {
            json.decodeFromString<Policy>(jsonString)
        } catch (e: Exception) {
            throw PolicyParseException("Failed to parse JSON policy: ${e.message}", e)
        }
    }

    /**
     * Parse a policy from YAML string.
     *
     * @param yamlString The YAML policy string
     * @return Parsed policy
     * @throws PolicyParseException if parsing fails
     */
    fun parseYaml(yamlString: String): Policy {
        return try {
            // First parse YAML to a structure
            val settings = LoadSettings.builder().build()
            val load = Load(settings)
            val yamlData = load.loadFromString(yamlString)

            // Convert YAML to JSON for serialization
            val jsonString = convertYamlToJson(yamlData)

            // Parse JSON using kotlinx.serialization
            json.decodeFromString<Policy>(jsonString)
        } catch (e: PolicyParseException) {
            throw e
        } catch (e: Exception) {
            throw PolicyParseException("Failed to parse YAML policy: ${e.message}", e)
        }
    }

    /**
     * Validate a policy for correctness.
     *
     * @param policy The policy to validate
     * @throws PolicyValidationException if validation fails
     */
    fun validate(policy: Policy) {
        if (policy.version < 1) {
            throw PolicyValidationException("Policy version must be >= 1")
        }

        val ruleIds = mutableSetOf<String>()
        for (rule in policy.rules) {
            if (rule.id.isBlank()) {
                throw PolicyValidationException("Rule ID cannot be blank")
            }

            if (rule.id in ruleIds) {
                throw PolicyValidationException("Duplicate rule ID: ${rule.id}")
            }
            ruleIds.add(rule.id)

            if (rule.match.skillId == null && rule.match.skillIdPattern == null) {
                throw PolicyValidationException("Rule ${rule.id}: must specify skillId or skillIdPattern")
            }

            if (rule.match.skillId != null && rule.match.skillIdPattern != null) {
                throw PolicyValidationException("Rule ${rule.id}: cannot specify both skillId and skillIdPattern")
            }

            // Validate pattern if present
            rule.match.skillIdPattern?.let { pattern ->
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    throw PolicyValidationException("Rule ${rule.id}: invalid skillIdPattern regex: $pattern")
                }
            }
        }
    }

    /**
     * Convert YAML data structure to JSON string.
     */
    @Suppress("UNCHECKED_CAST")
    private fun convertYamlToJson(data: Any?): String {
        return when (data) {
            is Map<*, *> -> {
                val map = data as Map<String, Any?>
                "{" + map.entries.joinToString(",") { (k, v) ->
                    "\"$k\":${convertYamlToJson(v)}"
                } + "}"
            }

            is List<*> -> {
                "[" + data.joinToString(",") { convertYamlToJson(it) } + "]"
            }

            is String -> "\"$data\""
            is Number -> data.toString()
            is Boolean -> data.toString()
            null -> "null"
            else -> throw PolicyParseException("Unsupported YAML type: ${data::class}")
        }
    }
}

/**
 * Exception thrown when policy parsing fails.
 */
class PolicyParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when policy validation fails.
 */
class PolicyValidationException(message: String) : Exception(message)
