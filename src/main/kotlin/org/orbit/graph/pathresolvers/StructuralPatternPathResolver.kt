package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object DiscardBindingPatternPathResolver : PathResolver<DiscardBindingPatternNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: DiscardBindingPatternNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        return PathResolver.Result.Success(Path.infer)
    }
}

object TypeBindingPatternPathResolver : PathResolver<TypeBindingPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeBindingPatternNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.typeIdentifier.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.typeIdentifier, PathResolver.Pass.Initial, environment, graph)
    }
}

object TypedIdentifierBindingPathResolver : PathResolver<TypedIdentifierBindingPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypedIdentifierBindingPatternNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.typePattern.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.typePattern, PathResolver.Pass.Initial, environment, graph)
    }
}

object StructuralPatternPathResolver : PathResolver<StructuralPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: StructuralPatternNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        input.bindings.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }
        pathResolverUtil.resolveAll(input.bindings, PathResolver.Pass.Initial, environment, graph)

        return PathResolver.Result.Success(Path.empty)
    }
}