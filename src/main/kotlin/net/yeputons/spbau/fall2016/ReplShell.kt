package net.yeputons.spbau.fall2016

import net.yeputons.spbau.fall2016.executables.*
import net.yeputons.spbau.fall2016.parsing.*

fun main(args: Array<String>) {
    ReplShell().run()
}

class ReplShell {
    fun run() : Unit {
        val env = Environment.fromCurrentEnvironment()

        val lineParser = LineParser(env)
        val commandWithArgumentsParser = CommandWithArgumentsParser(env)
        commandWithArgumentsParser.addCommand("pwd", { command, args, env -> PwdCommand(args, env) })
        commandWithArgumentsParser.addCommand("echo", { command, args, env -> EchoCommand(args) })
        commandWithArgumentsParser.addCommand("cat", { command, args, env -> CatCommand(args, env) })
        commandWithArgumentsParser.addCommand("wc", { command, args, env -> WcCommand(args, env) })
        commandWithArgumentsParser.addCommand("grep", { command, args, env -> GrepCommand(args, env) })

        var exitShell: Boolean = false
        commandWithArgumentsParser.addCommand("exit", { command, args, env -> object : BuiltinExecutable() {
            override fun call(): Int {
                exitShell = true
                return 0
            }
        }})

        val executableBuilder = ExecutableBuilder(commandWithArgumentsParser)

        while (!exitShell) {
            print(">>> ")
            val line = readLine() ?: break
            val tokens =
                try {
                    lineParser.parse(line)
                } catch (e : LineParserException) {
                    println("Unable to parse command line: ${e.message}\n")
                    continue
                }
            val executable: Executable =
                try {
                    executableBuilder.build(tokens)
                } catch (e: ParserException) {
                    println("Unable to parse command line: ${e.message}\n")
                    continue
                }
            executable.start(inheritStdin = true, inheritStdout = true)
            executable.waitForTermination()
        }
    }
}
