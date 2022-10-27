package com.sicpa.ble.reactnative

const val BLEEvent = "ble-event"

sealed class BLEEventType(val type: String)

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
