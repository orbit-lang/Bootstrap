package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeLambdaConstraintPathResolver : IPathResolver<TypeLambdaConstraintNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: TypeLambdaConstraintNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.invocation.annotate(input.getGraphID(), Annotations.graphId)

        return pathResolverUtil.resolve(input.invocation, pass, environment, graph)
    }
}

object TypeLambdaPathResolver : IPathResolver<TypeLambdaNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    private fun resolveDependentTypeParameter(node: DependentTypeParameterNode, environment: Environment, graph: Graph) {
        pathResolverUtil.resolve(node.type, IPathResolver.Pass.Initial, environment, graph)

        environment.bind(Binding.Kind.Value, node.identifier.getTypeName(), Path(node.identifier.getTypeName()))
    }

    private fun resolveTypeParameter(node: ITypeLambdaParameterNode, environment: Environment, graph: Graph) = when (node) {
        is DependentTypeParameterNode -> resolveDependentTypeParameter(node, environment, graph)
        else -> environment.bind(Binding.Kind.Type, node.getTypeName(), Path(node.getTypeName()))
    }

    override fun resolve(input: TypeLambdaNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result = when (pass) {
        IPathResolver.Pass.Last -> {
            IPathResolver.Result.Success(input.getPath())
        }

        else -> environment.withScope {
            input.domain.forEach { resolveTypeParameter(it, environment, graph) }

            input.codomain.annotate(input.getGraphID(), Annotations.graphId)
            input.constraints.forEach { c ->
                c.annotate(input.getGraphID(), Annotations.graphId)
                pathResolverUtil.resolve(c, pass, environment, graph)
            }

            input.elseClause?.let {
                it.annotate(input.getGraphID(), Annotations.graphId)
                pathResolverUtil.resolve(it, pass, environment, graph)
            }

            pathResolverUtil.resolve(input.codomain, pass, environment, graph).also { result -> input.annotate(result.asSuccess().path, Annotations.path) }.also {
                input.annotate(it.asSuccess().path, Annotations.path)

                input.domain.forEach { tp ->
                    environment.unbind(Binding.Kind.Type, tp.getTypeName(), Path(tp.getTypeName()))
                    environment.unbind(Binding.Kind.Value, tp.getTypeName(), Path(tp.getTypeName()))
                }
            }
        }
    }
}