package com.sicpa.ble.reactnative

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Message
import android.os.ParcelUuid
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.random.Random

private const val MODULE_NAME = "BLEModule"

@ReactModule(name = MODULE_NAME)
class BLEModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val serviceUUID: UUID = UUID.fromString("6E33748B-D176-4D38-A962-8947ECEC8271")
    private val characteristicUUID = UUID.fromString("F026CBE5-A1DB-44FB-9E2F-E55FDB94B293")
    private val manufacturerId = 0xFFFF

    private val bluetoothManager by lazy {
        (reactContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    }

    private var scanCallback: ScanCallback? = null
    private var advertiseCallback: AdvertiseCallback? = null

    private val cachedScannedDevices = mutableMapOf<String, BluetoothDevice>()

    private var connectedPeripheralManager: ClientBleManager? = null
    private var serverManager: ServerBleManager? = null

    override fun getName() = MODULE_NAME

    private val scope = CoroutineScope(Dispatchers.Default)

    @ReactMethod
    fun generateBleId(promise: Promise) {
        promise.resolve(Random(System.currentTimeMillis()).nextInt(1000, 10000).toString())
    }

    @ReactMethod
    fun advertise(bleId: String, promise: Promise) {
        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            throw BLEException("Bluetooth scan permission is not granted")
        }

        val newAdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d(MODULE_NAME, "Started advertise")
                promise.resolve(null)
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(MODULE_NAME, "Could not start advertise: $errorCode")
                promise.reject(BLEException("Could not start advertise: $errorCode"))
            }
        }
        advertiseCallback = newAdvertiseCallback

        serverManager = ServerBleManager(reactContext).also { it.open() }
        bluetoothManager.adapter.bluetoothLeAdvertiser.startAdvertising(
            AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build(),
            AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(serviceUUID))
                .build(),
            AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addManufacturerData(manufacturerId, bleId.toByteArray(StandardCharsets.UTF_8))
                .addServiceUuid(ParcelUuid(serviceUUID))
                .build(),
            newAdvertiseCallback,
        )
    }

    @ReactMethod
    fun stopAdvertise() {
        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            throw BLEException("Bluetooth scan permission is not granted")
        }

        bluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    @ReactMethod
    fun scan(filterBleId: String, stopIfFound: Boolean, promise: Promise) {
        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            throw BLEException("Bluetooth scan permission is not granted")
        }

        val newScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return

                // filter by bleId. It may be the device name or in manufacturer specific data

                Log.d(MODULE_NAME, "BLE scan result found: $result")

                val resultValid = validateScanResult(filterBleId, result).getOrElse {
                    promise.reject(it)
                    Log.e(MODULE_NAME, "Could not validate BLE scan result ${result.device.address}")
                    return
                }

                Log.d(
                    MODULE_NAME,
                    "BLE scan result ${result.device.address} is ${if (resultValid) "" else "not"} valid"
                )

                if (resultValid) {
                    if (stopIfFound) {
                        stopScan()
                    }

                    cachedScannedDevices[result.device.address] = result.device
                    promise.resolve(result.device.address)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(MODULE_NAME, "Scan failed with error code: $errorCode")
                promise.reject(BLEException("Scan failed with error code: $errorCode"))
            }
        }
        scanCallback = newScanCallback


        bluetoothManager.adapter.bluetoothLeScanner.startScan(
            mutableListOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(serviceUUID))
                    .build()
            ),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0)
                .build(),
            newScanCallback,
        )
    }

    @ReactMethod
    fun stopScan() {
        scanCallback?.let {
            if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                throw BLEException("Bluetooth scan permission is not granted")
            }

            bluetoothManager.adapter.bluetoothLeScanner.stopScan(it)
        }
    }

    @ReactMethod
    fun connectToPeripheral(address: String, promise: Promise) {
        synchronized(connectedPeripheralManager ?: Unit) {
            val connectedDeviceManager = connectedPeripheralManager
            if (connectedDeviceManager != null) {
                // already connected to some device or trying to connect

                if (connectedDeviceManager.bluetoothDevice?.address == address) {
                    // already connected to the desired device
                    promise.resolve(null)
                }

                connectedDeviceManager.disconnect()
                connectedPeripheralManager = null
            }

            val device = cachedScannedDevices[address] ?: run {
                Log.e(MODULE_NAME, "Cannot connect to device: not found")
                promise.reject(BLEException("Requested device not found"))
                return
            }

            connectedPeripheralManager = ClientBleManager {
                // TODO send event to react native
            }.also {
                it.connect(device)
                    .done {
                        Log.d(MODULE_NAME, "Connected successfully to $address")
                        promise.resolve(null)
                    }
                    .fail { _, status ->
                        Log.e(MODULE_NAME, "Cannot connect to device, status: $status")
                        promise.reject(BLEException("Cannot connect to device, status: $status"))
                    }
                    .enqueue()
            }
        }
    }

    @ReactMethod
    fun sendMessage(message: String, promise: Promise) {
        val bytes = message.encodeToByteArray()

        if (connectedPeripheralManager?.isReady == true) {
            connectedPeripheralManager?.sendMessage(bytes)
        } else if (serverManager?.isClientConnected() == true) {
            serverManager?.setCharacteristicValue(bytes)
        }
    }

    private fun validateScanResult(filterBleId: String, scanResult: ScanResult): Result<Boolean> {
        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(BLEException("Bluetooth connect permission is not granted"))
        }

        if (scanResult.device?.name == filterBleId) {
            return Result.success(true)
        } else {
            val manufacturerData =
                scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId) ?: return Result.success(false)

            return Result.success(String(manufacturerData) == filterBleId)
        }
    }

    inner class ClientBleManager(private val onMessageReceived: (String) -> Unit) : BleManager(reactContext) {

        private var writeCharacteristic: BluetoothGattCharacteristic? = null

        override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {
            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val service = gatt.getService(serviceUUID) ?: return false
                writeCharacteristic = service.getCharacteristic(characteristicUUID)
                Log.d(MODULE_NAME, "Characteristics discovered")
                setNotificationCallback(writeCharacteristic).with { _, data ->
                    Log.d(MODULE_NAME, "Received a notification")
                    data.value?.let {
                        val message = String(it)
                        Log.d(MODULE_NAME, "Received data $message")
                        onMessageReceived(message)
                    }
                }
                enableNotifications(writeCharacteristic).enqueue()
                return true
            }

            override fun onServicesInvalidated() {

            }

        }

        fun sendMessage(message: ByteArray) {
            writeCharacteristic(
                writeCharacteristic, message,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ).enqueue()
        }
    }

    inner class ServerBleManager(private val context: Context) : BleServerManager(reactContext), ServerObserver {

        private val characteristic = sharedCharacteristic(
            characteristicUUID,
            BluetoothGattCharacteristic.PROPERTY_READ
                    or BluetoothGattCharacteristic.PROPERTY_WRITE
                    or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE,
            cccd(),
        )

        private val service = service(
            serviceUUID,
            characteristic,
        )

        private val services = Collections.singletonList(service)

        private var serverConnection: ServerConnection? = null

        fun setCharacteristicValue(value: ByteArray) {
            serverConnection?.writeCharacteristic(value)
        }

        fun isClientConnected() = serverConnection?.isReady == true

        override fun initializeServer(): MutableList<BluetoothGattService> {
            setServerObserver(this)

            return services
        }

        override fun onServerReady() {
            Log.d(MODULE_NAME, "GATT server ready")
        }

        override fun onDeviceConnectedToServer(device: BluetoothDevice) {
            serverConnection = ServerConnection().apply {
                useServer(this@ServerBleManager)
                connect(device).enqueue()
            }
        }

        override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {
            serverConnection?.close()
            serverConnection = null
        }

        /*
         * Manages the state of an individual server connection (there can be many of these)
         */
        inner class ServerConnection : BleManager(context) {

            private var gattCallback: GattCallback? = null

            init {
                setWriteCallback(characteristic).with { _, data ->
                    val bytes = data.value ?: return@with

                    val message = String(bytes)
                    Log.d(MODULE_NAME, "Received data $message")
                    // TODO send event to react native
                }
            }

            fun sendNotificationForMyGattCharacteristic(value: ByteArray) {
                sendNotification(characteristic, value).enqueue()
            }

            override fun log(priority: Int, message: String) {
                this@ServerBleManager.log(priority, message)
            }

            override fun getGattCallback(): BleManagerGattCallback {
                gattCallback = GattCallback()
                return gattCallback!!
            }

            fun writeCharacteristic(value: ByteArray) {
                writeCharacteristic(
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                ).enqueue()
                sendNotificationForMyGattCharacteristic(value)
            }

            private inner class GattCallback : BleManagerGattCallback() {

                // There are no services that we need from the connecting device, but
                // if there were, we could specify them here.
                override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                    return true
                }

                override fun onServicesInvalidated() {
                    // This is the place to nullify characteristics obtained above.
                }
            }
        }
    }
}