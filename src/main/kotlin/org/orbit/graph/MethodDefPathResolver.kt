package org.orbit.graph

import org.orbit.core.Path
import org.orbit.core.nodes.MethodDefNode
import org.orbit.util.Invocation

class MethodDefPathResolver(
    override val invocation: Invocation,
    override val environment: Environment,
	override val graph: Graph
) : PathResolver<MethodDefNode> {
	override fun resolve(input: MethodDefNode, pass: PathResolver.Pass) : PathResolver.Result {
		val signatureResolver = MethodSignaturePathResolver(invocation, environment, graph)
		val signatureResult = signatureResolver.execute(PathResolver.InputType(input.signature, pass))

		signatureResult.withSuccess {
			input.annotate(it, Annotations.Path)
		}

		// NOTE - We're only resolving declared names. We are not performing type
		// checking/inference, so no need to delve into the method body

		return signatureResult
	}
}