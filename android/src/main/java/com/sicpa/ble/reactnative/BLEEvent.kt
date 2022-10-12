package com.sicpa.ble.reactnative

sealed class BLEEvent(val type: String)

class MessageReceived(val payload: String): BLEEvent("ble-message-received")