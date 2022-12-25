package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ProjectionEffectNode
import org.orbit.core.nodes.TypeEffectNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ProjectionEffectPathResolver : IPathResolver<ProjectionEffectNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ProjectionEffectNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.type.annotate(input.getGraphID(), Annotations.graphId)
        input.trait.annotate(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.type, pass, environment, graph)

        return pathResolverUtil.resolve(input.trait, pass, environment, graph)
    }
}

class TypeEffectPathResolver(private val parentPath: Path) : IPathResolver<TypeEffectNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeEffectNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = parentPath + input.identifier.value

        if (pass == IPathResolver.Pass.Initial) {
            input.annotate(path, Annotations.path)
            input.identifier.annotate(path, Annotations.path)

            environment.bind(Binding.Kind.Attribute, input.identifier.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.identifier.value)

            graph.link(parentGraphID, graphID)

            input.annotate(graphID, Annotations.graphId)
            input.body.annotate(graphID, Annotations.graphId)
        } else {
            input.body.annotate(path, Annotations.path)

            input.parameters.forEach {
                it.annotate(input.getGraphID(), Annotations.graphId)
                environment.bind(Binding.Kind.Type, it.getTypeName(), Path(it.getTypeName()))
            }

            pathResolverUtil.resolve(input.body, pass, environment, graph).also {
                input.body.annotate(it.asSuccess().path, Annotations.path)
                input.parameters.forEach { p ->
                    environment.unbind(Binding.Kind.Type, p.getTypeName(), Path(p.getTypeName()))
                }
            }
        }

        return IPathResolver.Result.Success(path)
    }
}