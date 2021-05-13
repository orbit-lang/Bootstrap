package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.CodeWriter
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.types.Context
import org.orbit.types.Entity
import org.orbit.types.Property
import org.orbit.util.Invocation
import org.orbit.util.partial

class ExpressionUnit(override val node: ExpressionNode, override val depth: Int) : CodeUnit<ExpressionNode> {
    override fun generate(mangler: Mangler): String = when (node) {
        is LiteralNode<*> ->
            LiteralUnitUtil.generateLiteralUnit(node, depth).generate(mangler)
        is RValueNode ->
            RValueUnit(node, depth).generate(mangler)
        is ConstructorNode ->
            ConstructorUnit(node, depth).generate(mangler)
        is IdentifierNode ->
            IdentifierUnit(node, depth).generate(mangler)

        else -> TODO("@ExpressionUnit:16")
    }
}

class IdentifierUnit(override val node: IdentifierNode, override val depth: Int) : CodeUnit<IdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return node.identifier
    }
}

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : CodeUnit<ConstructorNode>, KoinComponent {
    private val context: Context by injectResult<Context>(CompilationSchemeEntry.typeChecker)
    private val invocation: Invocation by inject()

    override fun generate(mangler: Mangler): String {
        val targetType = context.getType(node.typeIdentifierNode.getPath())

        if (targetType !is Entity) {
            throw invocation.make<CodeWriter>("Only types may be initialised via a constructor call. Found $targetType", node.typeIdentifierNode)
        }

        val parameters = node.parameterNodes
            .map(partial(::ExpressionUnit, depth))
            .map(partial(ExpressionUnit::generate, mangler))

        val parameterNames = targetType.properties.map(Property::name)

        val properties = parameterNames.zip(parameters).map {
            "${it.first}: ${it.second}"
        }.joinToString(", ")

        val targetTypeName = node.typeIdentifierNode.getPath().toString(mangler)

        return """ 
            |$targetTypeName($properties)
        """.trimMargin().prependIndent(indent())
    }
}

class RValueUnit(override val node: RValueNode, override val depth: Int) : CodeUnit<RValueNode> {
    override fun generate(mangler: Mangler): String = ExpressionUnit(node.expressionNode, depth).generate(mangler)
}

class ReturnStatementUnit(override val node: ReturnStatementNode, override val depth: Int) : CodeUnit<ReturnStatementNode> {
    override fun generate(mangler: Mangler): String = """
        |return ${RValueUnit(node.valueNode, depth).generate(mangler)}
    """.trimMargin().prependIndent(indent())
}