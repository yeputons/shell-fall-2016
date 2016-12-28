package net.yeputons.spbau.fall2016.parsing

import com.nhaarman.mockito_kotlin.mock
import net.yeputons.spbau.fall2016.Environment
import net.yeputons.spbau.fall2016.executables.EnvvarAssignment
import net.yeputons.spbau.fall2016.executables.Executable
import net.yeputons.spbau.fall2016.executables.ExternalProcess
import org.junit.Test
import org.junit.Assert.*

class CommandWithArgumentsParserTest {
    val env = Environment()
    val parser = CommandWithArgumentsParser(env)

    @Test fun testExternalProcess() {
        val command = parser.constructCommand("some_command", listOf("arg 1", "arg 2"))
        assertEquals(ExternalProcess::class.java, command.javaClass)
    }

    @Test fun testCustomCommand() {
        val fake_command = mock<Executable>()
        parser.addCommand("some_command", { name, args, cmd_env ->
            assertEquals("some_command", name)
            assertEquals(listOf("arg 1", "arg 2"), args)
            assertSame(env, cmd_env)
            fake_command
        })

        val command = parser.constructCommand("some_command", listOf("arg 1", "arg 2"))

        assertSame(fake_command, command)
    }

    @Test(expected = ParserException::class) fun testEmptyLine() {
        parser.parse(listOf())
    }

    @Test fun testLineWithArgs() {
        val fake_command = mock<Executable>()
        parser.addCommand("some_command", { name, args, cmd_env ->
            assertEquals("some_command", name)
            assertEquals(listOf("arg 1", "arg 2"), args)
            assertSame(env, cmd_env)
            fake_command
        })

        val command = parser.parse(listOf("some_command", "arg 1", "arg 2"))

        assertSame(fake_command, command)
    }

    @Test fun testLineNoArgs() {
        val fake_command = mock<Executable>()
        parser.addCommand("some_command", { name, args, cmd_env ->
            assertEquals("some_command", name)
            assertEquals(listOf<String>(), args)
            assertSame(env, cmd_env)
            fake_command
        })

        val command = parser.parse(listOf("some_command"))

        assertSame(fake_command, command)
    }

    @Test fun testVarAssignment() {
        val command = parser.parse(listOf("VAR=1234=5 67"))
        assertEquals(EnvvarAssignment::class.java, command.javaClass)
        command.start()
        command.waitForTermination()
        command.stdout.close()
        assertEquals("1234=5 67", env["VAR"])
    }
}

class ExecutableBuilderSplitByPipesTest {
    fun unquotedTokens(data: List<String>) = data.map { x -> Token(x, false) }

    @Test fun testEmpty() {
        assertEquals(
                listOf<List<String>>(),
                ExecutableBuilder.splitByPipes(listOf())
        )
    }

    @Test fun testNoPipes() {
        assertEquals(
                listOf(
                        listOf("hello", "world")
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("hello", "world")))
        )
    }

    @Test fun testSeveralPipes() {
        assertEquals(
                listOf(
                        listOf("hello world"),
                        listOf("second", "pipe"),
                        listOf("third long pipe")
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("hello world", "|", "second", "pipe", "|", "third long pipe")))
        )
    }

    @Test fun testEmptyTokens() {
        assertEquals(
                listOf(
                        listOf("hello", "", "world"),
                        listOf(""),
                        listOf("wow")
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("hello", "", "world", "|", "", "|", "wow")))
        )
    }

    @Test fun testTokenWithPipe() {
        assertEquals(
                listOf(
                        listOf("hello | world")
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("hello | world")))
        )
    }

    @Test fun testQuotedPipes() {
        assertEquals(
                listOf(
                        listOf("hello", "|", "world"),
                        listOf("foo", "|", "bar")
                ),
                ExecutableBuilder.splitByPipes(listOf(
                        Token("hello", false),
                        Token("|", true),
                        Token("world", false),
                        Token("|", false),
                        Token("foo", false),
                        Token("|", true),
                        Token("bar", false)
                ))
        )
    }

    @Test fun testConsecutivePipes() {
        assertEquals(
                listOf(
                        listOf("hello"),
                        listOf(),
                        listOf(),
                        listOf("world")
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("hello", "|", "|", "|", "world")))
        )
    }

    @Test fun testBorderPipes() {
        assertEquals(
                listOf(
                        listOf(),
                        listOf(),
                        listOf("hello", "world"),
                        listOf(),
                        listOf()
                ),
                ExecutableBuilder.splitByPipes(unquotedTokens(listOf("|", "|", "hello", "world", "|", "|")))
        )
    }
}