package org.orbit.graph.pathresolvers

import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.types.components.IntrinsicTypes

object SymbolLiteralPathResolver : LiteralPathResolver<SymbolLiteralNode>(IntrinsicTypes.Symbol.path)