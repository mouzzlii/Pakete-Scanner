package at.trakuna.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

internal data class DiscoveredPrinter(
    val name: String,
    val host: String,
    val port: Int,
    val serviceType: String
) {
    val key: String get() = "${host.lowercase()}:$port"
}

/**
 * Discovers common DNS-SD printer service advertisements on the current LAN.
 * Discovery is advisory: a service announcement does not prove print-language
 * compatibility or that the device accepts a print job.
 */
internal class PrinterDiscovery(
    context: Context,
    private val onPrinterFound: (DiscoveredPrinter) -> Unit,
    private val onStatus: (String) -> Unit
) : Closeable {
    private val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val listeners = CopyOnWriteArrayList<NsdManager.DiscoveryListener>()
    private val resolved = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var closed = false

    fun start() {
        if (closed || listeners.isNotEmpty()) return
        onStatus("Suche im lokalen Netzwerk nach bekannten Druckerdiensten …")
        SERVICE_TYPES.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (closed) return
                    try {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                if (closed) return
                                val address = info.host ?: return
                                if (!LocalNetworkAddressPolicy.isAllowed(address)) return
                                val printer = DiscoveredPrinter(
                                    name = info.serviceName.orEmpty().ifBlank { "Netzwerkdrucker" },
                                    host = address.hostAddress.orEmpty(),
                                    port = info.port,
                                    serviceType = info.serviceType.orEmpty()
                                )
                                if (printer.port in 1..65535 && resolved.add(printer.key)) {
                                    onPrinterFound(printer)
                                    onStatus("Druckerdienst gefunden: ${printer.name}. Bitte Gerät und Anschluss vor dem Verbinden prüfen.")
                                }
                            }
                        })
                    } catch (_: IllegalArgumentException) {
                        // Android may reject a service that disappeared during discovery.
                    } catch (_: IllegalStateException) {
                        // Discovery can be stopped while a result is being resolved.
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                override fun onDiscoveryStopped(serviceType: String) {
                    listeners.remove(this)
                }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
                    listeners.remove(this)
                    onStatus("Netzwerkerkennung für einen Diensttyp fehlgeschlagen (Code $errorCode).")
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
                    listeners.remove(this)
                }
            }
            listeners.add(listener)
            try {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {
                listeners.remove(listener)
                onStatus("Netzwerkerkennung konnte nicht gestartet werden.")
            }
        }
    }

    fun stop() {
        listeners.toList().forEach { listener ->
            try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        listeners.clear()
        onStatus("Netzwerksuche beendet.")
    }

    override fun close() {
        closed = true
        stop()
    }

    companion object {
        private val SERVICE_TYPES = listOf(
            "_ipp._tcp.",
            "_ipps._tcp.",
            "_printer._tcp.",
            "_pdl-datastream._tcp."
        )
    }
}
