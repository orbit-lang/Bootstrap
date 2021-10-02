package org.orbit.types.components

import org.orbit.core.Path

interface IntrinsicOperators {
    val position: OperatorPosition
    val returnType: TypeProtocol

    fun getType() : Operator
    fun getPath() : Path
}