package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeLambdaNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeLambdaPathResolver : IPathResolver<TypeLambdaNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeLambdaNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result = when (pass) {
        IPathResolver.Pass.Last -> IPathResolver.Result.Success(input.getPath())
        else -> environment.withScope {
            input.domain.forEach { tp ->
                environment.bind(Binding.Kind.Type, tp.getTypeName(), Path(tp.getTypeName()))
            }

            input.codomain.annotate(input.getGraphID(), Annotations.graphId)

            pathResolverUtil.resolve(input.codomain, pass, environment, graph).also { result -> input.annotate(result.asSuccess().path, Annotations.path) }.also {
                input.domain.forEach { tp ->
                    environment.unbind(Binding.Kind.Type, tp.getTypeName(), Path(tp.getTypeName()))
                }
            }
        }
    }
}