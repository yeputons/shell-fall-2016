package net.yeputons.spbau.fall2016

import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

class StreamPipingThread(val from: InputStream, val to: OutputStream, val closeTo: Boolean = true) : Thread("StreamPipingThread") {
    override fun run() {
        try {
            while (true) {
                val symbol = from.read()
                if (symbol == -1) {
                    break
                }
                to.write(symbol)
            }
        } catch (_: IOException) {
            // Either of two pipes became broken
        }
        try {
            from.close()
        } catch (_: IOException) {
        }
        try {
            if (closeTo) {
                to.close()
            }
        } catch (_: IOException) {
        }
    }
}