package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TypeQueryExpressionNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeQueryPathResolver : IPathResolver<TypeQueryExpressionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeQueryExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = Path(input.resultIdentifier.value)

        environment.bind(Binding.Kind.Type, input.resultIdentifier.value, path)
        input.clause.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.clause, pass, environment, graph).also {
            environment.unbind(Binding.Kind.Type, input.resultIdentifier.value, path)
        }
    }
}