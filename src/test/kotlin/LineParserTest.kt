package net.yeputons.spbau.fall2016

import org.junit.Test
import org.junit.Assert.*

val quotationShortcuts = mapOf(
        AnnotatedChar.Quotation.UNQUOTED to ' ',
        AnnotatedChar.Quotation.QUOTED_DOUBLE to 'd',
        AnnotatedChar.Quotation.QUOTED_SINGLE to 's',
        AnnotatedChar.Quotation.QUOTER to '\''
)
val quotationByShortcut = AnnotatedChar.Quotation.values().associateBy { quotationShortcuts[it] }
fun getCharQuotations(str: List<AnnotatedChar>): String {
    return str.map({ quotationShortcuts[it.quotation] }).joinToString("")
}

fun toQuotedCharList(str: String, quotations: String): List<AnnotatedChar> {
    return str.zip(quotations).map { data ->
        AnnotatedChar(data.first, quotationByShortcut[data.second] as AnnotatedChar.Quotation)
    }
}

class TestProcessQuotes {
    fun checkQuotes(str: String, quoted: String) {
        val result = LineParser.processQuotes(str)
        assertEquals(str.toList(), result.map(AnnotatedChar::char))
        assertEquals(quoted, getCharQuotations(result))
    }

    @Test fun testSimpleString() {
        checkQuotes(
                """hi there""",
                """        """)
    }

    @Test fun testEscaped() {
        checkQuotes(
                """hi\ there""",
                """  's     """)
    }

    @Test fun testQuotes() {
        checkQuotes(
                """hi' ther'e""",
                """  'sssss' """)
        checkQuotes(
                """hi" ther"e""",
                """  'ddddd' """)
    }

    @Test fun testEscapedQuotes() {
        checkQuotes(
                """hi' th\'ere""",
                """  'ssss'   """)
        checkQuotes(
                """hi" th\"ere"!""",
                """  'ddd'sddd' """)
    }

    @Test fun testQuotesInsideQuotes() {
        checkQuotes(
                """hi' t"h'ere""",
                """  'ssss'   """)
        checkQuotes(
                """hi" t'h"ere""",
                """  'dddd'   """)
    }

    @Test fun testMultupleQuotes() {
        checkQuotes(
                """hello "my dear"\ world it"'"s really' 'beautiful""",
                """      'ddddddd''s        'd'        's'         """)
    }

    @Test(expected = ParserException::class) fun testUnclosedSingleQuote() {
        LineParser.processQuotes("hello ' world")
    }

    @Test(expected = ParserException::class) fun testUnclosedDoubleQuote() {
        LineParser.processQuotes("hello \" world")
    }
}

class TestSubstitute {
    val parser: LineParser = LineParser(Environment())

    fun checkSubstitute(expected: String, str: String, quotations: String) {
        assertEquals(expected, parser.substitute(toQuotedCharList(str, quotations)))
    }

    @Test fun testNoSubstitute() {
        checkSubstitute(
                """hello "my great" world""",
                """hello "my great" world""",
                """      'dddddddd'      """
        )
    }

    @Test fun testSimpleSubstitute() {
        parser.environment["VAR"] = "my great"
        checkSubstitute(
                """hello my great world""",
                """hello @VAR world""".replace('@', '$'),
                """                """
        )
    }

    @Test fun testEscapedSubstitute() {
        checkSubstitute(
                """hello @VAR world""".replace('@', '$'),
                """hello @VAR world""".replace('@', '$'),
                """      s         """
        )
    }

    @Test fun testSubstitudeInsideDoubleQuotes() {
        parser.environment["VAR"] = "my great"
        checkSubstitute(
                """hello my great world""",
                """hello @VAR world""".replace('@', '$'),
                """      dddd      """
        )
    }

    @Test fun testSubstitudeInsideSingleQuotes() {
        checkSubstitute(
                """hello @VAR world""".replace('@', '$'),
                """hello @VAR world""".replace('@', '$'),
                """      ssss      """
        )
    }

