package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.graph.components.StringKey

interface AbstractReturnStatementUnit : CodeUnit<ReturnStatementNode> {
    val deferFunctions: List<StringKey>
}