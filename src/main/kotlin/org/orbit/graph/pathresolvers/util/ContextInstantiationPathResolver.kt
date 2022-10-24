package org.orbit.graph.pathresolvers.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.util.Invocation
import org.orbit.util.toPath

object ContextInstantiationPathResolver : PathResolver<ContextInstantiationNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ContextInstantiationNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val nPath = environment.getCurrentContainerPath() + input.contextIdentifierNode.value

        input.annotate(nPath, Annotations.path)
        input.contextIdentifierNode.annotateByKey(input.getGraphID(), Annotations.graphId)
        input.typeParameters.forEach { it.annotateByKey(input.getGraphID(), Annotations.graphId) }

        val result = pathResolverUtil.resolve(input.contextIdentifierNode, pass, environment, graph)

        pathResolverUtil.resolveAll(input.typeParameters, pass, environment, graph)

        return result
    }
}

object ContextCompositionPathResolver : PathResolver<ContextCompositionNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ContextCompositionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.leftContext.annotateByKey(input.getGraphID(), Annotations.graphId)
        input.rightContext.annotateByKey(input.getGraphID(), Annotations.graphId)

        pathResolverUtil.resolve(input.leftContext, pass, environment, graph)
        return pathResolverUtil.resolve(input.rightContext, pass, environment, graph)
    }
}