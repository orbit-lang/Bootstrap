package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.EffectNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class EffectPathResolver(val parentPath: Path) : IPathResolver<EffectNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: EffectNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = parentPath + input.identifier.value

        if (pass == IPathResolver.Pass.Initial) {
            input.annotate(path, Annotations.path)
            input.identifier.annotate(path, Annotations.path)

            environment.bind(Binding.Kind.Attribute, input.identifier.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.identifier.value)

            graph.link(parentGraphID, graphID)

            input.annotate(graphID, Annotations.graphId)
            input.identifier.annotate(graphID, Annotations.graphId)
            input.lambda.annotate(graphID, Annotations.graphId)
        } else {
            input.identifier.annotate(path, Annotations.path)
            pathResolverUtil.resolve(input.lambda, pass, environment, graph)
        }

        return IPathResolver.Result.Success(path)
    }
}