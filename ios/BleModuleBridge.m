//
//  BLEModuleBridge.m
//  Ble
//
//  Created by AndrewNadraliev on 29.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BleModule, NSObject)

RCT_EXTERN_METHOD(scan:(NSString *)filterBleId stopIfFound:(BOOL)stopIfFound resolve:(RCTPromiseResolveBlock *)resolve reject:(RCTPromiseRejectBlock *)reject)

@end
