package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.Annotations
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object AlgebraicConstructorPathResolver : IPathResolver<AlgebraicConstructorNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: AlgebraicConstructorNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val parentGraphId = input.getGraphID()
        val nGraphId = graph.insert(input.typeIdentifier.value)

        graph.link(parentGraphId, nGraphId)

        val parentPath = OrbitMangler.unmangle(graph.getName(parentGraphId))
        val nPath = parentPath + input.typeIdentifier.value

        input.annotate(nPath, Annotations.path)

        environment.bind(Binding.Kind.Type, input.typeIdentifier.value, nPath)

        input.parameters.forEach { it.annotate(nGraphId, Annotations.graphId) }

        pathResolverUtil.resolveAll(input.parameters, IPathResolver.Pass.Initial, environment, graph)

        return IPathResolver.Result.Success(nPath)
    }
}