package org.orbit.types.typeactions

import org.orbit.core.nodes.TraitDefNode
import org.orbit.types.components.Trait

class CreateTraitStub(override val node: TraitDefNode) : CreateStub<TraitDefNode, Trait> {
    override val constructor: (TraitDefNode) -> Trait = ::Trait
}