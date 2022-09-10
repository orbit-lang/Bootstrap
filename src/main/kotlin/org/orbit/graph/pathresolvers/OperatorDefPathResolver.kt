package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class OperatorDefPathResolver(private val parentPath: Path) : PathResolver<OperatorDefNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: OperatorDefNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val path = parentPath + Path(input.identifierNode.identifier)

        input.annotateByKey(path, Annotations.path)

        environment.bind(Binding.Kind.Type, input.identifierNode.identifier, path)

        val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
        val graphID = graph.insert(input.identifierNode.identifier)

        graph.link(parentGraphID, graphID)

        input.methodReferenceNode.annotateByKey(graphID, Annotations.graphId)
        pathResolverUtil.resolve(input.methodReferenceNode, pass, environment, graph)

        return PathResolver.Result.Success(path)
    }
}