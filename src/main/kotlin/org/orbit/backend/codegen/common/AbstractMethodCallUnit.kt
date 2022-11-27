package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.nodes.IInvokableNode
import org.orbit.core.nodes.MethodCallNode
import org.orbit.core.nodes.ReferenceCallNode

interface AbstractCallUnit<N: IInvokableNode> : CodeUnit<N>
interface AbstractReferenceCallUnit : AbstractCallUnit<ReferenceCallNode>
interface AbstractMethodCallUnit : AbstractCallUnit<MethodCallNode>