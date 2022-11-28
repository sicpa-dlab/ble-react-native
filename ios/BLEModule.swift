//
//  BleModule.swift
//  Ble
//
//  Created by AndrewNadraliev on 29.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation
import React

@available(iOS 13.0, *)
@objc(BLEModule)
class BLEModule: RCTEventEmitter {
    
    private let PAYLOAD_STRING_KEY = "payload"
    
    private let tag = "BLEModule"
    
    private let bleClient = BLEClient()
    private let bleServer = BLEServer()
    
    @objc(supportedEvents)
    override func supportedEvents() -> [String]! {
        return BLEEventType.allCases.map { $0.rawValue }
    }
    
    @objc(constantsToExport)
    override func constantsToExport() -> [AnyHashable : Any]! {
        return ["PAYLOAD_STRING_KEY": PAYLOAD_STRING_KEY]
    }
    
    @objc(generateBleId:reject:)
    func generateBleId(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) -> Void {
        bleServer.generateBleId(completion: { result in
            switch (result) {
            case .success(let bleId):
                resolve?(bleId)
            case .failure(let error):
                reject?(nil, nil, error)
            }
        })
    }
    
    @objc(advertise:resolve:reject:)
    func advertise(_ bleId: String, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        bleServer.advertise(bleId, completion: { result in
            switch (result) {
            case .success(_):
                resolve?(nil)
            case .failure(let error):
                reject?(nil, nil, error)
            }
        })
    }
    
    @objc(stopAdvertise:reject:)
    func stopAdvertise(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        bleServer.stopAdvertise()
        resolve?(nil)
    }
    
    @objc(start)
    func start() -> Void {
        let onEventFired = { [unowned self] (event: BLEEvent) -> Void in
            var body: [String:Any] = [:]
            if let messageReceivedEvent = event as? MessageReceivedEvent {
                body[PAYLOAD_STRING_KEY] = messageReceivedEvent.message
            }
            
            sendEvent(withName: event.type.rawValue, body: body)
        }
        
        bleClient.start()
        bleClient.onEventFired = onEventFired
        
        bleServer.start()
        bleServer.onEventFired = onEventFired
    }
    
    @objc(stop)
    func stop() -> Void {
        bleClient.stop()
        bleServer.stop()
    }
    
    @objc(scan:stopIfFound:resolve:reject:)
    func scan(_ filterBleId: String, stopIfFound: Bool, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) -> Void {
        Task.init(operation: {
            switch (await bleClient.scan(filterBleId: filterBleId, stopIfFound: stopIfFound)) {
            case .success(let identifier):
                resolve?(identifier)
            case .failure(let error):
                reject?(nil, nil, error)
            }
        })
    }
    
    @objc(stopScan:reject:)
    func stopScan(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        bleClient.stopScan()
        resolve?(nil)
    }
    
    @objc(connectToPeripheral:resolve:reject:)
    func connectToPeripheral(_ address: String, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) -> Void {
        bleClient.connectToPeripheral(identifier: address, completion: { (result: Result<Any?, BLEError>) -> Void in
            switch (result) {
            case .success(_):
                resolve?(nil)
            case .failure(let error):
                reject?(nil, nil, error)
            }
        })
    }
    
    @objc(sendMessage:resolve:reject:)
    func sendMessage(_ message: String, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        if (bleClient.isReady()) {
            bleClient.sendMessage(message: message) { result in
                switch (result) {
                case .success(_):
                    resolve?(nil)
                case .failure(let error):
                    reject?(nil, nil, error)
                }
            }
        } else if (bleServer.isReady()) {
            bleServer.sendMessage(message: message) { result in
                switch (result) {
                case .success(_):
                    resolve?(nil)
                case .failure(let error):
                    reject?(nil, nil, error)
                }
            }
        } else {
            log(tag: tag, message: "No one to send the message to")
            reject?(nil, nil, BLEError(message: "No one to send the message to"))
        }
    }
    
    @objc(disconnect:reject:)
    func disconnect(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        bleClient.disconnect()
        resolve?(nil)
    }

    @objc(finish:reject:)
    func finish(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        bleClient.finish()
        bleServer.finish()
        resolve?(nil)
    }
    
}
