package net.yeputons.spbau.fall2016

import org.junit.Test
import org.junit.Assert.*

class TestTokenizeAndSubstitute {
    val parser: LineParser = LineParser(Environment())

    @Test fun testSimpleSplit() {
        assertEquals(listOf("hello", "world"), parser.tokenizeAndSubstitute("hello world"))
        assertEquals(listOf("hello", "world"), parser.tokenizeAndSubstitute(" hello world"))
        assertEquals(listOf("hello", "world"), parser.tokenizeAndSubstitute("  hello  world"))
        assertEquals(listOf("hello", "world"), parser.tokenizeAndSubstitute("  hello  world  "))
    }

    @Test fun testEscaping() {
        assertEquals(listOf("this\"is\'some thing", "with", "\$VAR"), parser.tokenizeAndSubstitute("this\\\"is\\'some\\ thing with \\\$VAR"))
    }

    @Test(expected = ParserException::class) fun testEolAfterBackslash() {
        parser.tokenizeAndSubstitute("malformed\\")
    }

    @Test fun testPipeline() {
        assertEquals(listOf("this", "|", "is", "|", "|", "some", "|", "|", "|", "pipeline"),
                parser.tokenizeAndSubstitute("this | is || some | || pipeline"))
    }

    @Test fun testQuotes() {
        assertEquals(listOf("somequoted 'tex|tIs \"hereand", "there"),
                parser.tokenizeAndSubstitute("some\"quoted 'tex|t\"I's \"here'and there"))
    }

    @Test(expected = ParserException::class) fun testUnclosedQuotes1() {
        parser.tokenizeAndSubstitute("Hello \"world ")
    }

    @Test(expected = ParserException::class) fun testUnclosedQuotes2() {
        parser.tokenizeAndSubstitute("Hello 'world ")
    }

    @Test fun testEmptyQuotes() {
        assertEquals(listOf("", "hello", "", "world", ""),
                parser.tokenizeAndSubstitute("\"\" hello '' world \"\""))
    }

    @Test fun testSubstitutions() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(listOf("Value", "of", "VAR", "is", "Some",  "value"),
                parser.tokenizeAndSubstitute("Value of VAR is \$VAR"))
        assertEquals(listOf("Value", "of", "VAR", "is", " Some value"),
                parser.tokenizeAndSubstitute("Value of VAR is \" \$VAR\""))
        assertEquals(listOf("Value", "of", "OTHER_VAR1", "is", "Other", "\$VAR"),
                parser.tokenizeAndSubstitute("Value of OTHER_VAR1 is \$OTHER_VAR1"))
    }

    @Test(expected = ParserException::class) fun testSubstituteNotExistent() {
        parser.tokenizeAndSubstitute("Value of VAR is \$VAR")
    }

    @Test fun testSubstitutionsInsideQuotes() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(listOf("Value of \$VAR is not here"),
                parser.tokenizeAndSubstitute("'Value of \$VAR is not here'"))
        assertEquals(listOf("Value of \$VAR is Some value, yes"),
                parser.tokenizeAndSubstitute("\"Value of \\\$VAR is \$VAR, yes\""))
    }
}
