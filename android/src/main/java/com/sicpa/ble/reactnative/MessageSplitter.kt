package com.sicpa.ble.reactnative

import no.nordicsemi.android.ble.data.DataSplitter
import kotlin.math.min

class MessageSplitter: DataSplitter {

    override fun chunk(message: ByteArray, index: Int, maxLength: Int): ByteArray? =
        if (maxLength * index == message.size) {
            byteArrayOf(0x00)
        } else if (maxLength * index > message.size) {
            null
        } else {
            message.sliceArray((maxLength * index) until min(maxLength * (index + 1), message.size))
                .let {
                    if (maxLength * (index + 1) > message.size) {
                        return@let it.toMutableList().apply { add(0x00) }.toByteArray()
                    } else return@let it
                }
        }
}
