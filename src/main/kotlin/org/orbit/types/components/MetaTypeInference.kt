package org.orbit.types.components

import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.types.util.TypeMonomorphisation

object MetaTypeInference : TypeInference<MetaTypeNode> {
    override fun infer(context: Context, node: MetaTypeNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val typeConstructor = context.getTypeByPath(node.getPath())
            // TODO - Trait Constructors
            as? TypeConstructor
            ?: TODO("")

        // TODO - Recursive inference on type parameters
        val typeParameters = node.typeParameters
            .map { TypeExpressionInference.infer(context, it, null) }
            .map { it as ValuePositionType }

        val specialisation = TypeMonomorphisation(typeConstructor, typeParameters)

        return specialisation.specialise(context)
    }
}