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
class BLEModule: NSObject {
    
    private let bleClient = BLEClient()
    
    @objc(start)
    func start() -> Void {
        bleClient.start()
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
                if let resolve = resolve {
                    resolve(identifier)
                }
            case .failure(let error):
                if let reject = reject {
                    reject(nil, nil, error)
                }
            }
        })
    }
    
    func stopScan(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        Task.init(operation: {
            await bleClient.stopScan()
        })
    }
    
    func connectToPeripheral(_ address: String, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }
    
    func sendMessage(_ message: String, resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }
    
    func disconnect(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }

    func finish(_ resolve: RCTPromiseResolveBlock?, reject: RCTPromiseRejectBlock?) {
        
    }
    
}
