package org.orbit.graph.pathresolvers

import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.SymbolLiteralNode

object SymbolLiteralPathResolver : LiteralPathResolver<SymbolLiteralNode>(OrbitMangler.unmangle("Orb::Core::Types::Symbol"))