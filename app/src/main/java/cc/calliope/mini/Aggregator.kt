package cc.calliope.mini

import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResultData
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResults

class Aggregator {
    private val devices = mutableMapOf<ServerDevice, List<BleScanResultData>?>()
    private val results
        get() = devices.map { BleScanResults(it.key, it.value ?: emptyList()) }

    private fun aggregate(scanItem: BleScanResult): List<BleScanResults> {
        val data = scanItem.data
        if (data != null) {
            devices[scanItem.device] = (devices[scanItem.device] ?: emptyList()) + data
        } else {
            devices[scanItem.device] = devices[scanItem.device]
        }
        return results
    }

    fun aggregateDevices(scanItem: BleScanResult): List<ServerDeviceWrapper> {
        val aggregatedResults = aggregate(scanItem)
        return aggregatedResults.map { ServerDeviceWrapper(it.device) }
    }
}