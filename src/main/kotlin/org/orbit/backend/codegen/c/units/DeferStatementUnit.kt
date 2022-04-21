package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractDeferStatementUnit
import org.orbit.core.*
import org.orbit.core.nodes.DeferNode

class DeferFunctionUnit(override val node: DeferNode, override val depth: Int, private val index: Int) : CodeUnit<DeferNode>, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val funcName = "__defer$index"
        val param = when (node.returnValueIdentifier) {
            null -> ""
            else -> {
                val paramType = (OrbitMangler + mangler)(node.getType().name)
                val paramName = node.returnValueIdentifier!!.identifier

                "$paramType $paramName"
            }
        }

        val body = codeGenFactory.getBlockUnit(node.blockNode, depth + 1, true, false)
            .generate(mangler)

        return ""
//        node.annotate(StringKey(funcName), key = Annotations.DeferFunction, true)
//        node.blockNode.annotate(StringKey(funcName), key = Annotations.DeferFunction, true)
//
//        return """
//            |void $funcName($param) {
//            |$body
//            |}
//        """.trimMargin().prependIndent(indent())
    }
}

class DeferStatementUnit(override val node: DeferNode, override val depth: Int) : AbstractDeferStatementUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

    // TODO
    override fun generate(mangler: Mangler): String {
//        val deferFunc = node.getAnnotation<StringKey>(Annotations.DeferFunction)
//            ?.value
//            ?: return ""

        return ""
    }
}