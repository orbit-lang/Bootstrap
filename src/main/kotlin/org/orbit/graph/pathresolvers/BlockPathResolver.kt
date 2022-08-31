package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.*
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotateByKey
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Invocation

class BlockPathResolver : PathResolver<BlockNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: BlockNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return environment.withScope {

			// TODO - Non-linear routes through a block, e.g. conditionals, controls etc
			var result: PathResolver.Result = PathResolver.Result.Success(OrbitMangler.unmangle(Native.Types.Unit.type.fullyQualifiedName))

			for (node in input.body) {
				node.annotateByKey(input.getGraphID(), Annotations.GraphID)

				when (node) {
					is PrintNode ->
						pathResolverUtil.resolve(node, pass, environment, graph)

					is ReturnStatementNode -> {
						node.valueNode.expressionNode.annotateByKey(input.getGraphID(), Annotations.GraphID)
						result = pathResolverUtil.resolve(node.valueNode.expressionNode, pass, environment, graph)
					}

					is AssignmentStatementNode -> pathResolverUtil.resolve(node, pass, environment, graph)

					is DeferNode -> result =
						pathResolverUtil.resolve(node.blockNode, pass, environment, graph)

					is MethodCallNode -> result = pathResolverUtil.resolve(node, pass, environment, graph)

					else -> result = pathResolverUtil.resolve(node, pass, environment, graph)

//					else -> throw invocation.make<CanonicalNameResolver>("Unsupported statement in block", node)
				}
			}

			result
		}
	}
}