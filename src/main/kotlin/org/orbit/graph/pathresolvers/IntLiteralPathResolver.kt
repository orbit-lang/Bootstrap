package org.orbit.graph.pathresolvers

import org.orbit.core.nodes.IntLiteralNode
import org.orbit.types.components.IntrinsicTypes

object IntLiteralPathResolver : LiteralPathResolver<IntLiteralNode>(IntrinsicTypes.Int.path)