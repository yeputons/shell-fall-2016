package net.yeputons.spbau.fall2016.executables

object ExecutableTest {
    fun run(executable: Executable, input: String? = null): Pair<Int, String> {
        executable.start()
        if (input != null) {
            executable.stdin.write(input.toByteArray())
        }
        executable.stdin.close()
        executable.waitForTermination()
        val output = executable.stdout.bufferedReader().readText()
        executable.stdout.close()
        val exitCode = executable.exitCode!!
        return Pair(exitCode, output)
    }
}
