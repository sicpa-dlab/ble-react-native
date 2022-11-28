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
class BLEClient : NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    private let tag = "BLEClient"
    private let zeroCharacterSetWithWhitespaces = CharacterSet(charactersIn: "\0").union(.whitespacesAndNewlines)
    
    public var onEventFired: ((BLEEvent) -> Void)?
    
    private lazy var centralManager = CBCentralManager(delegate: self, queue: nil)
    private var connectedPeripheral: CBPeripheral?
    private var characteristic: CBCharacteristic?
    
    private var scanResults: AsyncStream<Result<Peripheral, BLEError>>?
    private var scanResultsContinuation: AsyncStream<Result<Peripheral, BLEError>>.Continuation?
    
    private var connectCompletion: ((Result<Any?, BLEError>) -> Void)?
    private var sendMessageCompletion: ((Result<Any?, BLEError>) -> Void)?
    
    private var inboundBuffer = ""    
    
    func isReady() -> Bool {
        return connectedPeripheral != nil
        && connectCompletion == nil // no connection is in progress
        && characteristic != nil
    }
    
    func start() {
        // check the state to init bluetooth manager
        centralManager.state
    }
    
    func stop() {
        // nothing to clean up
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
    
    func stopScan() {
        centralManager.stopScan()
    }
    
    func connectToPeripheral(identifier: String, completion: @escaping (Result<Any?, BLEError>) -> Void) -> Void {
        log(tag: tag, message: "Trying to connect to \(identifier)")
        guard let uuid = UUID(uuidString: identifier), let peripheral = centralManager.retrievePeripherals(withIdentifiers: [uuid]).first else {
            log(tag: tag, message: "Cannot connect to \(identifier), not found.")
            completion(.failure(BLEError(message: "Could not find peripheral with the specified identifier \(identifier)")))
            return
        }
        
        connectCompletion = completion
        connectedPeripheral = peripheral
        
        onEventFired?(.init(type: .connectingToServer))
        centralManager.connect(peripheral)
    }
    
    func sendMessage(message: String, completion: @escaping (Result<Any?, BLEError>) -> Void) {
        log(tag: tag, message: "Trying to send a message to peripheral")
        
        if let characteristic = characteristic, let peripheral = connectedPeripheral {
            onEventFired?(.init(type: .sendingMessage))
            peripheral.writeValue(Data(message.appending("\0").utf8), for: characteristic, type: .withResponse)
            sendMessageCompletion = completion
        } else {
            let bleError = BLEError(message: "Error sending message, probably not connected to peripheral or characteristics not yet discovered")
            log(tag: tag, error: bleError)
            completion(.failure(bleError))
        }
    }
    
    func disconnect() {
        if let peripheral = connectedPeripheral {
            onEventFired?(.init(type: .disconnectingFromServer))
            centralManager.cancelPeripheralConnection(peripheral)
            connectedPeripheral = nil
            characteristic = nil
            onEventFired?(.init(type: .disconnectedFromServer))
            log(tag: tag, message: "Disconnecting from \(peripheral.identifier)")
        }
    }

    func finish() {
        disconnect()
        stopScan()
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
        log(tag: tag, message: "Successfully connected to peer \(peripheral.identifier.uuidString), discovering services...")
        peripheral.delegate = self
        peripheral.discoverServices([SERVICE_ID])
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        let bleError = BLEError(message: "Failed to connect to peripheral \(peripheral.identifier.uuidString)", cause: error)
        connectedPeripheral = nil
        log(tag: tag, error: bleError)
        connectCompletion?(.failure(bleError))
        connectCompletion = nil
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            let bleError = BLEError(message: "Error discovering services of peripheral \(peripheral.identifier.uuidString)", cause: error)
            log(tag: tag, error: bleError)
            connectCompletion?(.failure(bleError))
            connectCompletion = nil
        } else {
            log(tag: tag, message: "Successfully discovered services, looking for our service...")
            
            if let service = peripheral.services?.first(where: { $0.uuid == SERVICE_ID}) {
                log(tag: tag, message: "Found our service, discovering characteristics...")
                peripheral.discoverCharacteristics([CHARACTERISTIC_ID], for: service)
            } else {
                let bleError = BLEError(message: "Our service was not found, something went horribly wrong")
                log(tag: tag, error: bleError)
                connectCompletion?(.failure(bleError))
                connectCompletion = nil
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error = error {
            let bleError = BLEError(message: "Error discovering characteristics of peripheral \(peripheral.identifier.uuidString)", cause: error)
            log(tag: tag, error: bleError)
            connectCompletion?(.failure(bleError))
            connectCompletion = nil
        } else {
            log(tag: tag, message: "Successfully discovered characteristics, looking for our characteristic...")
            
            if let characteristic = service.characteristics?.first(where: { $0.uuid == CHARACTERISTIC_ID }) {
                log(tag: tag, message: "Found our characteristic, peripheral is ready!")
                self.characteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)
            } else {
                let bleError = BLEError(message: "Our characteristic was not found, something went horribly wrong")
                log(tag: tag, error: bleError)
                connectCompletion?(.failure(bleError))
                connectCompletion = nil
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        log(tag: tag, message: "Characteristic value updated")
        
        guard let data = characteristic.value else {
            log(tag: tag, message: "Update was empty")
            return
        }
        
        guard let dataStr = String(data: data, encoding: String.Encoding.utf8) else {
            log(tag: tag, message: "Could not convert received data into a string")
            return
        }
        
        if !dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces).isEmpty {
            log(tag: tag, message: "Received data: \(dataStr)")
            if dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces) == "ready" {
                connectCompletion?(.success(nil))
                connectCompletion = nil
                onEventFired?(.init(type: .connectedToServer))
            } else if dataStr.contains("\0") {
                log(tag: tag, message: "Received last update part, sending to RN")
                let message = inboundBuffer + dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces)
                inboundBuffer = ""
                onEventFired?(MessageReceivedEvent(message: message))
            } else {
                log(tag: tag, message: "Received partial update, buffering...")
                
                if inboundBuffer.isEmpty {
                    onEventFired?(.init(type: .startedMessageReceive))
                }
                
                inboundBuffer += dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces)
            }
        } else {
            log(tag: tag, message: "Update was empty")
        }
    }
        
    /**
     Called after #sendMessage
     */
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        if (self.characteristic?.uuid == characteristic.uuid) {
            if let error = error {
                let bleError = BLEError(message: "Error sending message", cause: error)
                log(tag: tag, error: bleError)
                sendMessageCompletion?(.failure(bleError))
            } else {
                log(tag: tag, message: "Successfully sent the message")
                onEventFired?(.init(type: .messageSent))
                sendMessageCompletion?(.success(nil))
            }
        }
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
