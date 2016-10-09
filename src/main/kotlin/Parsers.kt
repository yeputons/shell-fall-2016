package net.yeputons.spbau.fall2016

class LineParser(val environment: Environment) {
    fun tokenizeAndSubstitute(line : String) : List<String> {
        val tokens = mutableListOf<String>()

        var currentToken : String? = null
        var insideQuotes : Char? = null
        var pos = 0
        while (pos < line.length) {
            val errorPos = pos
            val c = line[pos]
            if (c == '\\') {
                pos++
                if (pos >= line.length) {
                    throw ParserException("Unexpected eol after backslash", errorPos)
                }
                currentToken = currentToken ?: ""
                currentToken += line[pos]
                pos++
            } else if (c == '$' && insideQuotes != '\'') {
                var varName = ""
                pos++
                while (pos < line.length && isShellIdentifierPart(line[pos])) {
                    varName += line[pos]
                    pos++
                }
                if (varName.isEmpty()) {
                    throw ParserException("Empty variable name", errorPos)
                }

                val varValue: String = environment[varName] ?: throw ParserException("Non-existent variable " + varName, errorPos)
                currentToken = currentToken ?: ""
                currentToken += varValue
            } else if (insideQuotes != null) {
                if (c == insideQuotes) {
                    insideQuotes = null
                    pos++
                    continue
                } else {
                    currentToken = currentToken ?: ""
                    currentToken += line[pos]
                    pos++
                }
            } else if (c == '"' || c == '\'') {
                insideQuotes = c
                currentToken = currentToken ?: ""
                pos++
            } else if (c.isWhitespace()) {
                if (currentToken != null) {
                    tokens.add(currentToken)
                    currentToken = null
                }
                pos++
            } else if (c == '|') {
                if (currentToken != null) {
                    tokens.add(currentToken)
                    currentToken = null
                }
                tokens.add("" + c)
                pos++
            } else {
                currentToken = currentToken ?: ""
                currentToken += line[pos]
                pos++
            }
        }
        if (insideQuotes != null) {
            throw ParserException("Quote $insideQuotes is not closed", pos)
        }
        if (currentToken != null) {
            tokens += currentToken
        }
        return tokens
    }

    companion object {
        fun isShellIdentifierPart(c : Char): Boolean = c.isLetterOrDigit() || c == '_'
    }
}

class ParserException(message : String, pos : Int) : Exception(message + " at position " + pos) {
}
