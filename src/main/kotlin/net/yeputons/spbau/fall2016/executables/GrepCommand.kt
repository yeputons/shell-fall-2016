package net.yeputons.spbau.fall2016.executables

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import net.yeputons.spbau.fall2016.Environment
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Class which contains grep's configuration.
 */
class GrepConfig {
    @Parameter(names = arrayOf("-i"), description = "Ignore case")
    @JvmField
    var caseInsensitive: Boolean = false

    @Parameter(names = arrayOf("-w"), description = "Match full words only")
    @JvmField
    var fullWordsOnly: Boolean = false

    @Parameter(names = arrayOf("-A"), description = "Print specified number of lines after match")
    @JvmField
    var printAfter: Int? = null

    @Parameter(required = true)
    @JvmField
    var positionals: List<String> = mutableListOf()

    var regex: Regex = Regex("")

    var files: List<String> = listOf()

    companion object {
        fun fromArguments(args: List<String>): GrepConfig {
            val config = GrepConfig()
            JCommander(config, *args.toTypedArray())
            if (config.positionals.isEmpty())
                throw ParameterException("Not enough arguments for grep")
            try {
                val regexOptions =
                        if (config.caseInsensitive) setOf(RegexOption.IGNORE_CASE) else setOf()
                config.regex = Regex(config.positionals[0], regexOptions)
            } catch (e: PatternSyntaxException) {
                throw ParameterException("Invalid pattern for grep: ${e.message}", e)
            }
            config.files =
                    if (config.positionals.size > 1)
                        config.positionals.subList(1, config.positionals.size)
                    else
                        listOf("-")
            return config
        }
    }
    fun calculateRegex() {
    }

    override fun toString(): String {
        return "GrepConfig(caseInsensitive=$caseInsensitive, fullWordsOnly=$fullWordsOnly, printAfter=$printAfter, positionals=$positionals)"
    }
}

/**
 * Helper class which processes lines from different files one by one and produces
 * expected grep's output, including separators and file names.
 */
class GrepProcessor(val config: GrepConfig) {
    companion object {
        val WORD_SPLIT_REGEX = Pattern.compile("\\b")
    }

    private var remainingInGroup = 0
    var groupsFound = false
        get() = field
        private set(newValue) {
            field = newValue
        }
    private var previousPrinted = false

    fun process(line: String, fileNameToPrint: String?): Sequence<String> {
        val currentMatches =
                if (config.fullWordsOnly) {
                    line.split(WORD_SPLIT_REGEX).any({ it.matches(config.regex) })
                } else {
                    line.contains(config.regex)
                }
        if (currentMatches) {
            remainingInGroup = (config.printAfter ?: 0) + 1
        }
        val isInBlock =
                if (remainingInGroup > 0) {
                    remainingInGroup--
                    true
                } else {
                    false
                }
        if (!isInBlock) {
            previousPrinted = false
            return emptySequence()
        }

        val result = mutableListOf<String>()
        if (config.printAfter != null) {
            if (!previousPrinted && groupsFound) {
                result += "--"
            }
        }
        result +=
                if (fileNameToPrint == null) line
                else {
                    val sep = if (currentMatches) ":" else "-"
                    "$fileNameToPrint$sep$line"
                }
        groupsFound = true
        previousPrinted = true
        return result.asSequence()
    }

    fun fileEnded() {
        remainingInGroup = 0
        previousPrinted = false
    }
}

/**
 * Main class for grep command, parses arguments, reads files and uses GrepProcessor to output the answer.
 */
class GrepCommand(val args: List<String>, val env: Environment) : BuiltinExecutable() {
    override fun call(): Int {
        val config = GrepConfig.fromArguments(args)
        val processor = GrepProcessor(config)
        for (fileName in config.files) {
            val fileNameToPrint = if (config.files.size > 1) fileName else null
            try {
                val inp = if (fileName == "-") input else FileInputStream(env.getFile(fileName))
                Sequence { BufferedReader(inp.reader(), 1).lines().iterator() }
                        .flatMap { processor.process(it, fileNameToPrint) }
                        .forEach { outputWrite("$it\n") }
                processor.fileEnded()
                if (fileName != "-") {
                    try {
                        inp.close()
                    } catch (_: IOException) {
                    }
                }
            } catch (e: IOException) {
                outputWrite(e.toString() + "\n")
            }
        }
        return if (processor.groupsFound) 0 else 1
    }
}
