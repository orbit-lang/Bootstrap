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

object DiscardBindingPatternPathResolver : IPathResolver<DiscardBindingPatternNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: DiscardBindingPatternNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        return IPathResolver.Result.Success(Path.infer)
    }
}

object TypeBindingPatternPathResolver : IPathResolver<TypeBindingPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeBindingPatternNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.typeIdentifier.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.typeIdentifier, IPathResolver.Pass.Initial, environment, graph)
    }
}

object TypedIdentifierBindingPathResolver : IPathResolver<TypedIdentifierBindingPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypedIdentifierBindingPatternNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.typePattern.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.typePattern, IPathResolver.Pass.Initial, environment, graph)
    }
}

object StructuralPatternPathResolver : IPathResolver<StructuralPatternNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: StructuralPatternNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.typeExpressionNode.annotate(input.getGraphID(), Annotations.graphId)
        input.bindings.forEach { it.annotate(input.getGraphID(), Annotations.graphId) }

        pathResolverUtil.resolve(input.typeExpressionNode, pass, environment, graph)
        pathResolverUtil.resolveAll(input.bindings, pass, environment, graph)

        return IPathResolver.Result.Success(Path.empty)
    }
}