package at.trakuna.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ext.SdkExtensions
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
@Suppress("DEPRECATION")
internal class PrinterDiscovery(
    context: Context,
    private val onPrinterFound: (DiscoveredPrinter) -> Unit,
    private val onStatus: (String) -> Unit
) : Closeable {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<NsdManager.DiscoveryListener>()
    private val resolved = ConcurrentHashMap.newKeySet<String>()
    private var multicastLock: WifiManager.MulticastLock? = null
    @Volatile private var closed = false
    @Volatile private var generation = 0

    private val stopAfterWindow = Runnable {
        if (!closed && listeners.isNotEmpty()) {
            val count = resolved.size
            stopInternal()
            onStatus(if (count == 0) {
                "Suche beendet: keine passenden Druckerdienste gefunden."
            } else {
                "Suche beendet: $count Druckerdienst(e) gefunden."
            })
        }
    }

    fun start() {
        if (closed || listeners.isNotEmpty()) return
        val scanGeneration = ++generation
        resolved.clear()
        acquireMulticastLockIfNeeded()
        onStatus("Suche im lokalen Netzwerk nach bekannten Druckerdiensten (max. 20 Sekunden) …")
        handler.removeCallbacks(stopAfterWindow)
        handler.postDelayed(stopAfterWindow, DISCOVERY_WINDOW_MS)

        SERVICE_TYPES.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (closed || scanGeneration != generation) return
                    try {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                if (closed || scanGeneration != generation) return
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
                    releaseMulticastLockIfIdle()
                }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
                    listeners.remove(this)
                    onStatus("Netzwerkerkennung für einen Diensttyp fehlgeschlagen (Code $errorCode).")
                    releaseMulticastLockIfIdle()
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
                    listeners.remove(this)
                    releaseMulticastLockIfIdle()
                }
            }
            listeners.add(listener)
            try {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {
                listeners.remove(listener)
                onStatus("Netzwerkerkennung konnte nicht gestartet werden.")
                releaseMulticastLockIfIdle()
            }
        }
    }

    private fun acquireMulticastLockIfNeeded() {
        val needsManualLock = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) < 7)
        if (!needsManualLock || multicastLock?.isHeld == true) return
        try {
            multicastLock = wifiManager.createMulticastLock("trakuna-printer-discovery").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: SecurityException) {
            multicastLock = null
            onStatus("WLAN-Multicast nicht freigegeben; automatische Suche kann eingeschränkt sein.")
        }
    }

    private fun releaseMulticastLockIfIdle() {
        if (listeners.isNotEmpty()) return
        try {
            multicastLock?.takeIf { it.isHeld }?.release()
        } catch (_: RuntimeException) {
            // Lock may already have been released during teardown.
        } finally {
            multicastLock = null
        }
    }

    private fun stopInternal() {
        generation++
        handler.removeCallbacks(stopAfterWindow)
        listeners.toList().forEach { listener ->
            try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        listeners.clear()
        releaseMulticastLockIfIdle()
    }

    fun stop() {
        if (listeners.isEmpty()) {
            handler.removeCallbacks(stopAfterWindow)
            releaseMulticastLockIfIdle()
            return
        }
        stopInternal()
        onStatus("Netzwerksuche beendet.")
    }

    override fun close() {
        closed = true
        stopInternal()
    }

    companion object {
        private const val DISCOVERY_WINDOW_MS = 20_000L
        private val SERVICE_TYPES = listOf(
            "_ipp._tcp.",
            "_ipps._tcp.",
            "_printer._tcp.",
            "_pdl-datastream._tcp."
        )
    }
}
