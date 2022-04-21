package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.LiteralNode
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

abstract class LiteralPathResolver<N: LiteralNode<*>>(private val path: Path) : PathResolver<N> {
	override val invocation: Invocation by inject()

	override fun resolve(
        input: N,
        pass: PathResolver.Pass,
        environment: Environment,
        graph: Graph
	): PathResolver.Result {
		input.annotate(path, Annotations.Path)

		return PathResolver.Result.Success(path)
	}
}