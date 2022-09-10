package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.INode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

class AnyPathResolver<N: INode> : PathResolver<N> {
    override val invocation: Invocation by inject()

    override fun resolve(input: N, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result
        = PathResolver.Result.Success(Path.empty)
}