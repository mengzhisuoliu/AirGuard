package de.seemoo.at_tracking_detection.ui.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import androidx.databinding.ObservableField
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getConnectionState
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getPublicKey
import de.seemoo.at_tracking_detection.database.models.device.DeviceManager.getDeviceType
import de.seemoo.at_tracking_detection.database.models.device.DeviceType
import de.seemoo.at_tracking_detection.database.models.device.types.SamsungTracker

@SuppressLint("MissingPermission")
data class ScanResultWrapper(val scanResult: ScanResult){
    val deviceAddress: String = scanResult.device.address
    val rssi: ObservableField<Int> = ObservableField(scanResult.rssi) // This is so the image can update itself live
    var rssiValue: Int = scanResult.rssi
    var txPower: Int = scanResult.txPower
    var isConnectable: Boolean = scanResult.isConnectable
    val deviceType = getDeviceType(scanResult)
    val uniqueIdentifier = getPublicKey(scanResult, deviceType)  // either public key or MAC-Address
    var connectionState = getConnectionState(scanResult, deviceType)
    val serviceUuids = scanResult.scanRecord?.serviceUuids?.map { it.toString() }?.toList()
    val mfg = scanResult.scanRecord?.bytes
    val advertisementFlags = scanResult.scanRecord?.advertiseFlags

    // Information for Sub categorization
    @SuppressLint("MissingPermission")
    val advertisedName: String? = scanResult.device.name
    val uwbCapable = when (deviceType) {
        DeviceType.SAMSUNG_TRACKER -> SamsungTracker.getUwbAvailability(scanResult)
        DeviceType.AIRTAG -> true
        else -> false
    }

    override fun hashCode(): Int {
        return scanResult.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}