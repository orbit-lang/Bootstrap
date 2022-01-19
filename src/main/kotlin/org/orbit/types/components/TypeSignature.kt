package org.orbit.types.components

import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.phase.AnyEqualityConstraint

data class TypeSignature(
    override val name: String,
    override val receiver: ValuePositionType,
    override val parameters: List<Parameter>,
    override val returnType: ValuePositionType,
    val typeParameters: List<TypeParameter> = emptyList()
) : SignatureProtocol<ValuePositionType> {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = SignatureEquality
    override val isEphemeral: Boolean = false
    override val kind: TypeKind = FunctionKind

    fun toNode(context: Context) : MethodSignatureNode {
        val rec = receiver.name
        val params = parameters.map { it.toNode(context) }
        val ret = returnType.name
        val startToken = Token(TokenTypes.LParen, "(", SourcePosition.unknown)
        val endToken = Token(TokenTypes.RBrace, "}", SourcePosition.unknown)

        val recPath = OrbitMangler.unmangle(rec)
        val recType = context.getTypeByPath(recPath)

        val retPath = OrbitMangler.unmangle(ret)
        val retType = context.getTypeByPath(retPath)

        val recNode = TypeIdentifierNode(startToken, endToken, rec)
        val retNode = TypeIdentifierNode(startToken, endToken, ret)

        recNode.annotate(recPath, Annotations.Path)
        recNode.annotate(recType, Annotations.Type)

        retNode.annotate(retPath, Annotations.Path)
        retNode.annotate(retType, Annotations.Type)

        return MethodSignatureNode(startToken, endToken,
            IdentifierNode(startToken, endToken, name),
            recNode,
            params,
            retNode)
    }

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
    }

    override fun isReceiverSatisfied(by: Entity, context: ContextProtocol): Boolean
        = AnyEqualityConstraint(receiver).checkConformance(context, by)

    override fun evaluate(context: ContextProtocol): TypeProtocol = this
}