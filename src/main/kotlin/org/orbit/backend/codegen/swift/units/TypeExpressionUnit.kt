package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.components.Context

class TypeExpressionUnit(override val node: TypeExpressionNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : CodeUnit<TypeExpressionNode> {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    override fun generate(mangler: Mangler) : String {
        val path = node.getPath()
        val type = context.getTypeByPath(path)
        val typeName = (OrbitMangler + mangler).invoke(type.name)

        return when (node) {
            is TypeIdentifierNode -> typeName
            is MetaTypeNode -> MetaTypeUnit(node, depth, inFuncNamePosition).generate(mangler)
            else -> TODO("???")
        }
    }
}