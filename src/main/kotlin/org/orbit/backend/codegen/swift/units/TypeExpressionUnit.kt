package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeConstructor

class TypeExpressionUnit(override val node: TypeExpressionNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : CodeUnit<TypeExpressionNode> {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    override fun generate(mangler: Mangler) : String {
        val path = node.getPath()
        val type = context.getTypeByPath(path)
        val typeName = (OrbitMangler + mangler).invoke(type.name)

        return when (node) {
            is TypeIdentifierNode -> {
                if (type is TypeConstructor) {
                    // Implicit `T<AnyType>`
                    val nTypeParameterNode = TypeIdentifierNode(node.firstToken, node.lastToken, "AnyType")
                    val nNode = MetaTypeNode(node.firstToken, node.lastToken, node, listOf(nTypeParameterNode))

                    nNode.annotate(node.getPath(), Annotations.Path)
                    nTypeParameterNode.annotate(IntrinsicTypes.AnyType.path, Annotations.Path)

                    MetaTypeUnit(nNode, depth, inFuncNamePosition)
                        .generate(mangler)
                }

                typeName
            }
            is MetaTypeNode -> MetaTypeUnit(node, depth, inFuncNamePosition).generate(mangler)
            else -> TODO("???")
        }
    }
}