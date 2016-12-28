package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.Environment
import org.junit.Assert.*
import org.junit.Test

class ExternalProcessTest {
    @Test fun testNonExistingCommand() {
        val (exitCode, output) = ExecutableTest.run(ExternalProcess("some-non-existing-command-for-yeputons-shell", listOf(), Environment()))
        assertNotEquals("", output)
        assertNotEquals(0, exitCode)
    }

    @Test fun testExistingCommandCat() {
        val (exitCode, output) = ExecutableTest.run(ExternalProcess("cat", listOf("--version"), Environment.fromCurrentEnvironment()))
        assertEquals(0, exitCode)
    }

    @Test fun testCat() {
        val (exitCode, output) = ExecutableTest.run(ExternalProcess("cat", listOf(), Environment.fromCurrentEnvironment()), "sample\ntext")
        assertEquals("sample\ntext", output)
        assertEquals(0, exitCode)
    }

    @Test fun testExitCodeInRunningCommand() {
        val executable = ExternalProcess("cat", listOf(), Environment.fromCurrentEnvironment())
        executable.start()
        assertNull(executable.exitCode)
        executable.stdin.close()
        executable.stdout.close()
        executable.waitForTermination()
    }
}

