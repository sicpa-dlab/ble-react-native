//
//  BLEModuleBridge.m
//  Ble
//
//  Created by AndrewNadraliev on 29.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BLEModule, NSObject)

RCT_EXTERN_METHOD(scan:(NSString *)filterBleId stopIfFound:(BOOL)stopIfFound resolve:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(start)

RCT_EXTERN_METHOD(stop)

RCT_EXTERN_METHOD(stopScan:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(connectToPeripheral:(NSString *)address resolve:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(sendMessage:(NSString *)message resolve:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(disconnect:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(finish:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(generateBleId:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(advertise:(NSString *)bleId resolve:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

RCT_EXTERN_METHOD(stopAdvertise:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

@end
