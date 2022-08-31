package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeIndexNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation

object TypeIndexResolver : PathResolver<TypeIndexNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: TypeIndexNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.annotateByKey(Binding.Self.path, Annotations.Path)

        return PathResolver.Result.Success(Binding.Self.path)
    }
}