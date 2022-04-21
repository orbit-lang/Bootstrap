package org.orbit.graph.pathresolvers.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.AnnotatedNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getScopeIdentifier
import org.orbit.graph.pathresolvers.AnnotationResolver
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.Invocation
import org.orbit.util.partial

class PathResolverUtil : KoinComponent {
	private val invocation: Invocation by inject()
	private val pathResolvers = mutableMapOf<Class<out Node>, PathResolver<*>>()

	fun <N: Node> registerPathResolver(pathResolver: PathResolver<N>, nodeType: Class<N>) {
		pathResolvers[nodeType] = pathResolver
	}

	fun <N: Node> resolve(node: N, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		val resolver = pathResolvers[node::class.java] as? PathResolver<N>
			?: throw invocation.make<CanonicalNameResolver>("Cannot resolve path for Node ${node::class.java}", node)

		val result = resolver.execute(PathResolver.InputType(node, pass))

		if (node is AnnotatedNode && pass == node.annotationPass) {
			// If we resolve Annotations after the annotated node, we can sponge off of it for the scope
			node.phaseAnnotationNodes.forEach { it.annotate(node.getScopeIdentifier(), Annotations.Scope) }
			node.phaseAnnotationNodes.forEach { AnnotationResolver(it, Node::class.java).resolve(it, pass, environment, graph) }
		}

		return result
	}

	fun <N: Node> resolveAll(nodes: List<N>, pass: PathResolver.Pass, environment: Environment, graph: Graph) : List<PathResolver.Result> {
		return nodes.map(partial(::resolve, pass, environment, graph))
	}
}