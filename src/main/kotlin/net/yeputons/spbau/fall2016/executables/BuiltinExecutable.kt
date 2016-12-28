package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.Environment
import java.io.*
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.regex.Pattern

abstract class BuiltinExecutable() : Executable, Callable<Int> {
    var futureTask: FutureTask<Int>? = null

    final override fun start(inheritStdin: Boolean, inheritStdout: Boolean) {
        if (inheritStdin) {
            input = System.`in`
        }
        if (inheritStdout) {
            output = System.out
        }
        futureTask = FutureTask(Callable<Int> {
            val result = this@BuiltinExecutable.call()
            if (!inheritStdin) {
                input.close()
            }
            if (!inheritStdout) {
                output.close()
            }
            result
        })
        Thread(futureTask).start()
    }

    final override val stdin = PipedOutputStream()
    final override val stdout = PipedInputStream()
    final override val exitCode: Int? get() {
        if (futureTask == null) {
            return null
        }
        val task = futureTask!!
        if (task.isDone) {
            return task.get()
        } else {
            return null
        }
    }

    protected var input: InputStream = PipedInputStream(stdin)
    private var output: OutputStream = PipedOutputStream(stdout)

    final override fun waitForTermination() {
        if (futureTask == null) {
            throw IllegalStateException("Executable was not started yet")
        }
        futureTask!!.get()
    }

    protected fun outputWrite(s: ByteArray) {
        try {
            output.write(s)
        } catch (_: IOException) {
            // Do nothing, pipe is probably broken
        }
    }

    protected fun outputWrite(s: String) {
        outputWrite(s.toByteArray())
    }
}

class EnvvarAssignment(val name: String, val value: String, val env: Environment) : BuiltinExecutable() {
    override fun call(): Int {
        env[name] = value
        return 0
    }
}

class PwdCommand(val args: List<String>, val env: Environment) : BuiltinExecutable() {
    override fun call(): Int {
        val result = if (args.isNotEmpty()) {
            outputWrite("No arguments expected for pwd\n")
            1
        } else {
            outputWrite(env.currentDirectory.toString() + "\n")
            0
        }
        return result
    }
}

class EchoCommand(val args: List<String>) : BuiltinExecutable() {
    override fun call(): Int {
        outputWrite(args.joinToString(" ") + "\n")
        return 0
    }
}

class CatCommand(val args: List<String>, val env: Environment) : BuiltinExecutable() {
    override fun call(): Int {
        val files =
                if (args.isEmpty()) {
                    listOf("-")
                } else {
                    args
                }
        var result = 0
        for (fileName in files) {
            val inp =
                    if (fileName == "-") {
                        input
                    } else {
                        try {
                            FileInputStream(File(env.currentDirectory, fileName))
                        } catch (e: IOException) {
                            outputWrite(e.toString() + "\n")
                            result = 1
                            continue
                        }
                    }
            while (true) {
                val byte = inp.read()
                if (byte == -1) {
                    break
                }
                outputWrite(byteArrayOf(byte.toByte()))
            }
            if (fileName != "-") {
                try {
                    inp.close()
                } catch (_: IOException) {
                }
            }
        }
        return result
    }
}

class WcCommand(val args: List<String>, val env: Environment) : BuiltinExecutable() {
    var totalLines = 0
    var totalWords = 0
    var totalChars = 0
    val printPerFileStats = args.isNotEmpty()

    companion object {
        val WORDS_SEPARATOR = Pattern.compile("\\s+")!!
    }

    fun processFile(data: String, fileName: String) {
        if (data.isEmpty()) {
            return
        }
        val curLines = data.split("\n").size - if (data.endsWith("\n")) 1 else 0
        val curWords = data.split(WORDS_SEPARATOR).filter({ it != "" }).size
        val curChars = data.length
        if (printPerFileStats) {
            outputWrite("$curLines $curWords $curChars $fileName\n")
        }
        totalLines += curLines
        totalWords += curWords
        totalChars += curChars
    }

    override fun call(): Int {
        val files =
                if (args.isEmpty()) {
                    listOf("-")
                } else {
                    args
                }
        var result = 0
        for (fileName in files) {
            val inp =
                    if (fileName == "-") {
                        input
                    } else {
                        try {
                            FileInputStream(File(env.currentDirectory, fileName))
                        } catch (e: IOException) {
                            outputWrite(e.toString() + "\n")
                            result = 1
                            continue
                        }
                    }
            val data = inp.bufferedReader().readText()
            processFile(data, fileName)
            if (fileName != "-") {
                try {
                    inp.close()
                } catch (_: IOException) {
                }
            }
        }
        if (args.size >= 2) {
            outputWrite("$totalLines $totalWords $totalChars total\n")
        } else if (args.isEmpty()) {
            outputWrite("$totalLines $totalWords $totalChars\n")
        }
        return result
    }
}