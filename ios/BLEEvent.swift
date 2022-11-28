//
//  BLEEventType.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation

enum BLEEventType: String, CaseIterable {
    case messageReceived = "ble-message-received"
    case startedMessageReceive = "ble-started-message-receive"
    case connectingToServer = "ble-connecting-to-server"
    case connectedToServer = "ble-connected-to-server"
    case disconnectingFromServer = "ble-disconnecting-from-server"
    case disconnectedFromServer = "ble-disconnected-from-server"
    case clientConnected = "ble-device-connected"
    case clientDisconnected = "ble-device-disconnected"
    case sendingMessage = "ble-sending-message"
    case messageSent = "ble-message-sent"
}

class BLEEvent {
    let type: BLEEventType
    
    init(type: BLEEventType) {
        self.type = type
    }
}

final class MessageReceivedEvent: BLEEvent {
    let message: String
    
    init(message: String) {
        self.message = message
        super.init(type: .messageReceived)
    }
}
