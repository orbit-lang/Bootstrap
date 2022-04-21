package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractReturnStatementUnit
import org.orbit.core.*
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.core.StringKey

class ReturnStatementUnit(override val node: ReturnStatementNode, override val depth: Int, private val resultIsDeferred: Boolean, override val deferFunctions: List<StringKey>) : AbstractReturnStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val retType = node.valueNode.expressionNode.getType()
        val retTypeName = OrbitMangler.plus(mangler)(retType.name)
        val retVal = codeGenFactory.getRValueUnit(node.valueNode, depth)
            .generate(mangler)

        return if (resultIsDeferred) {
            """
            |$retTypeName __ret_val = $retVal;
            |${deferFunctions.joinToString("\n") { "${it.value}(__ret_val);" }}
            |return __ret_val;
            """.trimMargin()
        } else {
            """
            |return $retVal;
            """.trimMargin()
        }.prependIndent(indent())
    }
}