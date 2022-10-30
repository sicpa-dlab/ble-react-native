//
//  BLEEventType.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation

/*
 class MessageReceived(val payload: String): BLEEventType("ble-message-received")
 object StartedMessageReceive: BLEEventType("ble-started-message-receive")
 object ConnectingToServer: BLEEventType("ble-connecting-to-server")
 object ConnectedToServer: BLEEventType("ble-connected-to-server")
 object DisconnectingFromServer: BLEEventType("ble-disconnecting-from-server")
 object DisconnectedFromServer: BLEEventType("ble-disconnected-from-server")
 object ClientConnected: BLEEventType("ble-device-connected")
 object ClientDisconnected: BLEEventType("ble-device-disconnected")
 object SendingMessage: BLEEventType("ble-sending-message")
 object MessageSent: BLEEventType("ble-message-sent")
 */

enum BLEEventType: String, CaseIterable {
    case MessageReceived = "ble-message-received"
    case StartedMessageReceive = "ble-started-message-receive"
    case ConnectingToServer = "ble-connecting-to-server"
    case ConnectedToServer = "ble-connected-to-server"
    case DisconnectingFromServer = "ble-disconnecting-from-server"
    case DisconnectedFromServer = "ble-disconnected-from-server"
    case ClientConnected = "ble-device-connected"
    case ClientDisconnected = "ble-device-disconnected"
    case SendingMessage = "ble-sending-message"
    case MessageSent = "ble-message-sent"
}
