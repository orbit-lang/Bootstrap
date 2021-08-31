package org.orbit.types.typeactions

import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.Type

class CreateTypeStub(override val node: TypeDefNode) : CreateStub<TypeDefNode, Type> {
    override val constructor: (TypeDefNode) -> Type = ::Type
}