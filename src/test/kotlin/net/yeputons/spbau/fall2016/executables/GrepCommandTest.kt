package net.yeputons.spbau.fall2016.executables

import com.beust.jcommander.ParameterException
import net.yeputons.spbau.fall2016.Environment
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GrepConfigTest {
    @Test(expected = ParameterException::class) fun testNoArgs() {
        GrepConfig.fromArguments(listOf())
    }

    @Test(expected = ParameterException::class) fun testNoPositionals() {
        GrepConfig.fromArguments(listOf("-i", "-A", "10"))
    }

    @Test fun testPatternOnly() {
        val config = GrepConfig.fromArguments(listOf("my pattern"))
        assertEquals("my pattern", config.regex.pattern)
        assertEquals(listOf("-"), config.files)
        assertFalse(config.caseInsensitive)
        assertFalse(config.fullWordsOnly)
        assertNull(config.printAfter)
    }

    @Test fun testOptions() {
        val config = GrepConfig.fromArguments(listOf("my pattern", "-A", "5", "-i", "-w"))
        assertEquals("my pattern", config.regex.pattern)
        assertEquals(listOf("-"), config.files)
        assertTrue(config.caseInsensitive)
        assertTrue(config.fullWordsOnly)
        assertEquals(5, config.printAfter)
    }

    @Test fun testFiles() {
        val config = GrepConfig.fromArguments(listOf("my pattern", "file1", "file2"))
        assertEquals("my pattern", config.regex.pattern)
        assertEquals(listOf("file1", "file2"), config.files)
        assertFalse(config.caseInsensitive)
        assertFalse(config.fullWordsOnly)
        assertNull(config.printAfter)
    }
}

class GrepProcessorTest {
    @Test fun testSimple() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("f[ab]o")))
        assertEquals(listOf<String>(), proc.process("botva", null).toList())
        assertEquals(listOf<String>("bofaoba"), proc.process("bofaoba", null).toList())
        assertEquals(listOf<String>("bofboba"), proc.process("bofboba", null).toList())
        assertEquals(listOf<String>(), proc.process("bofcoba", null).toList())
    }

    @Test fun testWordBorders() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("-w", "f[ab]o")))
        assertEquals(listOf<String>(), proc.process("botva", null).toList())
        assertEquals(listOf<String>(), proc.process("bofaoba", null).toList())
        assertEquals(listOf<String>(), proc.process("bofboba", null).toList())
        assertEquals(listOf<String>(), proc.process("bo faoba", null).toList())
        assertEquals(listOf<String>(), proc.process("bo fboba", null).toList())
        assertEquals(listOf<String>(), proc.process("bofao ba", null).toList())
        assertEquals(listOf<String>(), proc.process("bofbo ba", null).toList())
        assertEquals(listOf<String>("bo fao ba"), proc.process("bo fao ba", null).toList())
        assertEquals(listOf<String>("bo fbo ba"), proc.process("bo fbo ba", null).toList())
        assertEquals(listOf<String>("fao"), proc.process("fao", null).toList())
        assertEquals(listOf<String>("fbo"), proc.process("fbo", null).toList())
        assertEquals(listOf<String>(), proc.process("fco", null).toList())
    }

    @Test fun testCaseInsensitive() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("-i", "f[ab]o")))
        assertEquals(listOf<String>(), proc.process("bOtva", null).toList())
        assertEquals(listOf<String>("boFaoba"), proc.process("boFaoba", null).toList())
        assertEquals(listOf<String>("boFboba"), proc.process("boFboba", null).toList())
        assertEquals(listOf<String>(), proc.process("boFcoba", null).toList())
    }

    @Test fun testFileNames() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("f[ab]o")))
        assertEquals(listOf<String>(), proc.process("botva", "a.txt").toList())
        assertEquals(listOf<String>("a.txt:bofaoba"), proc.process("bofaoba", "a.txt").toList())
        assertEquals(listOf<String>("b.txt:bofboba"), proc.process("bofboba", "b.txt").toList())
        assertEquals(listOf<String>(), proc.process("bofcoba", "b.txt").toList())
    }

    @Test fun testAfter() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("-A", "2", "foo")))
        val source = listOf("boo", "foo", "bar", "baz", "boo", "foo", "bax", "foo2", "bam", "bia", "bum")
        val expected = listOf("f:foo", "f-bar", "f-baz", "--", "f:foo", "f-bax", "f:foo2", "f-bam", "f-bia")
        val result = source.asSequence().flatMap { proc.process(it, "f") }.toList()
        assertEquals(expected, result)
    }

    @Test fun testAfterBreaksBetweenFiles() {
        val proc = GrepProcessor(GrepConfig.fromArguments(listOf("-A", "1", "foo")))
        assertEquals(listOf<String>(), proc.process("botva", "a.txt").toList())
        assertEquals(listOf<String>("a.txt:foo"), proc.process("foo", "a.txt").toList())
        proc.fileEnded()
        assertEquals(listOf<String>(), proc.process("bar", "a.txt").toList())
        assertEquals(listOf<String>("--", "a.txt:foo"), proc.process("foo", "a.txt").toList())
        assertEquals(listOf<String>("a.txt-bar"), proc.process("bar", "a.txt").toList())
    }
}

class GrepCommandTest {
    @Rule @JvmField val tmpFolder = TemporaryFolder()

    @Test fun testNoMatch() {
        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("foo"), Environment()), "bar")
        assertNotEquals(0, exitCode)
        assertEquals("", output)
    }

    @Test fun testMatch() {
        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("foo"), Environment()), "barfoobar")
        assertEquals(0, exitCode)
        assertEquals("barfoobar\n", output)
    }

    @Test fun testNonExistingFile() {
        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("foo", "some-non-existing-file-for-yeputons-shell"), Environment()))
        assertNotEquals(0, exitCode)
        assertNotEquals("", output)
    }

    @Test fun testExistingFile() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\nmeow\nwoof")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("meo", "a.txt"), env))

        assertEquals(0, exitCode)
        assertEquals("meow\n", output)
    }

    @Test fun testExistingFileAbsolutePath() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\nmeow\nwoof")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val path = File(tmpFolder.root, "a.txt").absolutePath
        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("meo", path), env))

        assertEquals(0, exitCode)
        assertEquals("meow\n", output)
    }

    @Test fun testMultipleFiles() {
        tmpFolder.create()
        tmpFolder.newFile("a.txt").writeText("content of a.txt\nmeow\n")
        tmpFolder.newFile("b.txt").writeText("content of b.txt\nno meow\nwoof\nbotva\n")
        val env = Environment()
        env.currentDirectory = tmpFolder.root

        val (exitCode, output) = ExecutableTest.run(GrepCommand(listOf("-A", "1", "meo", "a.txt", "b.txt"), env))

        assertEquals(0, exitCode)
        assertEquals("a.txt:meow\n--\nb.txt:no meow\nb.txt-woof\n", output)
    }
}