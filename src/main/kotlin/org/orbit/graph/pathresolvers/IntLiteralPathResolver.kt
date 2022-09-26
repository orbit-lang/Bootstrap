package org.orbit.graph.pathresolvers

import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.IntLiteralNode

object IntLiteralPathResolver : LiteralPathResolver<IntLiteralNode>(OrbCoreNumbers.intType.getPath())