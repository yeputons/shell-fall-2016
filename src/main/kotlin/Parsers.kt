package net.yeputons.spbau.fall2016

data class QuotedChar(val char: Char, val quotation: Quotation, val position: Int) {
    enum class Quotation(val char: Char?) {
        UNQUOTED(null),
        QUOTER(null),
        QUOTED_DOUBLE('"'),
        QUOTED_SINGLE('\'');

        companion object {
            val quotationByChar = Quotation.values().associateBy(Quotation::char)
        }
    }
}

class LineParser(val environment: Environment) {
    companion object {
        fun isShellIdentifierPart(c: Char) = c.isLetterOrDigit() || c == '_'

        fun processQuotes(str: String): List<QuotedChar> {
            val result = mutableListOf<QuotedChar>()

            var pos = 0
            var quoted = QuotedChar.Quotation.UNQUOTED
            while (pos < str.length) {
                val c = str[pos]
                if (c == '\\' && quoted != QuotedChar.Quotation.QUOTED_SINGLE) {
                    result.add(QuotedChar(c, QuotedChar.Quotation.QUOTER, pos))
                    pos++
                    val realChar = str.getOrNull(pos) ?: throw ParserException("Unexpected eol after backslash", pos)
                    result.add(QuotedChar(realChar, QuotedChar.Quotation.QUOTED_SINGLE, pos))
                    pos++
                    continue
                }

                val quotation = QuotedChar.Quotation.quotationByChar[c]
                if (quotation != null) {
                    when (quoted) {
                        QuotedChar.Quotation.UNQUOTED -> {
                            result.add(QuotedChar(c, QuotedChar.Quotation.QUOTER, pos))
                            quoted = quotation
                        }
                        quotation -> {
                            quoted = QuotedChar.Quotation.UNQUOTED
                            result.add(QuotedChar(c, QuotedChar.Quotation.QUOTER, pos))
                        }
                        else -> result.add(QuotedChar(c, quoted, pos))
                    }
                    pos++
                } else {
                    result.add(QuotedChar(c, quoted, pos))
                    pos++
                }
            }
            if (quoted != QuotedChar.Quotation.UNQUOTED) {
                throw ParserException("Unclosed quote detected", pos)
            }
            return result
        }

        fun tokenize(s: List<QuotedChar>): List<String> {
            val result = mutableListOf<String>()

            fun isSeparator(c: QuotedChar): Boolean = c.quotation == QuotedChar.Quotation.UNQUOTED && c.char.isWhitespace()

            fun isSpecialChar(c: QuotedChar): Boolean = c.quotation == QuotedChar.Quotation.UNQUOTED && c.char == '|'

            fun splitBetween(a: QuotedChar, b: QuotedChar): Boolean =
                    isSeparator(a) || isSpecialChar(a) ||
                            isSeparator(b) || isSpecialChar(b)

            var currentToken: StringBuilder? = null
            var previous: QuotedChar? = null
            for (c in s) {
                if (previous != null && splitBetween(previous, c)) {
                    if (currentToken != null) {
                        result.add(currentToken.toString())
                        currentToken = null
                    }
                }

                if (!isSeparator(c)) {
                    currentToken = currentToken ?: StringBuilder();
                    if (c.quotation != QuotedChar.Quotation.QUOTER) {
                        currentToken.append(c.char)
                    }
                }
                previous = c
            }
            if (currentToken != null) {
                result.add(currentToken.toString())
            }
            return result
        }
    }

    fun substitute(str: List<QuotedChar>): String {
        val result = StringBuilder()
        var pos = 0
        while (pos < str.size) {
            val startPos = pos
            if (str[pos].char == '$' && str[pos].quotation != QuotedChar.Quotation.QUOTED_SINGLE) {
                val varNameBuilder = StringBuilder()
                pos++
                while (pos < str.size && isShellIdentifierPart(str[pos].char)) {
                    varNameBuilder.append(str[pos].char)
                    pos++
                }
                val varName = varNameBuilder.toString()
                val varValue: String = environment[varName] ?: throw ParserException("Non-existent variable '$varName'", startPos)
                result.append(varValue)
            } else {
                result.append(str[pos].char)
                pos++
            }
        }
        return result.toString()
    }

    fun parse(line: String): List<String> = tokenize(processQuotes(substitute(processQuotes(line))))
}

class ParserException(message: String, pos: Int) : Exception(message + " at position " + pos) {
}
