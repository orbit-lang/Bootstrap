package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.types.components.Context
import org.orbit.types.components.Entity
import org.orbit.types.components.Property
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
        is CallNode ->
            CallUnit(node, depth).generate(mangler)
        is BinaryExpressionNode ->
            BinaryExpressionUnit(node, depth).generate(mangler)
        is UnaryExpressionNode ->
            UnaryExpressionUnit(node, depth).generate(mangler)

        else -> TODO("@ExpressionUnit:16")
    }
}

class UnaryExpressionUnit(override val node: UnaryExpressionNode, override val depth: Int) : CodeUnit<UnaryExpressionNode> {
    override fun generate(mangler: Mangler): String {
        val operand = ExpressionUnit(node.operand, depth).generate(mangler)

        return "(${node.operator}($operand))"
    }
}

class BinaryExpressionUnit(override val node: BinaryExpressionNode, override val depth: Int) : CodeUnit<BinaryExpressionNode> {
    override fun generate(mangler: Mangler): String {
        val left = ExpressionUnit(node.left, depth).generate(mangler)
        val right = ExpressionUnit(node.right, depth).generate(mangler)

        return "(($left) ${node.operator} ($right))"
    }
}

class IdentifierUnit(override val node: IdentifierNode, override val depth: Int) : CodeUnit<IdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return node.identifier
    }
}

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : CodeUnit<ConstructorNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    private val invocation: Invocation by inject()

    override fun generate(mangler: Mangler): String {
        val targetType = node.typeExpressionNode.getType()

        if (targetType !is Entity) {
            throw invocation.make<CodeWriter>("Only types may be initialised via a constructor call. Found $targetType", node.typeExpressionNode)
        }

        val parameters = node.parameterNodes
            .map(partial(::ExpressionUnit, depth))
            .map(partial(ExpressionUnit::generate, mangler))

        val parameterNames = targetType.properties.map(Property::name)

        val properties = parameterNames.zip(parameters).joinToString(", ") {
            "${it.first}: ${it.second}"
        }

        val targetTypeName = (OrbitMangler + mangler).invoke(targetType.name)

        return """ 
            |$targetTypeName($properties)
        """.trimMargin()
    }
}

class RValueUnit(override val node: RValueNode, override val depth: Int) : CodeUnit<RValueNode> {
    override fun generate(mangler: Mangler): String = ExpressionUnit(node.expressionNode, depth).generate(mangler)
}