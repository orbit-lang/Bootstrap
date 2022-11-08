package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ProjectedPropertyAssignmentNode
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object ProjectedPropertyAssignmentPathResolver : IPathResolver<ProjectedPropertyAssignmentNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ProjectedPropertyAssignmentNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.expression.annotate(input.getGraphID(), Annotations.graphId)
        return pathResolverUtil.resolve(input.expression, pass, environment, graph)
    }
}