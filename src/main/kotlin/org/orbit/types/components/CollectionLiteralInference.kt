package org.orbit.types.components

import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.types.util.TypeMonomorphisation

object CollectionLiteralInference : TypeInference<CollectionLiteralNode> {
    override fun infer(context: Context, node: CollectionLiteralNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val base = IntrinsicTypes.Array.type as TypeConstructor
        val elementTypes = node.elements.map { TypeInferenceUtil.infer(context, it) }

        // TODO - Heterogeneous lists should resolve to closest common ancestor Trait (or Any if no common Traits)
        //  For now, just take the first element or AnyType if empty
        val elementType = (elementTypes.firstOrNull() ?: IntrinsicTypes.AnyType.type) as ValuePositionType
        val specialist = TypeMonomorphisation(base, listOf(elementType), true)

        val nArrayType = specialist.specialise(context)

        context.registerIntrinsicTypeAlias(nArrayType, elementType)

        return nArrayType
    }
}