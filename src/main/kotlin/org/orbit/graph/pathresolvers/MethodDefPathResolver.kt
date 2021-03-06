package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.MethodDefNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class MethodDefPathResolver : PathResolver<MethodDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val signatureResult = pathResolverUtil.resolve(input.signature, pass, environment, graph)

		signatureResult.withSuccess {
			input.annotate(it, Annotations.Path)
		}

		pathResolverUtil.resolve(input.body, PathResolver.Pass.Initial, environment, graph)

		return signatureResult
	}
}