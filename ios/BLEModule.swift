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
    
    private let bleClient = BLEClient()
    
    @objc(supportedEvents)
    override func supportedEvents() -> [String]! {
        return BLEEventType.allCases.map { $0.rawValue }
    }
    
    @objc(start)
    func start() -> Void {
        bleClient.start()
        bleClient.onMessageReceivedListener = { [weak self] value in
            
            self?.sendEvent(withName: BLEEventType.MessageReceived.rawValue, body: ["payload": value])
        }
    }
    
    @objc(stop)
    func stop() -> Void {
        bleClient.stop()
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
        Task.init(operation: {
            await bleClient.stopScan()
        })
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
        
    }
    
    @objc(disconnect:reject:)
    func disconnect(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }

    @objc(finish:reject:)
    func finish(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }
    
}
