package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.util.partial

interface AbstractMetaTypeUnit : CodeUnit<MetaTypeNode>

class MetaTypeUnit(override val node: MetaTypeNode, override val depth: Int, private val inFuncNamePosition: Boolean = false) : AbstractMetaTypeUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler) : String {
        val type = node.getType()
        return type.getFullyQualifiedPath().toString(mangler)

//        val path = node.getPath()
//
//        val separator = when (inFuncNamePosition) {
//            true -> "_"
//            else -> ", "
//        }
//
//        val typeParameters = node.typeParameters
//            .map(partial(codeGenFactory::getTypeExpressionUnit, depth))
//            .joinToString(separator, transform = partial(AbstractTypeExpressionUnit::generate, mangler))
//
//        val typeName = path.toString(mangler)
//
//        return "${typeName}_$typeParameters"
    }
}