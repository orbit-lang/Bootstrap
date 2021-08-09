@file:Suppress("UNCHECKED_CAST")

package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
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
        return "${symbolType}(value: \"${node.value.second}\", len: ${node.value.second.length})"
    }
}

class TypeLiteralUnit(override val node: LiteralNode<String>, override val depth: Int) : LiteralUnit<String> {
    override fun generate(mangler: Mangler): String {
        return "${node.getPath().toString(mangler)}.self"
    }
}

//class MetaTypeUnit(override val node: MetaTypeNode, override val depth: Int) : LiteralUnit<String>, KoinComponent {
//    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)
//
//    override fun generate(mangler: Mangler): String {
//        val type = node.getType()
//        val typeName = (OrbitMangler + mangler).invoke(type.name)
//
//        return "${typeName}.self"
//    }
//}

object LiteralUnitUtil {
    fun <T> generateLiteralUnit(node: LiteralNode<T>, depth: Int) : LiteralUnit<T> = when (node) {
        is IntLiteralNode -> IntLiteralUnit(node as LiteralNode<Pair<Int, BigInteger>>, depth) as LiteralUnit<T>
        is SymbolLiteralNode -> SymbolLiteralUnit(node, depth) as LiteralUnit<T>
        is TypeIdentifierNode -> TypeLiteralUnit(node, depth) as LiteralUnit<T>
        //is MetaTypeNode -> MetaTypeUnit(node, depth) as LiteralUnit<T>
        else -> TODO("@LiteralUnitUtil:19")
    }
}