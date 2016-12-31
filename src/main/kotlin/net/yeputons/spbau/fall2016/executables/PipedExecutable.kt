package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.StreamPipingThread
import java.io.InputStream
import java.io.OutputStream

class PipedExecutable(val executableA: Executable, val executableB: Executable) : Executable {
    var thread: Thread? = null

    override fun start(inheritStdin: Boolean, inheritStdout: Boolean) {
        executableA.start(inheritStdin = inheritStdin)
        executableB.start(inheritStdout = inheritStdout)
        thread = StreamPipingThread(executableA.stdout, executableB.stdin)
        thread!!.start()
    }

    override val stdin: OutputStream get() = executableA.stdin
    override val stdout: InputStream get() = executableB.stdout
    override val exitCode: Int? get() {
        val codeA = executableA.exitCode
        val codeB = executableB.exitCode
        if (codeA == null || codeB == null) {
            return null
        }
        if (codeA != 0) {
            return codeA
        }
        if (codeB != 0) {
            return codeB
        }
        return 0
    }

    override fun waitForTermination() {
        executableA.waitForTermination()
        executableB.waitForTermination()
    }
}