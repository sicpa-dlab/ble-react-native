//
//  BLEError.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation

struct BLEError : LocalizedError {
    let message: String
    let cause: Error? = nil
    
    var errorDescription: String? {
        get {
            return message
        }
    }
    
}
