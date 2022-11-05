package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeIndexNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object TypeIndexResolver : IPathResolver<TypeIndexNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: TypeIndexNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.annotateByKey(Binding.Self.path, Annotations.path)

        return IPathResolver.Result.Success(Binding.Self.path)
    }
}