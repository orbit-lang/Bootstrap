package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode

object TypeExpressionInference : TypeInference<TypeExpressionNode> {
    override fun infer(context: Context, node: TypeExpressionNode, typeAnnotation: TypeProtocol?): TypeProtocol = when (node) {
        is TypeIdentifierNode -> {
//            IntrinsicTypes.Type.type
            context.getType(node.getPath().toString(OrbitMangler))
        }
        is MetaTypeNode -> MetaTypeInference.infer(context, node, typeAnnotation)
        else -> TODO("???")
    }
}