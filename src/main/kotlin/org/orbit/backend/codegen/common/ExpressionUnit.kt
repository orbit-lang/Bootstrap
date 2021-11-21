package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.RValueNode

interface AbstractRValueUnit : CodeUnit<RValueNode>

class RValueUnit(override val node: RValueNode, override val depth: Int) : AbstractRValueUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String
        = codeGenFactory.getExpressionUnit(node.expressionNode, depth)
            .generate(mangler)
}