package org.orbit.graph.pathresolvers.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.INode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation
import org.orbit.util.partial

class PathResolverUtil : KoinComponent {
	private val invocation: Invocation by inject()
	private val pathResolvers = mutableMapOf<Class<out INode>, PathResolver<*>>()

	fun <N: INode> registerPathResolver(pathResolver: PathResolver<N>, nodeType: Class<N>) {
		pathResolvers[nodeType] = pathResolver
	}

	fun <N: INode> resolve(node: N, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val resolver = pathResolvers[node::class.java] as? PathResolver<N>
			?: throw invocation.make<CanonicalNameResolver>("Cannot resolve path for Node ${node::class.java}", node)

		val result = resolver.execute(PathResolver.InputType(node, pass))

		return result
	}

	fun <N: INode> resolveAll(nodes: List<N>, pass: PathResolver.Pass, environment: Environment, graph: Graph) : List<PathResolver.Result> {
		return nodes.map(partial(::resolve, pass, environment, graph))
	}
}