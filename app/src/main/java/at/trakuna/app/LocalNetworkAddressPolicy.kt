package at.trakuna.app

import java.net.Inet6Address
import java.net.InetAddress

/**
 * Limits the manual printer connectivity check to local/private network targets.
 * The resolved address must be used for the actual socket connection to avoid
 * resolving the hostname a second time after validation.
 */
internal object LocalNetworkAddressPolicy {
    fun isAllowed(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isMulticastAddress) return false
        if (address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress
        ) return true

        val bytes = address.address
        return address is Inet6Address &&
            bytes.isNotEmpty() &&
            (bytes[0].toInt() and 0xFE) == 0xFC // IPv6 Unique Local Address (fc00::/7)
    }
}
