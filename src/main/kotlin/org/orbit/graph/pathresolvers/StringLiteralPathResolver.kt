package org.orbit.graph.pathresolvers

import org.orbit.backend.typesystem.intrinsics.OrbCoreStrings
import org.orbit.core.nodes.StringLiteralNode

object StringLiteralPathResolver : LiteralPathResolver<StringLiteralNode>(OrbCoreStrings.stringType.getPath())