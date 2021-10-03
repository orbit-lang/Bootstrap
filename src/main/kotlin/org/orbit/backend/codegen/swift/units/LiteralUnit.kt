@file:Suppress("UNCHECKED_CAST")

package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.common.AbstractLiteralUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import java.math.BigInteger

class IntLiteralUnit(override val node: LiteralNode<Pair<Int, BigInteger>>, override val depth: Int) : AbstractLiteralUnit<Pair<Int, BigInteger>> {
    override fun generate(mangler: Mangler): String {
        // TODO - Int width
        return "${node.value.second}"
    }
}

class SymbolLiteralUnit(override val node: LiteralNode<Pair<Int, String>>, override val depth: Int) : AbstractLiteralUnit<Pair<Int, String>> {
    override fun generate(mangler: Mangler): String {
        val symbolType = (OrbitMangler + mangler)(IntrinsicTypes.Symbol.type.name)
        return "${symbolType}(value: \"${node.value.second}\")"
    }
}

class TypeLiteralUnit(override val node: LiteralNode<String>, override val depth: Int) : AbstractLiteralUnit<String>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    override fun generate(mangler: Mangler): String {
        val type = context.getTypeByPath(node.getPath())

        return "Orb_Meta_Entities_Type(name: \"${type.name}\")"
    }
}