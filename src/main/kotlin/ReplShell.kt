package net.yeputons.spbau.fall2016

fun main(args: Array<String>) {
    ReplShell().run()
}

class ReplShell {
    fun run() : Unit {
        val env = Environment.fromCurrentEnvironment()
        val parser = LineParser(env)

        while (true) {
            print(">>> ")
            val line = readLine() ?: break
            val tokens: List<String> =
                try {
                    parser.tokenizeAndSubstitute(line)
                } catch (e : ParserException) {
                    println("Unable to parse command line:" + e.message)
                    continue
                }
            println("Parsed into %d elements: %s".format(tokens.size, tokens.toString()))
        }
    }
}
