package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.c.units.*
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.*
import org.orbit.util.partial

interface AbstractBlockUnit : CodeUnit<BlockNode>

class BlockUnit(override val node: BlockNode, override val depth: Int, private val stripBraces: Boolean = false) : AbstractBlockUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val units: List<CodeUnit<*>> = node.body.map {
            when (it) {
                is ReturnStatementNode ->
                    codeGenFactory.getReturnStatementUnit(it, depth, node.search(DeferNode::class.java).isNotEmpty())

                is AssignmentStatementNode ->
                    codeGenFactory.getAssignmentStatementUnit(it, depth)

                is PrintNode -> codeGenFactory.getPrintStatementUnit(it, depth)
                is DeferNode -> codeGenFactory.getDeferStatementUnit(it, depth)

                else ->
                    TODO("Generate code for statement in block: $it")
            }
        }

        // Blocks that defer without returning (i.e implied Unit return type) need to call their defer block here
        val shouldOutputDeferCall = node.containsDefer && !node.containsReturn
        val deferCall = codeGenFactory.getDeferCallUnit(node, depth)
            .generate(mangler)

        val body = units.joinToString("\n|", transform = partial(CodeUnit<*>::generate, mangler))
        return when (stripBraces) {
            true -> body
            else -> """
            |{
            |$body
            |$deferCall
            |}
            """.trimMargin()
        }
    }
}