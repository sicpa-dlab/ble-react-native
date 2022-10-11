package com.sicpa.ble.reactnative

import no.nordicsemi.android.ble.data.DataMerger
import no.nordicsemi.android.ble.data.DataStream

class MessageMerger: DataMerger {

    override fun merge(output: DataStream, lastPacket: ByteArray?, index: Int): Boolean {
        output.write(lastPacket)
        return String(lastPacket ?: byteArrayOf()).isBlank()
    }
}