    @Test fun testConsecutiveSubstitude() {
        parser.environment["VAR1"] = "hello"
        parser.environment["VAR2"] = "world"
        checkSubstitute(
                """hello helloworld world""",
                """hello @VAR1@VAR2 world""".replace('@', '$'),
                """                      """
        )
    }

    @Test(expected = ParserException::class) fun testNonExistentVariable() {
        parser.substitute(toQuotedCharList(
                "HI @THERE".replace('@', '$'),
                "         "
        ))
    }
}

class TestTokenize {
    fun checkTokenize(expected: List<String>, str: String, quotations: String) {
        assertEquals(expected, LineParser.tokenize(toQuotedCharList(str, quotations)))
    }

    @Test fun testSimpleTokenize() {
        checkTokenize(
                listOf("hi"),
                """hi""",
                """  """
        )
        checkTokenize(
                listOf("hello", "world"),
                """hello world""",
                """           """
        )
        checkTokenize(
                listOf("hello", "world"),
                """hello  world""",
                """            """
        )
        checkTokenize(
                listOf("hello", "world"),
                """ hello  world """,
                """              """
        )
    }

    @Test fun testEmpty() {
        checkTokenize(listOf(), "", "")
    }

    @Test fun testEmptyTokens() {
        checkTokenize(
                listOf("hi", "", "my super", " ", "great", "", "world"),
                """   hi   '''' my super    great "" world """,
                """        ''''  sss      s       ''       """
        )
    }

    @Test fun testSpecialChar() {
        checkTokenize(
                listOf("|", "hello", "|", "world", "|", "this", "is", "|", "great", "|", "pipe", "|"),
                """|  hello|world |this is| great | pipe |""",
                """                                       """
        )
    }

    @Test fun testEscapedSpecialChar() {
        checkTokenize(
                listOf("some", "non|", "|pipe test"),
                """some non| |pipe test""",
                """        s s    s    """
        )
    }
}

class TestLineParserParse {
    val parser: LineParser = LineParser(Environment())

    @Test fun testSimpleSplit() {
        assertEquals(listOf("hello", "world"), parser.parse("hello world"))
        assertEquals(listOf("hello", "world"), parser.parse(" hello world"))
        assertEquals(listOf("hello", "world"), parser.parse("  hello  world"))
        assertEquals(listOf("hello", "world"), parser.parse("  hello  world  "))
    }

    @Test fun testEscaping() {
        assertEquals(listOf("this\"is\'some thing", "with", "\$VAR"), parser.parse("this\\\"is\\'some\\ thing with \\\$VAR"))
    }

    @Test(expected = ParserException::class) fun testEolAfterBackslash() {
        parser.parse("malformed\\")
    }

    @Test fun testPipeline() {
        assertEquals(listOf("this", "|", "is", "|", "|", "some", "|", "|", "|", "pipeline"),
                parser.parse("this | is || some | || pipeline"))
    }

    @Test fun testQuotes() {
        assertEquals(listOf("somequoted 'tex|tIs \"hereand", "there"),
                parser.parse("some\"quoted 'tex|t\"I's \"here'and there"))
    }

    @Test fun testEmptyQuotes() {
        assertEquals(listOf("", "hello", "", "world", ""),
                parser.parse("\"\" hello '' world \"\""))
    }

    @Test fun testSubstitutions() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(listOf("Value", "of", "VAR", "is", "Some", "value"),
                parser.parse("Value of VAR is \$VAR"))
        assertEquals(listOf("Value", "of", "VAR", "is", " Some value"),
                parser.parse("Value of VAR is \" \$VAR\""))
        assertEquals(listOf("Value", "of", "OTHER_VAR1", "is", "Other", "\$VAR"),
                parser.parse("Value of OTHER_VAR1 is \$OTHER_VAR1"))
    }

    @Test(expected = ParserException::class) fun testSubstituteNotExistent() {
        parser.parse("Value of VAR is \$VAR")
    }

    @Test fun testSubstitutionsInsideQuotes() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(listOf("Value of \$VAR is not here"),
                parser.parse("'Value of \$VAR is not here'"))
        assertEquals(listOf("Value of \$VAR is Some value, yes"),
                parser.parse("\"Value of \\\$VAR is \$VAR, yes\""))
    }
}
