package com.sicpa.ble.reactnative

class BLEException(override val message: String?, override val cause: Throwable? = null): Exception(message, cause)