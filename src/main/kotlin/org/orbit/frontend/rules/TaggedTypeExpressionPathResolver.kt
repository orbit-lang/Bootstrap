package org.orbit.frontend.rules

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TaggedTypeExpressionNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.IPathResolver
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TaggedTypeExpressionPathResolver : IPathResolver<TaggedTypeExpressionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TaggedTypeExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val parentVertex = graph.findVertex(input.getGraphID())
        val parentPath = OrbitMangler.unmangle(parentVertex.name)

        input.typeExpression.annotate(input.getGraphID(), Annotations.graphId)

        val nPath = parentPath + input.tag.value
        pathResolverUtil.resolve(input.typeExpression, pass, environment, graph)

        environment.bind(Binding.Kind.Type, input.tag.value, parentPath + input.tag.value)

        input.annotate(nPath, Annotations.path)
        input.typeExpression.annotate(nPath, Annotations.path)

        return IPathResolver.Result.Success(nPath)
    }
}