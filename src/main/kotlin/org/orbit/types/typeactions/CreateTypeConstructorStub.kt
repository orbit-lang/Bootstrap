package org.orbit.types.typeactions

import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.EntityConstructor
import org.orbit.types.components.TraitConstructor
import org.orbit.types.components.TypeConstructor

class CreateEntityConstructorStub<N: EntityConstructorNode, C: EntityConstructor>(override val node: N, override val constructor: (N) -> C) : CreateStub<N, C>

class CreateTypeConstructorStub(
    override val node: TypeConstructorNode
) : CreateStub<TypeConstructorNode, TypeConstructor> {
    override val constructor: (TypeConstructorNode) -> TypeConstructor = { TypeConstructor(node = it) }
}

class CreateTraitConstructorStub(
    override val node: TraitConstructorNode
) : CreateStub<TraitConstructorNode, TraitConstructor> {
    override val constructor: (TraitConstructorNode) -> TraitConstructor = { TraitConstructor(node = it) }
}
