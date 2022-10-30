//
//  Peripheral.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation
import CoreBluetooth

struct Peripheral {
    let cbPeripheral: CBPeripheral
    let advertisementData: [String : Any]
}
