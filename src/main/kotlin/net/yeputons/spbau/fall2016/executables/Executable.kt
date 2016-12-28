package net.yeputons.spbau.fall2016.executables

import java.io.*

/**
 * Invariant: both stdin and stdout should be closed by Executable's owner.
 * stdin can also be closed (became broken) at any moment by Executable.
 */
interface Executable {
    fun start(inheritStdin: Boolean = false, inheritStdout: Boolean = false): Unit
    val stdin: OutputStream
    val stdout: InputStream
    val exitCode: Int?
    fun waitForTermination(): Unit
}
