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
    let cause: Error?
    
    init(message: String, cause: Error? = nil) {
        self.message = message
        self.cause = cause
    }
    
    var errorDescription: String? {
        get {
            return "\(message). \(cause?.localizedDescription ?? "")"
        }
    }
    
}
