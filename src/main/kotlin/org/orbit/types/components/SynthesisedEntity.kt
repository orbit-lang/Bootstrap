package org.orbit.types.components

data class SynthesisedEntity(val entity: Entity, val metaType: MetaType): TypeProtocol by entity