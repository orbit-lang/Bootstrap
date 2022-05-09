package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.nodes.Annotations

interface AbstractTypeExpressionUnit : CodeUnit<TypeExpressionNode>

class TypeExpressionUnit(override val node: TypeExpressionNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : AbstractTypeExpressionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler) : String {
        return ""
//        var path = node.getPath()
//        val type = context.getTypeByPath(path)
//        path = type.getFullyQualifiedPath()
//        val typeName = mangler.mangle(path)
//
//        return when (node) {
//            is TypeIdentifierNode -> {
//                if (type is TypeConstructor) {
//                    // Implicit `T<AnyType>`
//                    val nTypeParameterNode = TypeIdentifierNode(node.firstToken, node.lastToken, "AnyType")
//                    val nNode = MetaTypeNode(node.firstToken, node.lastToken, node, listOf(nTypeParameterNode))
//
//                    nNode.annotate(node.getPath(), Annotations.Path)
//                    nTypeParameterNode.annotate(IntrinsicTypes.AnyType.path, Annotations.Path)
//
//                    codeGenFactory.getMetaTypeUnit(nNode, depth, inFuncNamePosition)
//                        .generate(mangler)
//                }
//
//                typeName
//            }
//            is MetaTypeNode -> codeGenFactory.getMetaTypeUnit(node, depth, inFuncNamePosition).generate(mangler)
//            else -> TODO("???")
//        }
    }
}