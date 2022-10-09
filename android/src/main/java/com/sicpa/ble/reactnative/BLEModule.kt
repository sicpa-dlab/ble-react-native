package com.sicpa.ble.reactnative

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

    override fun getName() = MODULE_NAME

    private val scope = CoroutineScope(Dispatchers.Default)

    @ReactMethod
    fun generateBleId(promise: Promise) {
        promise.resolve(Random(System.currentTimeMillis()).nextInt(1000,  10000).toString())
    }

    @ReactMethod
    fun advertise(bleId: String, promise: Promise) {
        Log.d(MODULE_NAME, "advertise")

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
        Log.d(MODULE_NAME, "scan")

        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            throw BLEException("Bluetooth scan permission is not granted")
        }

        Log.d(MODULE_NAME, "permission")

        val newScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Log.d(MODULE_NAME, "got result")

                result ?: return

                // filter by bleId. It may be the device name or in manufacturer specific data

                Log.d(MODULE_NAME, "BLE scan result found: $result")

                val resultValid = validateScanResult(filterBleId, result).getOrElse {
                    promise.reject(it)
                    Log.e(MODULE_NAME, "Could not validate BLE scan result ${result.device.address}")
                    return
                }

                Log.d(MODULE_NAME, "BLE scan result ${result.device.address} is ${if (resultValid) "" else "not"} valid")

                if (resultValid) {
                    if (stopIfFound) {
                        stopScan()
                    }
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

    private fun validateScanResult(filterBleId: String, scanResult: ScanResult): Result<Boolean> {
        if (reactContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(BLEException("Bluetooth connect permission is not granted"))
        }

        if (scanResult.device?.name == filterBleId) {
            return Result.success(true)
        } else {
            val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId) ?: return Result.success(false)

            Log.d(MODULE_NAME,"manufacturer " +  String(manufacturerData) + " filter " + filterBleId)

            return Result.success(String(manufacturerData) == filterBleId)
        }
    }
}