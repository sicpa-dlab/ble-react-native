package com.sicpa.ble.reactnative

import android.util.Log
import no.nordicsemi.android.ble.data.DataMerger
import no.nordicsemi.android.ble.data.DataStream

class MessageMerger: DataMerger {

    override fun merge(output: DataStream, lastPacket: ByteArray?, index: Int): Boolean {
        if (lastPacket?.last() == (0x00).toByte()) {
            output.write(lastPacket.dropLast(1).toByteArray())
            return true
        }
        output.write(lastPacket)
        return String(lastPacket ?: byteArrayOf()).isBlank()
    }
}
