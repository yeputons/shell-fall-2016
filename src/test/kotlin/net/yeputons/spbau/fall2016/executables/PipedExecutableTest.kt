package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.Environment
import org.junit.Assert.*
import org.junit.Test

class PipedExecutableTest {
    fun testPipedExitCode(codeA: Int, codeB: Int, expectedCode: Int) {
        val executableA = object : BuiltinExecutable() {
            override fun call(): Int {
                return codeA
            }
        }
        val executableB = object : BuiltinExecutable() {
            override fun call(): Int {
                return codeB
            }
        }
        val (exitCode, output) = ExecutableTest.run(PipedExecutable(executableA, executableB))
        assertEquals("", output)
        assertEquals(expectedCode, exitCode)
    }

    @Test fun testPipedExitCodeSuccess() {
        testPipedExitCode(0, 0, 0)
    }

    @Test fun testPipedExitCodeSecondFails() {
        testPipedExitCode(0, 1, 1)
    }

    @Test fun testPipedExitCodeFirstFails() {
        testPipedExitCode(1, 0, 1)
    }

    @Test fun testPipedExitCodeBothFail() {
        testPipedExitCode(1, 2, 1)
    }

    @Test fun testPipedOutput() {
        val executableA = object : BuiltinExecutable() {
            override fun call(): Int {
                assertEquals('a'.toInt(), input.read())
                assertEquals('b'.toInt(), input.read())
                assertEquals(-1, input.read())
                outputWrite("cd")
                return 0
            }
        }
        val executableB = object : BuiltinExecutable() {
            override fun call(): Int {
                assertEquals('c'.toInt(), input.read())
                assertEquals('d'.toInt(), input.read())
                assertEquals(-1, input.read())
                outputWrite("ef")
                return 0
            }
        }
        val (exitCode, output) = ExecutableTest.run(PipedExecutable(executableA, executableB), "ab")
        assertEquals(0, exitCode)
        assertEquals("ef", output)
    }

    @Test fun testPipedExternalProcess() {
        val executableA = ExternalProcess("some-unexisting-process-a", listOf(), Environment())
        val executableB = ExternalProcess("some-unexisting-process-b", listOf(), Environment())
        val (exitCode, output) = ExecutableTest.run(PipedExecutable(executableA, executableB))
        assertNotEquals(0, exitCode)
    }
}