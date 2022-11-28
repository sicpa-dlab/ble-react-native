//
//  BLEServer.swift
//  Ble
//
//  Created by AndrewNadraliev on 28.11.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation
import CoreBluetooth

@available(iOS 13.0, *)
class BLEServer: NSObject, CBPeripheralManagerDelegate {
    
    private let tag = "BLEServer"
    private let zeroCharacterSetWithWhitespaces = CharacterSet(charactersIn: "\0").union(.whitespacesAndNewlines)
    
    public var onMessageReceivedListener: ((String) -> Void)?
    
    private lazy var peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
    private var characteristic: CBMutableCharacteristic?
    
    private var inboundBuffer = ""
    
    private var advertiseCompletion: ((Result<Any?, BLEError>) -> Void)?
    
    private var outboundQueue: [Data] = []
    
    func isReady() -> Bool {
        return true
    }
    
    func start() -> Void {
        // check the state to init bluetooth manager
        peripheralManager.state
    }
    
    func stop() -> Void {
        // nothing to clean up
    }
    
    func generateBleId(completion: @escaping (Result<String, BLEError>) -> Void) -> Void {
        completion(.success(String(Int.random(in: 1000..<10000))))
    }
    
    func advertise(_ bleId: String, completion: @escaping (Result<Any?, BLEError>) -> Void) {
        advertiseCompletion = completion
        
        if characteristic == nil {
            addServices()
        }
        
        log(tag: tag, message: "Starting BLE advertise")
        peripheralManager.startAdvertising([CBAdvertisementDataLocalNameKey : bleId, CBAdvertisementDataServiceUUIDsKey: [SERVICE_ID]])
    }
    
    func stopAdvertise() {
        peripheralManager.stopAdvertising()
    }
    
    func sendMessage(message: String, completion: @escaping (Result<Any?, BLEError>) -> Void) {
        log(tag: tag, message: "Trying to send a message to central")
        
        guard let characteristic = characteristic else {
            let message = "Trying to send a message to central, but the server is not ready"
            log(tag: tag, message: message)
            completion(.failure(BLEError(message: message)))
            return
        }
        
        let chunks = splitMessage(data: Data(message.utf8), maxChunkLength: 180)
        
        for i in chunks.indices {
            let result = peripheralManager.updateValue(chunks[i], for: characteristic, onSubscribedCentrals: nil)
            if !result {
                log(tag: tag, message: "Cannot send the update chunk, the internal queue is full, adding to our own queue")
                outboundQueue.append(contentsOf: chunks.dropFirst(i))
                break
            }
        }
        
    }

    func finish() -> Void {
        stopAdvertise()
    }
    
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        switch peripheral.state {
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
    
    private func addServices() {
        log(tag: tag, message: "Adding services to our BLE peripheral")
        
        let writableCharacteristic = CBMutableCharacteristic(type: CHARACTERISTIC_ID, properties: [.notify, .write, .read], value: nil, permissions: [.readable, .writeable])
        characteristic = writableCharacteristic
        let service = CBMutableService(type: SERVICE_ID, primary: true)
        service.characteristics = [writableCharacteristic]
        peripheralManager.add(service)
    }
    
    func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        if let error = error {
            log(tag: tag, message: "Could not start advertising", error: error)
            advertiseCompletion?(.failure(BLEError(message: "Could not start advertising", cause: error)))
            advertiseCompletion = nil
        } else {
            log(tag: tag, message: "Successfully started advertising")
            advertiseCompletion?(.success(nil))
            advertiseCompletion = nil
        }
    }
    
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        log(tag: tag, message: "Received write requests")
        
        if requests.isEmpty {
            log(tag: tag, message: "Write requests list is empty")
            return
        }
        
        requests.forEach { request in
            handleWriteRequest(request: request)
        }
    }
    
    func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
        // peripheral is ready to send updates again, taking from the queue
        log(tag: tag, message: "Peripheral is ready to send updates again")
        guard let characteristic = characteristic else {
            log(tag: tag, message: "Characteristic is nil after sending some updates, this should not happen, something went terribly wrong")
            return
        }
        
        var updateResult = false
        
        repeat {
            if outboundQueue.isEmpty {
                break
            }
            
            let nextUpdate = outboundQueue.removeFirst()
            updateResult = peripheral.updateValue(nextUpdate, for: characteristic, onSubscribedCentrals: nil)
            
            if !updateResult {
                log(tag: tag, message: "Cannot send the update chunk, the internal queue is full, returning to our own queue")
                outboundQueue.insert(nextUpdate, at: 0)
            }
            
        } while !outboundQueue.isEmpty && updateResult
    }
    
    private func handleWriteRequest(request: CBATTRequest) {
        log(tag: tag, message: "Handling write request")
        
        defer {
            log(tag: tag, message: "Responding to write request")
            peripheralManager.respond(to: request, withResult: .success)
        }
        
        guard let data = request.value else {
            log(tag: tag, message: "Update was empty")
            return
        }
        
        guard let dataStr = String(data: data, encoding: String.Encoding.utf8) else {
            log(tag: tag, message: "Could not convert received data into a string")
            return
        }
        
        if !dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces).isEmpty {
            log(tag: tag, message: "Received data: \(dataStr)")
            if dataStr.contains("\0") {
                log(tag: tag, message: "Received last update part, sending to RN")
                let message = inboundBuffer + dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces)
                inboundBuffer = ""
                onMessageReceivedListener?(message)
            } else {
                log(tag: tag, message: "Received partial update, buffering...")
                inboundBuffer += dataStr.trimmingCharacters(in: zeroCharacterSetWithWhitespaces)
            }
        } else {
            log(tag: tag, message: "Update was empty")
        }
    }
    
}
