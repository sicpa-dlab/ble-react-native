//
//  BLEClient.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation
import CoreBluetooth
import Combine

@available(iOS 13.0, *)
class BLEClient : NSObject, CBCentralManagerDelegate {
    
    private let tag = "BLEClient"
    
    private lazy var centralManager = CBCentralManager(delegate: self, queue: nil)
    private var connectedPeripheral: CBPeripheral?
    
    private var scanResults: AsyncStream<Result<Peripheral, BLEError>>?
    private var scanResultsContinuation: AsyncStream<Result<Peripheral, BLEError>>.Continuation?
    
    func start() {
        // check the state to init bluetooth manager
        centralManager.state
    }
    
    func scan(filterBleId: String, stopIfFound: Bool) async -> Result<String, BLEError> {
        log(tag: tag, message: "Starting scan")
        
        if case .failure(let error) = assertBluetoothState() {
            return .failure(error)
        }
        
        scanResultsContinuation?.finish()
        (scanResultsContinuation, scanResults) = AsyncStream.pipe()
        
        centralManager.scanForPeripherals(withServices: [SERVICE_ID])
        
        if let resultsStream = scanResults {
            for await result in resultsStream {
                switch (result) {
                case .success(let peripheral):
                    if (validatePeripheral(peripheral: peripheral, bleId: filterBleId)) {
                        log(tag: tag, message: "Found a valid peripheral")
                        if (stopIfFound) {
                            centralManager.stopScan()
                        }
                        return .success(peripheral.cbPeripheral.identifier.uuidString)
                    } else {
                        log(tag: tag, message: "Found a peripheral, but it is not valid")
                    }
                case .failure(let error):
                    centralManager.stopScan()
                    log(tag: tag, message: "Scan finished with an error")
                    return .failure(error)
                }
            }
            
            return .failure(BLEError(message: "Could not find any valid peripherals"))
        } else {
            centralManager.stopScan()
            log(tag: tag, message: "Results stream is nil, cannot return scan results")
            return .failure(BLEError(message: "Results stream is nil, cannot return scan results"))
        }
    }
    
    func stopScan() async {
        centralManager.stopScan()
    }
    
    func connectToPeripheral(address: String) async {
        
    }
    
    func sendMessage(message: String) async {
        
    }
    
    func disconnect() async {
        
    }

    func finish() async {
        await disconnect()
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .unknown:
            log(tag: tag, message: "Bluetooth Device is UNKNOWN")
        case .unsupported:
            log(tag: tag, message: "Bluetooth Device is UNSUPPORTED")
        case .unauthorized:
            log(tag: tag, message: "Bluetooth Device is UNAUTHORIZED")
        case .resetting:
            log(tag: tag, message: "Bluetooth Device is RESETTING")
        case .poweredOff:
            log(tag: tag, message: "Bluetooth Device is POWERED OFF")
        case .poweredOn:
            log(tag: tag, message: "Bluetooth Device is POWERED ON")
        @unknown default:
            log(tag: tag, message: "Unknown State")
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        log(tag: tag, message: "Discovered peer \(peripheral.identifier)")
        scanResultsContinuation?.yield(.success(Peripheral(cbPeripheral: peripheral, advertisementData: advertisementData)))
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
//        print("Connected to peer \(peripheral.identifier)")
//        connected = true
//        centralManager?.stopScan()
//        peripheral.delegate = self
//        peripheral.discoverServices([service])
    }
    
    private func assertBluetoothState() -> Result<Any?, BLEError> {
        if (centralManager.state == .unauthorized) {
            return .failure(BLEError(message: "No permission to use Bluetooth"))
        } else if (centralManager.state == .poweredOff) {
            return .failure(BLEError(message: "Bluetooth is disabled"))
        } else if (centralManager.state != .poweredOn) {
            return .failure(BLEError(message: "Bluetooth manager is not ready, state: \(centralManager.state.rawValue)"))
        } else {
            return .success(nil)
        }
    }
    
    private func validatePeripheral(peripheral: Peripheral, bleId: String) -> Bool {
        if (peripheral.cbPeripheral.name == bleId) {
            return true
        } else {
            let manufacturerData = peripheral.advertisementData[CBAdvertisementDataManufacturerDataKey]
            if let data = manufacturerData as? Data {
                let sanitizedData = data.dropFirst(2)   // first two bytes is manufacturer id
                let extractedBleId = String(decoding: sanitizedData, as: Unicode.UTF8.self)
                return bleId == extractedBleId
            }
            
            return false
        }
    }
        
}
