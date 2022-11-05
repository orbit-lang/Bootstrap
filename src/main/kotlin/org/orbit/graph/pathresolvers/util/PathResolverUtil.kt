package org.orbit.graph.pathresolvers.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.INode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.IPathResolver
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation
import org.orbit.util.partial

class PathResolverUtil : KoinComponent {
	private val invocation: Invocation by inject()
	private val pathResolvers = mutableMapOf<Class<out INode>, IPathResolver<*>>()

	fun <N: INode> registerPathResolver(pathResolver: IPathResolver<N>, nodeType: Class<N>) {
		pathResolvers[nodeType] = pathResolver
	}

	fun <N: INode> resolve(node: N, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		val resolver = pathResolvers[node::class.java] as? IPathResolver<N>
			?: throw invocation.make<CanonicalNameResolver>("Cannot resolve path for Node ${node::class.java}", node)

		val result = resolver.execute(IPathResolver.InputType(node, pass))

		return result
	}

	fun <N: INode> resolveAll(nodes: List<N>, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : List<IPathResolver.Result> {
		return nodes.map(partial(::resolve, pass, environment, graph))
	}
}