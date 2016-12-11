package net.yeputons.spbau.fall2016

data class AnnotatedChar(val char: Char, val quotation: Quotation) {
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

        fun processQuotes(str: String): List<AnnotatedChar> {
            val result = mutableListOf<AnnotatedChar>()

            var pos = 0
            var quoted = AnnotatedChar.Quotation.UNQUOTED
            while (pos < str.length) {
                val c = str[pos]
                if (c == '\\' && quoted != AnnotatedChar.Quotation.QUOTED_SINGLE) {
                    result.add(AnnotatedChar(c, AnnotatedChar.Quotation.QUOTER))
                    pos++
                    val realChar = str.getOrNull(pos) ?: throw ParserException("Unexpected eol after backslash", pos)
                    result.add(AnnotatedChar(realChar, AnnotatedChar.Quotation.QUOTED_SINGLE))
                    pos++
                    continue
                }

                val quotation = AnnotatedChar.Quotation.quotationByChar[c]
                if (quotation != null) {
                    when (quoted) {
                        AnnotatedChar.Quotation.UNQUOTED -> {
                            result.add(AnnotatedChar(c, AnnotatedChar.Quotation.QUOTER))
                            quoted = quotation
                        }
                        quotation -> {
                            quoted = AnnotatedChar.Quotation.UNQUOTED
                            result.add(AnnotatedChar(c, AnnotatedChar.Quotation.QUOTER))
                        }
                        else -> result.add(AnnotatedChar(c, quoted))
                    }
                    pos++
                } else {
                    result.add(AnnotatedChar(c, quoted))
                    pos++
                }
            }
            if (quoted != AnnotatedChar.Quotation.UNQUOTED) {
                throw ParserException("Unclosed quote detected", pos)
            }
            return result
        }

        fun tokenize(s: List<AnnotatedChar>): List<String> {
            val result = mutableListOf<String>()

            fun isSeparator(c: AnnotatedChar): Boolean = c.quotation == AnnotatedChar.Quotation.UNQUOTED && c.char.isWhitespace()

            fun isSpecialChar(c: AnnotatedChar): Boolean = c.quotation == AnnotatedChar.Quotation.UNQUOTED && c.char == '|'

            fun splitBetween(a: AnnotatedChar, b: AnnotatedChar): Boolean =
                    isSeparator(a) || isSpecialChar(a) ||
                            isSeparator(b) || isSpecialChar(b)

            var currentToken: StringBuilder? = null
            var previous: AnnotatedChar? = null
            for (c in s) {
                if (previous != null && splitBetween(previous, c)) {
                    if (currentToken != null) {
                        result.add(currentToken.toString())
                        currentToken = null
                    }
                }

                if (!isSeparator(c)) {
                    currentToken = currentToken ?: StringBuilder();
                    if (c.quotation != AnnotatedChar.Quotation.QUOTER) {
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

    fun substitute(str: List<AnnotatedChar>): String {
        val result = StringBuilder()
        var pos = 0
        while (pos < str.size) {
            val startPos = pos
            if (str[pos].char == '$' && str[pos].quotation != AnnotatedChar.Quotation.QUOTED_SINGLE) {
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
