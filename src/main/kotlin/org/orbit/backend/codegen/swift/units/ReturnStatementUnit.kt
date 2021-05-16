package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.CodeWriter
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.graph.Annotations
import org.orbit.graph.getAnnotation
import org.orbit.types.*
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

class CallUnit(override val node: CallNode, override val depth: Int) : CodeUnit<CallNode> {
    override fun generate(mangler: Mangler): String {
        if (node.isPropertyAccess) {
            val receiver = ExpressionUnit(node.receiverExpression, depth).generate(mangler)
            return "$receiver.${node.messageIdentifier.identifier}"
        }

        val signature = node.getAnnotation<SignatureProtocol<*>>(Annotations.Type)?.value
            ?: TODO("@CallUnit:64")

        val sig = signature.toString(mangler)
        val params = (listOf(node.receiverExpression) + node.parameterNodes).zip(signature.parameters).joinToString(", ") {
            val expr = ExpressionUnit(it.first, depth).generate(mangler)

            "${it.second.name}: $expr"
        }

        return "$sig($params)"
    }
}

class IdentifierUnit(override val node: IdentifierNode, override val depth: Int) : CodeUnit<IdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return node.identifier
    }
}

class ConstructorUnit(override val node: ConstructorNode, override val depth: Int) : CodeUnit<ConstructorNode>, KoinComponent {
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)
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
        """.trimMargin()
    }
}

class RValueUnit(override val node: RValueNode, override val depth: Int) : CodeUnit<RValueNode> {
    override fun generate(mangler: Mangler): String = ExpressionUnit(node.expressionNode, depth).generate(mangler)
}