package com.mobileagent.policy

import com.mobileagent.policy.model.PolicyEffect
import com.mobileagent.policy.parser.PolicyParser
import com.mobileagent.policy.parser.PolicyParseException
import com.mobileagent.policy.parser.PolicyValidationException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PolicyParserTest {

    private lateinit var parser: PolicyParser

    @Before
    fun setup() {
        parser = PolicyParser()
    }

    @Test
    fun `parse valid JSON policy`() {
        val json = """
            {
              "version": 1,
              "default": "deny",
              "rules": [
                {
                  "id": "allow_webhook",
                  "effect": "allow",
                  "match": {
                    "skillId": "webhook.call"
                  },
                  "constraints": {
                    "network": {
                      "hosts": ["example.com"],
                      "methods": ["POST"]
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val policy = parser.parseJson(json)

        assertEquals(1, policy.version)
        assertEquals(PolicyEffect.DENY, policy.default)
        assertEquals(1, policy.rules.size)
        assertEquals("allow_webhook", policy.rules[0].id)
        assertEquals(PolicyEffect.ALLOW, policy.rules[0].effect)
        assertEquals("webhook.call", policy.rules[0].match.skillId)
    }

    @Test
    fun `parse valid YAML policy`() {
        val yaml = """
            version: 1
            default: deny
            rules:
              - id: allow_webhook
                effect: allow
                match:
                  skillId: webhook.call
                constraints:
                  network:
                    hosts:
                      - example.com
                    methods:
                      - POST
        """.trimIndent()

        val policy = parser.parseYaml(yaml)

        assertEquals(1, policy.version)
        assertEquals(PolicyEffect.DENY, policy.default)
        assertEquals(1, policy.rules.size)
        assertEquals("allow_webhook", policy.rules[0].id)
    }

    @Test(expected = PolicyParseException::class)
    fun `parse invalid JSON throws exception`() {
        val invalidJson = "{ invalid json }"
        parser.parseJson(invalidJson)
    }

    @Test
    fun `validate policy with duplicate rule IDs throws exception`() {
        val json = """
            {
              "version": 1,
              "default": "deny",
              "rules": [
                {
                  "id": "rule1",
                  "effect": "allow",
                  "match": { "skillId": "skill1" }
                },
                {
                  "id": "rule1",
                  "effect": "allow",
                  "match": { "skillId": "skill2" }
                }
              ]
            }
        """.trimIndent()

        val policy = parser.parseJson(json)

        try {
            parser.validate(policy)
            fail("Expected PolicyValidationException")
        } catch (e: PolicyValidationException) {
            assertTrue(e.message?.contains("Duplicate rule ID") ?: false)
        }
    }

    @Test
    fun `validate policy with invalid regex throws exception`() {
        val json = """
            {
              "version": 1,
              "default": "deny",
              "rules": [
                {
                  "id": "rule1",
                  "effect": "allow",
                  "match": { "skillIdPattern": "[invalid(" }
                }
              ]
            }
        """.trimIndent()

        val policy = parser.parseJson(json)

        try {
            parser.validate(policy)
            fail("Expected PolicyValidationException")
        } catch (e: PolicyValidationException) {
            assertTrue(e.message?.contains("invalid skillIdPattern regex") ?: false)
        }
    }

    @Test
    fun `validate policy without skillId or pattern throws exception`() {
        val json = """
            {
              "version": 1,
              "default": "deny",
              "rules": [
                {
                  "id": "rule1",
                  "effect": "allow",
                  "match": {}
                }
              ]
            }
        """.trimIndent()

        val policy = parser.parseJson(json)

        try {
            parser.validate(policy)
            fail("Expected PolicyValidationException")
        } catch (e: PolicyValidationException) {
            assertTrue(e.message?.contains("must specify skillId or skillIdPattern") ?: false)
        }
    }
}
