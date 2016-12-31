package net.yeputons.spbau.fall2016

import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

/**
 * Helper class which represents a thread which transfers bytes from
 * <code>from</code> stream to <code>to</code> stream. Terminates automatically
 * on either EOF or <code>IOException</code> in either stream.
 * Closes <code>from</code> stream in the end, also closes <code>to</code> stream
 * by default (can be disabled with constructor parameter).
 */
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