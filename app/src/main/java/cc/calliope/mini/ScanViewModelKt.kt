package cc.calliope.mini

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.scanner.BleNumOfMatches
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanMode
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResults
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerCallbackType
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerMatchMode
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerSettings
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner
import no.nordicsemi.android.kotlin.ble.scanner.aggregator.BleScanResultAggregator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScanViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _devices = MutableLiveData<List<ServerDeviceWrapper>>()
    val devices: LiveData<List<ServerDeviceWrapper>> get() = _devices

    fun startScan() {
        val context = getApplication<Application>().applicationContext
        val settings = BleScannerSettings(
                BleScanMode.SCAN_MODE_BALANCED,
                0L,
                BleScannerCallbackType.CALLBACK_TYPE_ALL_MATCHES,
                BleNumOfMatches.MATCH_NUM_MAX_ADVERTISEMENT,
                BleScannerMatchMode.MATCH_MODE_AGGRESSIVE,
                false,
                null
        )

        //Create aggregator which will concat scan records with a device
        val aggregator = Aggregator()
        BleScanner(context).scan(settings)
                .map { scanResult ->
                    val timestampNanos = scanResult.data?.timestampNanos
                    val rssiValue = scanResult.data?.rssi
                    val name = scanResult.device.name
                    val address = scanResult.device.address
                    val relevant = isRelevant(timestampNanos)

                    val isFound = name.contains("vipep")
                    if (isFound) {
                        Log.i("SCANNER", "Name: $name, address: $address, RSSI: $rssiValue, timestampNanos: $timestampNanos, relevant: $relevant")
                    }
                    aggregator.aggregateDevices(scanResult)// Add new device and return an aggregated list
                }
                .onEach { _devices.value = it } // Propagated state to UI
                .launchIn(viewModelScope) // Scanning will stop after we leave the screen
    }

    private fun isRelevant(recentUpdate: Long?): Boolean {
        if (recentUpdate == null)
            return false
        val currentTime = System.currentTimeMillis()
        return currentTime - recentUpdate < 1000
    }

    fun stopScan() {
        viewModelScope.cancel()
    }
}

