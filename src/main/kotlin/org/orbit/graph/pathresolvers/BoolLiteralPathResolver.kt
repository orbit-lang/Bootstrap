package org.orbit.graph.pathresolvers

import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.BoolLiteralNode

object BoolLiteralPathResolver : LiteralPathResolver<BoolLiteralNode>(OrbitMangler.unmangle("Orb::Core::Types::Bool"))