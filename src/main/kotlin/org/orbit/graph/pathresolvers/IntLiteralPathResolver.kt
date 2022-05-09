package org.orbit.graph.pathresolvers

import org.orbit.core.nodes.IntLiteralNode
import org.orbit.types.next.intrinsics.Native

object IntLiteralPathResolver : LiteralPathResolver<IntLiteralNode>(Native.Types.Int.path)