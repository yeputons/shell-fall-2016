package net.yeputons.spbau.fall2016.parsing

import net.yeputons.spbau.fall2016.executables.EnvvarAssignment
import net.yeputons.spbau.fall2016.executables.Executable
import net.yeputons.spbau.fall2016.executables.ExternalProcess
import net.yeputons.spbau.fall2016.executables.PipedExecutable

class CommandWithArgumentsParser(val environment: net.yeputons.spbau.fall2016.Environment) {
    val commands = mutableMapOf<String, (String, List<String>) -> Executable>()

    fun addCommand(command: String, commandSupplier: (String, List<String>) -> Executable) {
        commands.put(command, commandSupplier)
    }

    fun addCommand(command: String, commandSupplier: (String, List<String>, net.yeputons.spbau.fall2016.Environment) -> Executable) {
        addCommand(command, { command, args -> commandSupplier(command, args, environment) })
    }

    fun constructCommand(command: String, args: List<String>): Executable {
        val supplier = commands[command]
        if (supplier != null) {
            return supplier(command, args)
        } else {
            return ExternalProcess(command, args, environment)
        }
    }

    fun parse(line: List<String>): Executable {
        if (line.isEmpty()) {
            throw net.yeputons.spbau.fall2016.parsing.ParserException("No tokens to parse into a command")
        }
        val command = line[0]
        val args = line.subList(1, line.size)
        if (args.isEmpty() && command.contains('=')) {
            val assignment = command.split('=', limit = 2)
            return EnvvarAssignment(assignment[0], assignment[1], environment)
        }
        return constructCommand(command, args)
    }
}

class ExecutableBuilder(val commandWithArgumentsParser: net.yeputons.spbau.fall2016.parsing.CommandWithArgumentsParser) {
    companion object {
        fun splitByPipes(originalTokens: List<net.yeputons.spbau.fall2016.parsing.Token>): List<List<String>> {
            if (originalTokens.isEmpty()) {
                return listOf()
            }
            var tokens = originalTokens
            val result = mutableListOf<List<String>>()
            while (true) {
                val currentLine = mutableListOf<String>()
                var isLastLine = true
                while (!tokens.isEmpty()) {
                    val token = tokens[0]
                    tokens = tokens.drop(1)
                    if (!token.startQuoted && token.data == "|") {
                        isLastLine = false
                        break
                    } else {
                        currentLine.add(token.data)
                    }
                }
                result.add(currentLine)
                if (isLastLine) {
                    break
                }
            }
            return result
        }
    }

    fun build(parsedLine: List<net.yeputons.spbau.fall2016.parsing.Token>): Executable {
        val result = net.yeputons.spbau.fall2016.parsing.ExecutableBuilder.Companion.splitByPipes(parsedLine).mapIndexed({ i, line ->
            try {
                commandWithArgumentsParser.parse(line)
            } catch (e: net.yeputons.spbau.fall2016.parsing.ParserException) {
                throw net.yeputons.spbau.fall2016.parsing.ParserException("Illegal command after pipe #" + i, e)
            }
        })
        if (result.isEmpty()) {
            throw net.yeputons.spbau.fall2016.parsing.ParserException("No commands found")
        }
        return result.drop(1).fold(result[0], ::PipedExecutable)
    }
}

class ParserException(message: String, cause: Throwable? = null) : Exception(message, cause)