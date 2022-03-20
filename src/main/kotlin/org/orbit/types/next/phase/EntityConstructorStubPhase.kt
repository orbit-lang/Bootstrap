package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.types.next.components.Entity
import org.orbit.types.next.components.PolymorphicType

interface EntityConstructorStubPhase<N: EntityConstructorNode, E: Entity> : TypePhase<N, PolymorphicType<E>>, KoinComponent