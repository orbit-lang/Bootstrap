package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.core.nodes.LiteralNode

interface AbstractLiteralUnit<T> : CodeUnit<LiteralNode<T>>
interface AbstractCollectionLiteralUnit : CodeUnit<CollectionLiteralNode>