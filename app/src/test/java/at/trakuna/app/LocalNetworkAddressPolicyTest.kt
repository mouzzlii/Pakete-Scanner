package at.trakuna.app

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkAddressPolicyTest {
    private fun address(value: String) = InetAddress.getByName(value)

    @Test fun allowsPrivateIpv4Ranges() {
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("10.0.0.8")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("172.16.0.8")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("172.31.255.254")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("192.168.1.50")))
    }

    @Test fun allowsLinkLocalAndLoopbackIpv4() {
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("169.254.1.2")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("127.0.0.1")))
    }

    @Test fun allowsPrivateIpv6Ranges() {
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("fc00::1")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("fd12:3456::1")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("fe80::1")))
        assertTrue(LocalNetworkAddressPolicy.isAllowed(address("::1")))
    }

    @Test fun rejectsPublicAndSharedIpv4Addresses() {
        assertFalse(LocalNetworkAddressPolicy.isAllowed(address("8.8.8.8")))
        assertFalse(LocalNetworkAddressPolicy.isAllowed(address("172.32.0.1")))
        assertFalse(LocalNetworkAddressPolicy.isAllowed(address("100.64.0.1")))
    }

    @Test fun rejectsPublicIpv6Addresses() {
        assertFalse(LocalNetworkAddressPolicy.isAllowed(address("2001:4860:4860::8888")))
    }
}
