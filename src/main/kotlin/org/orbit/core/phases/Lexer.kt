package org.orbit.core.phases

import org.orbit.core.Phase

object Lexer : Phase<String, Array<String>> {
    override fun execute(input: String): Array<String> {
        return emptyArray()
    }
}
