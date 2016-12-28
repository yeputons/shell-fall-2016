package net.yeputons.spbau.fall2016.parsing

import net.yeputons.spbau.fall2016.Environment
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
        AnnotatedChar(data.first, quotationByShortcut[data.second]!!)
    }
}

class ProcessQuotesTest {
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

    @Test(expected = LineParserException::class) fun testUnclosedSingleQuote() {
        LineParser.processQuotes("hello ' world")
    }

    @Test(expected = LineParserException::class) fun testUnclosedDoubleQuote() {
        LineParser.processQuotes("""hello " world""")
    }
}

class SubstituteTest {
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

    @Test(expected = LineParserException::class) fun testNonExistentVariable() {
        parser.substitute(toQuotedCharList(
                "HI @THERE".replace('@', '$'),
                "         "
        ))
    }
}

class TokenizeTest {
    fun checkTokenize(expected: List<Token>, str: String, quotations: String) {
        assertEquals(expected, LineParser.tokenize(toQuotedCharList(str, quotations)))
    }

    fun checkTokenizeNoQuotes(expected: List<String>, str: String, quotations: String) {
        checkTokenize(expected.map({ data -> Token(data, startQuoted = false) }), str, quotations)
    }

    @Test fun testSimpleTokenize() {
        checkTokenizeNoQuotes(
                listOf("hi"),
                """hi""",
                """  """
        )
        checkTokenizeNoQuotes(
                listOf("hello", "world"),
                """hello world""",
                """           """
        )
        checkTokenizeNoQuotes(
                listOf("hello", "world"),
                """hello  world""",
                """            """
        )
        checkTokenizeNoQuotes(
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
                listOf(Token("hi", false),
                        Token("", true),
                        Token("my super", false),
                        Token(" ", true),
                        Token("great", false),
                        Token("", true),
                        Token("world", false)),
                """   hi   '''' my super    great "" world """,
                """        ''''  sss      s       ''       """
        )
    }

    @Test fun testSpecialChar() {
        checkTokenizeNoQuotes(
                listOf("|", "hello", "|", "world", "|", "this", "is", "|", "great", "|", "pipe", "|"),
                """|  hello|world |this is| great | pipe |""",
                """                                       """
        )
    }

    @Test fun testEscapedSpecialChar() {
        checkTokenize(
                listOf(
                        Token("some", false),
                        Token("non|", false),
                        Token("|pipe test", true),
                        Token("|", true),
                        Token("|", false),
                        Token("hi", false)
                ),
                """some non| |pipe test | | hi""",
                """        s s    s     s     """
        )
    }
}

class LineParserParseTest {
    val parser: LineParser = LineParser(Environment())

    fun unquotedTokens(data: List<String>) = data.map { x -> Token(x, false) }

    @Test fun testSimpleSplit() {
        assertEquals(unquotedTokens(listOf("hello", "world")), parser.parse("hello world"))
        assertEquals(unquotedTokens(listOf("hello", "world")), parser.parse(" hello world"))
        assertEquals(unquotedTokens(listOf("hello", "world")), parser.parse("  hello  world"))
        assertEquals(unquotedTokens(listOf("hello", "world")), parser.parse("  hello  world  "))
    }

    @Test fun testEscaping() {
        assertEquals(
                listOf(
                        Token("""this"is'some thing""", false),
                        Token("with", false),
                        Token("\$VAR", true)
                ),
                parser.parse("this\\\"is\\'some\\ thing with \\\$VAR")
        )
    }

    @Test(expected = LineParserException::class) fun testEolAfterBackslash() {
        parser.parse("malformed\\")
    }

    @Test fun testPipeline() {
        assertEquals(unquotedTokens(listOf("this", "|", "is", "|", "|", "some", "|", "|", "|", "pipeline")),
                parser.parse("this | is || some | || pipeline"))
    }

    @Test fun testQuotes() {
        assertEquals(unquotedTokens(listOf("somequoted 'tex|tIs \"hereand", "there")),
                parser.parse("""some"quoted 'tex|t"I's "here'and there"""))
    }

    @Test fun testQuotedPipeline() {
        assertEquals(
                listOf(
                        Token("hello", false),
                        Token("|", false),
                        Token("|", true),
                        Token("|", true),
                        Token("|", false),
                        Token("world", false)
                ),
                parser.parse("""hello|\| \||world""")
        )
    }

    @Test fun testEmptyQuotes() {
        assertEquals(
                listOf(
                        Token("", true),
                        Token("hello", false),
                        Token("", true),
                        Token("world", false),
                        Token("", true)
                ),
                parser.parse("\"\" hello '' world \"\"")
        )
    }

    @Test fun testSubstitutions() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(unquotedTokens(listOf("Value", "of", "VAR", "is", "Some", "value")),
                parser.parse("Value of VAR is \$VAR"))
        assertEquals(
                listOf(
                        Token("Value", false),
                        Token("of", false),
                        Token("VAR", false),
                        Token("is", false),
                        Token(" Some value", true)
                ),
                parser.parse("Value of VAR is \" \$VAR\"")
        )
        assertEquals(unquotedTokens(listOf("Value", "of", "OTHER_VAR1", "is", "Other", "\$VAR")),
                parser.parse("Value of OTHER_VAR1 is \$OTHER_VAR1"))
    }

    @Test(expected = LineParserException::class) fun testSubstituteNotExistent() {
        parser.parse("Value of VAR is \$VAR")
    }

    @Test fun testSubstitutionsInsideQuotes() {
        parser.environment["VAR"] = "Some value"
        parser.environment["OTHER_VAR1"] = "Other \$VAR"
        assertEquals(unquotedTokens(listOf("aValue of \$VAR is not here")),
                parser.parse("a'Value of \$VAR is not here'"))
        assertEquals(unquotedTokens(listOf("aValue of \$VAR is Some value, yes")),
                parser.parse("a\"Value of \\\$VAR is \$VAR, yes\""))
    }
}
