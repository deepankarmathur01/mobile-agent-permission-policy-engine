package com.mobileagent.policy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents capabilities that a skill may require.
 * Capabilities define what operations a skill is allowed to perform.
 */
@Serializable
sealed class Capability {

    @Serializable
    @SerialName("network")
    data class Network(
        val hosts: List<String>? = null,
        val methods: List<String>? = null
    ) : Capability() {
        fun allowsHost(host: String): Boolean {
            return hosts == null || hosts.contains(host) || hosts.any { pattern ->
                host.matches(Regex(pattern.replace("*", ".*")))
            }
        }

        fun allowsMethod(method: String): Boolean {
            return methods == null || methods.contains(method.uppercase())
        }
    }

    @Serializable
    @SerialName("read_calendar")
    data object ReadCalendar : Capability()

    @Serializable
    @SerialName("read_notifications")
    data class ReadNotifications(
        val apps: List<String>? = null
    ) : Capability() {
        fun allowsApp(app: String): Boolean {
            return apps == null || apps.contains(app)
        }
    }

    @Serializable
    @SerialName("write_local_storage")
    data object WriteLocalStorage : Capability()

    @Serializable
    @SerialName("custom")
    data class Custom(val name: String) : Capability()
}
