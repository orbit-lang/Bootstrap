package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.core.nodes.LiteralNode
import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.core.plus
import org.orbit.types.IntrinsicTypes
import java.math.BigInteger

interface LiteralUnit<T> : CodeUnit<LiteralNode<T>>

class IntLiteralUnit(override val node: LiteralNode<Pair<Int, BigInteger>>, override val depth: Int) : LiteralUnit<Pair<Int, BigInteger>> {
    override fun generate(mangler: Mangler): String {
        // TODO - Int width
        return "${node.value.second}"
    }
}

class SymbolLiteralUnit(override val node: LiteralNode<Pair<Int, String>>, override val depth: Int) : LiteralUnit<Pair<Int, String>> {
    override fun generate(mangler: Mangler): String {
        val symbolType = (OrbitMangler + mangler)(IntrinsicTypes.Symbol.type.name)
        return "${symbolType}(value: \"${node.value.second}\")"
    }
}

object LiteralUnitUtil {
    fun <T> generateLiteralUnit(node: LiteralNode<T>, depth: Int) : LiteralUnit<T> = when (node) {
        is IntLiteralNode -> IntLiteralUnit(node as LiteralNode<Pair<Int, BigInteger>>, depth) as LiteralUnit<T>
        is SymbolLiteralNode -> SymbolLiteralUnit(node, depth) as LiteralUnit<T>
        else -> TODO("@LiteralUnitUtil:19")
    }
}