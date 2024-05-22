package de.seemoo.at_tracking_detection.ui.tracking

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.*
import de.seemoo.at_tracking_detection.database.models.Beacon
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice
import de.seemoo.at_tracking_detection.database.models.device.Connectable
import de.seemoo.at_tracking_detection.database.models.device.DeviceManager
import de.seemoo.at_tracking_detection.database.models.device.DeviceType
import de.seemoo.at_tracking_detection.database.repository.BeaconRepository
import de.seemoo.at_tracking_detection.database.repository.DeviceRepository
import de.seemoo.at_tracking_detection.database.repository.NotificationRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

class TrackingViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val beaconRepository: BeaconRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    val deviceAddress = MutableLiveData<String>()

    val notificationId = MutableLiveData<Int>()

    val noLocationsYet = MutableLiveData(true)

    val manufacturerWebsiteUrl = MutableLiveData<String>()

    var deviceType = MutableLiveData<DeviceType>(DeviceType.UNKNOWN)

    val error = MutableLiveData(false)

    val falseAlarm = MutableLiveData(false)
    val deviceIgnored = MutableLiveData(false)
    val trackerObserved = MutableLiveData(false)

    val soundPlaying = MutableLiveData(false)
    val connecting = MutableLiveData(false)

    val device = MutableLiveData<BaseDevice?>()
    val connectable = MutableLiveData(false)

    val canBeIgnored = MutableLiveData(false)

    val showNfcHint = MutableLiveData(false)

    val isMapLoading = MutableLiveData(false)

    val markerLocations: LiveData<List<Beacon>> = deviceAddress.map {
        beaconRepository.getDeviceBeacons(it)
    }

    val beaconsHaveMissingLocation: LiveData<Boolean> = markerLocations.map {
        it.any { beacon ->
            beacon.locationId == null
        }
    }

    val amountBeacons: LiveData<String> = markerLocations.map {
        it.size.toString()
    }

    fun loadDevice(address: String) =
        deviceRepository.getDevice(address).also { device ->
            this.device.postValue(device)

            deviceType.value = DeviceManager.getDeviceTypeFromCache(address) ?: DeviceType.UNKNOWN
            Timber.d("Set Device type: ${deviceType.value}")

            if (device != null) {
                deviceType.value = device.device.deviceContext.deviceType
                val deviceObserved = device.nextObservationNotification != null && device.nextObservationNotification!!.isAfter(
                    LocalDateTime.now())
                trackerObserved.postValue(deviceObserved)
                deviceIgnored.postValue(device.ignore)
                noLocationsYet.postValue(false)
                connectable.postValue(device.device is Connectable)
                canBeIgnored.postValue(deviceType.value!!.canBeIgnored())
                val notification = notificationRepository.notificationForDevice(device).firstOrNull()
                notification?.let { notificationId.postValue(it.notificationId) }
            } else {
                noLocationsYet.postValue(true)
            }
            showNfcHint.postValue(deviceType.value == DeviceType.AIRTAG)
            if (deviceType.value != null) {
                val websiteURL = DeviceManager.getWebsiteURL(deviceType.value!!)
                manufacturerWebsiteUrl.postValue(websiteURL)
            } else {
                manufacturerWebsiteUrl.postValue("")
            }
        }

    fun toggleIgnoreDevice() {
        if (deviceAddress.value != null) {
            val newState = !deviceIgnored.value!!
            viewModelScope.launch {
                deviceRepository.setIgnoreFlag(deviceAddress.value!!, newState)
            }
            deviceIgnored.postValue(newState)
            Timber.d("Toggle ignore device - new State: $newState")
        }
    }

    fun toggleFalseAlarm() {
        if (notificationId.value != null) {
            val newState = !falseAlarm.value!!
            viewModelScope.launch {
                notificationRepository.setFalseAlarm(notificationId.value!!, newState)
            }
            falseAlarm.postValue(newState)
        }
    }

    fun clickOnWebsite(context: android.content.Context) {
        if (manufacturerWebsiteUrl.value != null) {
            Timber.d("Click on website: ${manufacturerWebsiteUrl.value}")
            val webpage: Uri = Uri.parse(manufacturerWebsiteUrl.value)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            context.startActivity(intent)
        }
    }
}