package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.INode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

class AnyPathResolver<N: INode> : IPathResolver<N> {
    override val invocation: Invocation by inject()

    override fun resolve(input: N, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result
        = IPathResolver.Result.Success(Path.empty)
}