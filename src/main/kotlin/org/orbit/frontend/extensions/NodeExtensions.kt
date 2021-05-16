package org.orbit.frontend.extensions

import org.orbit.core.nodes.Node
import org.orbit.frontend.rules.ParseRule

fun <N: Node> N.toParseResultSuccess() : ParseRule.Result.Success<N> {
    return ParseRule.Result.Success(this)
}

operator fun <N: Node> N.unaryPlus() : ParseRule.Result.Success<N> {
    return this.toParseResultSuccess()
}