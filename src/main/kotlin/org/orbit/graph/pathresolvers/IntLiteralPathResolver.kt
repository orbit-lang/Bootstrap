package org.orbit.graph.pathresolvers

import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.IntLiteralNode

object IntLiteralPathResolver : LiteralPathResolver<IntLiteralNode>(OrbitMangler.unmangle("Orb::Core::Types::Int"))