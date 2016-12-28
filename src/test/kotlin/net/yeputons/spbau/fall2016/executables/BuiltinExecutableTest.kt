package net.yeputons.spbau.fall2016.executables

import net.yeputons.spbau.fall2016.Environment
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuiltinExecutableTest {
    @Test fun testExitCodeZero() {
        val (exitCode, output) = ExecutableTest.run(object : BuiltinExecutable() {
            override fun call(): Int {
                return 0
            }
        })
        assertEquals(0, exitCode)
        assertEquals("", output)
    }

    @Test fun testExitCodeNonZero() {
        val (exitCode, output) = ExecutableTest.run(object : BuiltinExecutable() {
            override fun call(): Int {
                return 10
            }
        })
        assertEquals(10, exitCode)
        assertEquals("", output)
    }

    @Test(expected = Exception::class) fun testExecutableTestRunThrows() {
        ExecutableTest.run(object : BuiltinExecutable() {
            override fun call(): Int {
                throw IllegalStateException("Test exception")
            }
        })
    }

    @Test fun testOutput() {
        val (exitCode, output) = ExecutableTest.run(object : BuiltinExecutable() {
            override fun call(): Int {
                outputWrite("hello1\n")
                outputWrite("hello2\n")
                return 0
            }
        })
        assertEquals(0, exitCode)
        assertEquals("hello1\nhello2\n", output)
    }

    @Test fun testInput() {
        val (exitCode, output) = ExecutableTest.run(object : BuiltinExecutable() {
            override fun call(): Int {
                assertEquals("some input", input.bufferedReader().readText())
                return 0
            }
        }, "some input")
        assertEquals(0, exitCode)
        assertEquals("", output)
    }
}

class EnvvarAssignmentTest {
    @Test fun testEnvvarAssignment() {
        val env = Environment()
        val (exitCode, output) = ExecutableTest.run(EnvvarAssignment("var", "value", env))
        assertEquals(0, exitCode)
        assertEquals("", output)
        assertEquals("value", env["var"])
    }
}

class PwdCommandTest {
    @Test fun testPwdCommand() {
        val env = Environment()
        env.currentDirectory = File("some_directory")
        val (exitCode, output) = ExecutableTest.run(PwdCommand(listOf(), env))
        assertEquals(0, exitCode)
        assertEquals("some_directory\n", output)
    }
}

class EchoCommandTest {
    @Test fun testEchoCommand() {
        val (exitCode, output) = ExecutableTest.run(EchoCommand(listOf("hello", "world")))
        assertEquals(0, exitCode)
        assertEquals("hello world\n", output)
    }
}

class CatCommandTest {
    @Rule @JvmField val tmpFolder = TemporaryFolder()

    @Test fun testNoArgs() {
        val (exitCode, output) = ExecutableTest.run(CatCommand(listOf(), Environment()), "some\ndata")
        assertEquals(0, exitCode)
        assertEquals("some\ndata", output)
    }

    @Test fun testSingleDash() {
        val (exitCode, output) = ExecutableTest.run(CatCommand(listOf("-"), Environment()), "some\ndata")
        assertEquals(0, exitCode)
        assertEquals("some\ndata", output)
    }

    @Test fun testNonExistingFile() {
        val (exitCode, output) = ExecutableTest.run(CatCommand(listOf("some-non-existing-file-for-yeputons-shell"), Environment()))
        assertEquals(1, exitCode)
        assertNotEquals("", output)
    }

    @Test fun testExistingFile() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\n")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(CatCommand(listOf("a.txt"), env))

        assertEquals(0, exitCode)
        assertEquals("content of a.txt\n", output)
    }

    @Test fun testConcat() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\n")
        tmpFolder.newFile("b.txt").writeText("content of b.txt\n")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(CatCommand(listOf("a.txt", "-", "b.txt"), env), "stdin\n")

        assertEquals(0, exitCode)
        assertEquals("content of a.txt\nstdin\ncontent of b.txt\n", output)
    }
}

class WcCommandTest {
    @Rule @JvmField val tmpFolder = TemporaryFolder()

    @Test fun testEmpty() {
        val (exitCode, output) = ExecutableTest.run(WcCommand(listOf(), Environment()))
        assertEquals(0, exitCode)
        assertEquals("0 0 0\n", output)
    }

    @Test fun testCounters() {
        val (exitCode, output) = ExecutableTest.run(WcCommand(listOf(), Environment()), "hello world\nthis is some nice test\n")
        assertEquals(0, exitCode)
        assertEquals("2 7 35\n", output)
    }

    @Test fun testNonExistingFile() {
        val (exitCode, output) = ExecutableTest.run(WcCommand(listOf("some-non-existing-file-for-yeputons-shell"), Environment()))
        assertEquals(1, exitCode)
        assertNotEquals("", output)
    }

    @Test fun testExistingFile() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\n")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(WcCommand(listOf("a.txt"), env))

        assertEquals(0, exitCode)
        assertEquals("1 3 17 a.txt\n", output)
    }

    @Test fun testConcat() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\n")
        tmpFolder.newFile("b.txt").writeText("content of b.txt\n")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(WcCommand(listOf("a.txt", "-", "b.txt"), env), "stdin\n")

        assertEquals(0, exitCode)
        assertEquals("1 3 17 a.txt\n1 1 6 -\n1 3 17 b.txt\n3 7 40 total\n", output)
    }
}