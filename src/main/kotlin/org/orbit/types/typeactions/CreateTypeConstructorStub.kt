package org.orbit.types.typeactions

import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.TypeConstructor

class CreateTypeConstructorStub(override val node: TypeConstructorNode) : CreateStub<TypeConstructorNode, TypeConstructor> {
    override val constructor: (TypeConstructorNode) -> TypeConstructor = { TypeConstructor(node = it) }
}

