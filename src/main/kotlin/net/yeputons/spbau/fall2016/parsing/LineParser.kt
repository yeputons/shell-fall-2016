package net.yeputons.spbau.fall2016.parsing

import net.yeputons.spbau.fall2016.Environment

/**
 * This class represents an annotated char. Each character in a string can be
 * either unquoted, quoted by some quotation mark or be a quotation mark itself
 * (meaning that it should no go past tokenizer). Quotation status impacts
 * further tokenization and variable substitution.
 */
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

/**
 * Represents a parsed token: string plus flag whether beginning of the token was quoted
 * or escaped. It will be important when we split line by pipeline symbols - we should
 * ignore those which was escaped.
 */
data class Token(val data: String, val startIsQuoted: Boolean)

class LineParser(val environment: Environment) {
    companion object {
        fun isShellIdentifierPart(c: Char) = c.isLetterOrDigit() || c == '_'

        /**
         * Processes quotes and \ and returns the same string annotated.
         */
        fun processQuotes(str: String): List<AnnotatedChar> {
            val result = mutableListOf<AnnotatedChar>()

            var pos = 0
            var quoted = AnnotatedChar.Quotation.UNQUOTED
            while (pos < str.length) {
                val c = str[pos]
                if (c == '\\' && quoted != AnnotatedChar.Quotation.QUOTED_SINGLE) {
                    result.add(AnnotatedChar(c, AnnotatedChar.Quotation.QUOTER))
                    pos++
                    val realChar = str.getOrNull(pos) ?: throw LineParserException("Unexpected eol after backslash", pos)
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
                throw LineParserException("Unclosed quote detected", pos)
            }
            return result
        }

        /**
         * Separates annotated string into tokens.
         * Tokens are separated by non-quoted whitespaces.
         * Special characters ('|' only so far) are extracted in their own token.
         */
        fun tokenize(s: List<AnnotatedChar>): List<Token> {
            val result = mutableListOf<Token>()

            fun isSeparator(c: AnnotatedChar): Boolean = c.quotation == AnnotatedChar.Quotation.UNQUOTED && c.char.isWhitespace()

            fun isSpecialChar(c: AnnotatedChar): Boolean = c.quotation == AnnotatedChar.Quotation.UNQUOTED && c.char == '|'

            fun splitBetween(a: AnnotatedChar, b: AnnotatedChar): Boolean =
                    isSeparator(a) || isSpecialChar(a) ||
                            isSeparator(b) || isSpecialChar(b)

            var currentToken: Pair<StringBuilder, Boolean>? = null
            var previous: AnnotatedChar? = null
            for (c in s) {
                if (previous != null && splitBetween(previous, c)) {
                    if (currentToken != null) {
                        result.add(Token(currentToken.first.toString(), currentToken.second))
                        currentToken = null
                    }
                }

                if (!isSeparator(c)) {
                    if (currentToken == null) {
                        currentToken = Pair(StringBuilder(), c.quotation != AnnotatedChar.Quotation.UNQUOTED)
                    }
                    if (c.quotation != AnnotatedChar.Quotation.QUOTER) {
                        currentToken.first.append(c.char)
                    }
                }
                previous = c
            }
            if (currentToken != null) {
                result.add(Token(currentToken.first.toString(), currentToken.second))
            }
            return result
        }
    }

    /**
     * Looks for non-quoted sequences in the form of $ABC and replaces
     * them with corresponding env variable values. Assumes that if the
     * first $ is unquoted (or quoted by " only), then the following
     * characters can compose variable's name (it's the case if you
     * get the argument as <code>processQuotes</code>' result).
     */
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
                val varValue: String = environment[varName] ?: throw LineParserException("Non-existent variable '$varName'", startPos)
                result.append(varValue)
            } else {
                result.append(str[pos].char)
                pos++
            }
        }
        return result.toString()
    }

    /**
     * Parses shell line into sequence of words, performs one round of substitutions.
     */
    fun parse(line: String): List<Token> = tokenize(processQuotes(substitute(processQuotes(line))))
}

class LineParserException(message: String, pos: Int) : Exception(message + " at position " + pos)
