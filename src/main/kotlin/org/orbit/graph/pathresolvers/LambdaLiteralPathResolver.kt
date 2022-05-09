package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.LambdaLiteralNode
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Invocation

object LambdaLiteralPathResolver : PathResolver<LambdaLiteralNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: LambdaLiteralNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.bindings.forEach { pathResolverUtil.resolve(it, pass, environment, graph) }
        val bodyPaths = input.body.body.map { pathResolverUtil.resolve(it, pass, environment, graph) }

        return PathResolver.Result.Success(
            bodyPaths.lastOrNull()?.asSuccessOrNull()?.path
                ?: Native.Types.Unit.path
        )
    }
}