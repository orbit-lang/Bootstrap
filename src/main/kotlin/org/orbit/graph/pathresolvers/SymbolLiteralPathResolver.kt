package org.orbit.graph.pathresolvers

import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.types.next.intrinsics.Native

object SymbolLiteralPathResolver : LiteralPathResolver<SymbolLiteralNode>(Native.Types.Symbol.path)