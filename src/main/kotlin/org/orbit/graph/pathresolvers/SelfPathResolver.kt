package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.SelfNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object SelfPathResolver : IPathResolver<SelfNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: SelfNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        return IPathResolver.Result.Success(Path.self)
    }
}