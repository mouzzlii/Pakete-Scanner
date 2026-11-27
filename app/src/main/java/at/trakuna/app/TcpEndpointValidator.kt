package at.trakuna.app

internal data class TcpEndpoint(val host: String, val port: Int)

internal sealed interface TcpEndpointValidation {
    data class Valid(val endpoint: TcpEndpoint) : TcpEndpointValidation
    data class Invalid(val reason: String) : TcpEndpointValidation
}

/**
 * Validates only a user-entered TCP destination. It does not probe or connect.
 * DNS names, IPv4 and IPv6 literals are accepted; URL paths and schemes are not.
 */
internal object TcpEndpointValidator {
    fun parse(hostInput: String, portInput: String): TcpEndpointValidation {
        val rawHost = hostInput.trim()
        if (rawHost.isEmpty()) {
            return TcpEndpointValidation.Invalid("Bitte eine IP-Adresse oder einen Hostnamen eingeben.")
        }
        if (rawHost.any { it.isWhitespace() || it.isISOControl() } ||
            rawHost.contains("://") ||
            rawHost.any { it in "/?#@" }) {
            return TcpEndpointValidation.Invalid("Bitte nur eine IP-Adresse oder einen Hostnamen eingeben – keine URL oder Pfadangabe.")
        }

        val host = when {
            rawHost.startsWith("[") && rawHost.endsWith("]") -> rawHost.substring(1, rawHost.length - 1)
            rawHost.startsWith("[") || rawHost.endsWith("]") ->
                return TcpEndpointValidation.Invalid("Die IPv6-Klammern sind unvollständig.")
            else -> rawHost
        }
        if (host.isEmpty()) {
            return TcpEndpointValidation.Invalid("Der Hostname darf nicht leer sein.")
        }

        val port = portInput.trim().toIntOrNull()
            ?: return TcpEndpointValidation.Invalid("Bitte einen gültigen Port zwischen 1 und 65535 eingeben.")
        if (port !in 1..65535) {
            return TcpEndpointValidation.Invalid("Der Port muss zwischen 1 und 65535 liegen.")
        }

        return TcpEndpointValidation.Valid(TcpEndpoint(host, port))
    }
}
