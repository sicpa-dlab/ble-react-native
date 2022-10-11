package com.sicpa.ble.reactnative

import no.nordicsemi.android.ble.data.DataSplitter
import kotlin.math.min

class MessageSplitter: DataSplitter {

    override fun chunk(message: ByteArray, index: Int, maxLength: Int): ByteArray? =
        if (maxLength * (index - 1) >= message.size) {
            null
        } else if (maxLength * index >= message.size) {
            " ".encodeToByteArray()
        } else {
            message.sliceArray((maxLength * index) until min(maxLength * (index + 1), message.size))
        }
}
