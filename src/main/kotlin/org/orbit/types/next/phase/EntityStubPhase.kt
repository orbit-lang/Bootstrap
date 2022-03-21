package org.orbit.types.next.phase

import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.EntityDefNode
import org.orbit.types.next.components.Entity

interface EntityStubPhase<N: EntityDefNode, E: Entity> : TypePhase<N, E>