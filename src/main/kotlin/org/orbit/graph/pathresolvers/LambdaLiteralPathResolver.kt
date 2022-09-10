package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.LambdaLiteralNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object LambdaLiteralPathResolver : PathResolver<LambdaLiteralNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: LambdaLiteralNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.bindings.forEach { pathResolverUtil.resolve(it, pass, environment, graph) }
        input.body.annotateByKey(input.getGraphID(), Annotations.graphId)
        val bodyPaths = input.body.body.map {
            it.annotateByKey(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return PathResolver.Result.Success(
            bodyPaths.lastOrNull()?.asSuccessOrNull()?.path
                ?: OrbitMangler.unmangle("Orb::Core::Types::Unit")
        )
    }
}