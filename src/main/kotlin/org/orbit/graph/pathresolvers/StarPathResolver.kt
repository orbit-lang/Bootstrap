package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.NeverNode
import org.orbit.core.nodes.StarNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object StarPathResolver : IPathResolver<StarNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: StarNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result
        = IPathResolver.Result.Success(Path.any)
}

object NeverPathResolver : IPathResolver<NeverNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: NeverNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result
        = IPathResolver.Result.Success(Path.never)
}