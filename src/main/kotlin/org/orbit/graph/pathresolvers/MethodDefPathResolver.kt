package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.annotateByKey
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class MethodDefPathResolver : IPathResolver<MethodDefNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodDefNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		input.signature.annotateByKey(input.getGraphID(), Annotations.graphId)
		input.body.annotateByKey(input.getGraphID(), Annotations.graphId)

		input.context?.let {
			it.annotate(input.getGraphID(), Annotations.graphId)
			pathResolverUtil.resolve(it, pass, environment, graph)
		}

		val signatureResult = pathResolverUtil.resolve(input.signature, pass, environment, graph)

		signatureResult.withSuccess {
			input.annotateByKey(it, Annotations.path)
		}

		pathResolverUtil.resolve(input.body, IPathResolver.Pass.Initial, environment, graph)

		return signatureResult
	}
}