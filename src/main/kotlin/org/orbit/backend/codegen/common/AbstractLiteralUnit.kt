package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.core.nodes.ILiteralNode

interface AbstractLiteralUnit<T> : CodeUnit<ILiteralNode<T>>
interface AbstractCollectionLiteralUnit : CodeUnit<CollectionLiteralNode>