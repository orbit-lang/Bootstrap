package org.orbit.graph.pathresolvers

import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.RealLiteralNode

object RealLiteralPathResolver : LiteralPathResolver<RealLiteralNode>(OrbCoreNumbers.realType.getPath())