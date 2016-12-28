package net.yeputons.spbau.fall2016.executables

import jdk.internal.util.xml.impl.Input
import net.yeputons.spbau.fall2016.Environment
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ExternalProcess(command: String, args: List<String>, env: Environment) : Executable {
    val builder: ProcessBuilder = ProcessBuilder(listOf(command) + args)
            .directory(env.currentDirectory)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)!!
    var process: Process? = null

    // Used whenever error happens during process start
    var stdinOverride: OutputStream? = null
    var stdoutOverride: InputStream? = null
    var exitCodeOverride: Int? = null

    init {
        val subEnv = builder.environment()
        subEnv.clear()
        subEnv.putAll(env.variables)
    }

    private fun process(): Process {
        if (process == null) {
            throw IllegalStateException("Process is not started")
        }
        return process!!
    }

    override fun start(inheritStdin: Boolean, inheritStdout: Boolean) {
        if (process != null) {
            throw IllegalStateException("Process is already started")
        }
        if (inheritStdin) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        if (inheritStdout) {
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        try {
            process = builder.start()
        } catch (e: Exception) {
            stdinOverride = object : OutputStream() {
                override fun write(b: Int) {
                    throw IOException("Stream is closed")
                }
            }
            stdoutOverride = ByteArrayInputStream((e.toString() + "\n").toByteArray())
            exitCodeOverride = 1
        }
    }

    override val stdin: OutputStream get() = stdinOverride ?: process().outputStream!!

    override val stdout: InputStream get() = stdoutOverride ?: process().inputStream!!

    override val exitCode: Int? get() =
        exitCodeOverride ?: try {
            process?.exitValue()
        } catch (_: IllegalThreadStateException) {
            null
        }

    override fun waitForTermination() {
        if (exitCodeOverride == null) {
            process().waitFor()
        }
    }
}