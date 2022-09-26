package org.orbit.graph.pathresolvers

import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.TupleLiteralNode

object TupleLiteralPathResolver : LiteralPathResolver<TupleLiteralNode>(OrbitMangler.unmangle("Orb::Core::Types::Tuple"))