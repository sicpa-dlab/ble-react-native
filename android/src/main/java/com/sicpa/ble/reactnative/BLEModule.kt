package com.sicpa.ble.reactnative

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.observer.ConnectionObserver
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.random.Random

private const val MODULE_NAME = "BLEModule"

@ReactModule(name = MODULE_NAME)
class BLEModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val PAYLOAD_STRING_KEY = "payload"
    }

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

    override fun getConstants(): MutableMap<String, Any> =
        mutableMapOf(
            "PAYLOAD_STRING_KEY" to PAYLOAD_STRING_KEY,
            "BLE-EVENT-NAME" to BLEEvent,
        )

    @ReactMethod
    fun start() {
        // nothing to initialize
    }

    @ReactMethod
    fun stop() {
        finish(null)
    }

    @ReactMethod
    fun generateBleId(promise: Promise) {
        promise.resolve(Random(System.currentTimeMillis()).nextInt(1000, 10000).toString())
    }

    @SuppressLint("MissingPermission")
    @ReactMethod
    fun advertise(bleId: String, promise: Promise) {
        stopAdvertise()
        cleanUpServer()

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

    @SuppressLint("MissingPermission")
    @ReactMethod
    fun stopAdvertise() {
        advertiseCallback?.let {
            bluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
        }
    }

    @SuppressLint("MissingPermission")
    @ReactMethod
    fun scan(filterBleId: String, stopIfFound: Boolean, promise: Promise) {
        Log.d(MODULE_NAME, "Scanning with bleId: $filterBleId")
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

    @SuppressLint("MissingPermission")
    @ReactMethod
    fun stopScan() {
        scanCallback?.let {
            bluetoothManager.adapter.bluetoothLeScanner.stopScan(it)
        }
    }

    @ReactMethod
    fun connectToPeripheral(address: String, promise: Promise) =
        synchronized(connectedPeripheralManager ?: Unit) {
            runBlocking {
                val connectedDeviceManager = connectedPeripheralManager
                if (connectedDeviceManager != null) {
                    // already connected to some device or trying to connect

                    if (connectedDeviceManager.bluetoothDevice?.address == address) {
                        // already connected to the desired device
                        promise.resolve(null)
                    }

                    internalDisconnect()
                }

                val device = cachedScannedDevices[address] ?: run {
                    Log.e(MODULE_NAME, "Cannot connect to device: not found")
                    promise.reject(BLEException("Requested device not found"))
                    return@runBlocking
                }

                connectedPeripheralManager = ClientBleManager {
                    if (it.trim() == "ready") {
                        Log.d(MODULE_NAME, "Peripheral ready message received")
                        // android client is not ready to receive messages when the peripheral is ready so this code is probably not going to be called
                        // so ignoring the message and just doing nothing
                        // "ready" message from the peripheral is needed when an ios device is the client
                    } else {
                        sendEvent(MessageReceived(it))
                    }
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

        Log.d(MODULE_NAME, "Trying to send a message")

        if (connectedPeripheralManager?.isReady == true) {
            Log.d(MODULE_NAME, "Sending data to peripheral")
            sendEvent(SendingMessage)
            connectedPeripheralManager?.sendMessage(bytes)
            promise.resolve(null)
        } else if (serverManager?.isClientConnected() == true) {
            Log.d(MODULE_NAME, "Sending data to central")
            sendEvent(SendingMessage)
            serverManager?.setCharacteristicValue(bytes)
            promise.resolve(null)
        } else {
            Log.d(MODULE_NAME, "No one to send the message to.")
            promise.reject(BLEException("No one to send the message to"))
        }
    }

    @ReactMethod
    fun disconnect(promise: Promise) = scope.launch {
        internalDisconnect()
        promise.resolve(null)
    }

    @ReactMethod
    fun finish(promise: Promise?) = scope.launch {
        internalDisconnect()
        cachedScannedDevices.clear()
        stopScan()
        stopAdvertise()
        promise?.resolve(null)
    }

    private suspend fun internalDisconnect() {
        connectedPeripheralManager?.let {
            try {
                Log.d(MODULE_NAME, "Trying to disconnect from peripheral")
                it.disconnect().suspend()
                Log.d(MODULE_NAME, "Successfully disconnected from peripheral")
            } catch (exception: Exception) {
                Log.e(MODULE_NAME, "Error disconnecting from peripheral", exception)
            } finally {
                cleanUpClient()
            }
        }

        serverManager?.let {
            try {
                Log.d(MODULE_NAME, "Trying to disconnect clients")
                it.disconnect()
                Log.d(MODULE_NAME, "Successfully disconnected clients")
            } catch (exception: Exception) {
                Log.e(MODULE_NAME, "Error disconnecting clients")
            } finally {
                cleanUpServer()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun validateScanResult(filterBleId: String, scanResult: ScanResult): Result<Boolean> {
        if (scanResult.device?.name == filterBleId) {
            return Result.success(true)
        } else {
            val manufacturerData =
                scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId) ?: return Result.success(false)

            return Result.success(String(manufacturerData) == filterBleId)
        }
    }

    private fun sendEvent(event: BLEEventType) {
        val context = reactApplicationContext ?: run {
            Log.e(MODULE_NAME, "Error sending event to react native, context is missing")
            return
        }

        val params = Arguments.createMap().apply {
            putString("type", event.type)
            when (event) {
                is MessageReceived -> putString(PAYLOAD_STRING_KEY, event.payload)
                else -> {}
            }
        }

        try {
            context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(event.type, params)
            Log.d(MODULE_NAME, "Event sent to react native: ${event.type}")
        } catch (e: Exception) {
            Log.e(MODULE_NAME, "Error sending event to react native. ${event.type}, ${e.message}\n${e.stackTrace}")
        }
    }

    private fun onClientDisconnected() {
        scope.launch {
            cleanUpServer()
        }
    }

    private fun cleanUpServer() {
        serverManager?.setServerObserver(null)
        serverManager?.close()
        serverManager = null
    }

    private fun cleanUpClient() {
        connectedPeripheralManager?.close()
        connectedPeripheralManager?.connectionObserver = null
        connectedPeripheralManager = null
    }

    inner class ClientBleManager(private val onMessageReceived: (String) -> Unit) : BleManager(reactContext),
        ConnectionObserver {

        private var writeCharacteristic: BluetoothGattCharacteristic? = null
        private var messageMerger = MessageMerger()

        init {
            connectionObserver = this
        }

        override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {
            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val service = gatt.getService(serviceUUID) ?: return false
                writeCharacteristic = service.getCharacteristic(characteristicUUID)
                Log.d(MODULE_NAME, "Characteristics discovered")
                return true
            }

            override fun initialize() {
                setNotificationCallback(writeCharacteristic)
                    .merge { output, lastPacket, index ->
                        if (index == 0) sendEvent(StartedMessageReceive)
                        return@merge messageMerger.merge(output, lastPacket, index)
                    }
                    .with { _, data ->
                        Log.d(MODULE_NAME, "Received a notification")
                        data.value?.let {
                            val message = String(it).trim()
                            Log.d(MODULE_NAME, "Received data $message")
                            onMessageReceived(message)
                        }
                    }
                beginAtomicRequestQueue()
                    .add(requestMtu(185))
                    .add(enableNotifications(writeCharacteristic))
                    .enqueue()
            }

            override fun onServicesInvalidated() {

            }

        }

        fun sendMessage(message: ByteArray) {
            writeCharacteristic(
                writeCharacteristic, message,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
                .split(MessageSplitter())
                .then { sendEvent(MessageSent) }
                .enqueue()
        }

        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(MODULE_NAME, "Connecting to device ${device.address}")
            sendEvent(ConnectingToServer)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(MODULE_NAME, "Connected to device ${device.address}")
            sendEvent(ConnectedToServer)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(MODULE_NAME, "Failed to connect to device ${device.address}, reason: $reason")
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(MODULE_NAME, "Device $${device.address} is ready")
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(MODULE_NAME, "Device ${device.address} disconnecting")
            sendEvent(DisconnectingFromServer)
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            Log.d(MODULE_NAME, "Device ${device.address} disconnected")
            sendEvent(DisconnectedFromServer)
            cleanUpClient()
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
            Log.d(MODULE_NAME, "Device ${device.address} connected")
            stopAdvertise()

            serverConnection?.let {
                // if we have an existing connection, close it
                cleanUpServerConnection()
            }

            serverConnection = ServerConnection().apply {
                useServer(this@ServerBleManager)
                connectionObserver = this
                connect(device).enqueue()
            }
        }

        override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {
            Log.d(MODULE_NAME, "Device ${device.address} disconnected")
            sendEvent(ClientDisconnected)
            cleanUpServerConnection()
            onClientDisconnected()
        }

        suspend fun disconnect() {
            serverConnection?.disconnect()
                ?.then {
                    cleanUpServerConnection()
                }
                ?.suspend()
        }

        private fun cleanUpServerConnection() {
            serverConnection?.connectionObserver = null
            serverConnection?.close()
            serverConnection = null
        }

        /*
         * Manages the state of an individual server connection (there can be many of these)
         */
        inner class ServerConnection : BleManager(context), ConnectionObserver {

            private var gattCallback: GattCallback? = null
            private var messageMerger = MessageMerger()

            fun sendNotificationForMyGattCharacteristic(value: ByteArray) {
                Log.d(MODULE_NAME, "Sending update notification")
                sendNotification(characteristic, value)
                    .split(MessageSplitter())
                    .then { sendEvent(MessageSent) }
                    .enqueue()
            }

            override fun log(priority: Int, message: String) {
                this@ServerBleManager.log(priority, message)
            }

            override fun getGattCallback(): BleManagerGattCallback {
                gattCallback = GattCallback()
                return gattCallback!!
            }

            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.d(MODULE_NAME, "On device ${device.address} connecting")
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
            }

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            }

            override fun onDeviceReady(device: BluetoothDevice) {
                overrideMtu(185)
                setWriteCallback(characteristic)
                    .merge { output, lastPacket, index ->
                        if (index == 0) sendEvent(StartedMessageReceive)
                        return@merge messageMerger.merge(output, lastPacket, index)
                    }
                    .with { _, data ->
                        val bytes = data.value ?: return@with

                        val message = String(bytes).trim()
                        Log.d(MODULE_NAME, "Received data $message")
                        sendEvent(MessageReceived(message))
                    }
                sendEvent(ClientConnected)
                sendNotificationForMyGattCharacteristic("ready".encodeToByteArray())
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            }

            fun writeCharacteristic(value: ByteArray) {
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