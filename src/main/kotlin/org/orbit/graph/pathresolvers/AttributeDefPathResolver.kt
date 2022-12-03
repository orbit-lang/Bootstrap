package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object AttributeInvocationPathResolver : IPathResolver<AttributeInvocationNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: AttributeInvocationNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.identifier.annotate(input.getGraphID(), Annotations.graphId)

        val result = pathResolverUtil.resolve(input.identifier, pass, environment, graph)

        input.arguments.forEach {
            it.annotate(input.getGraphID(), Annotations.graphId)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return result
    }
}

object AttributeOperatorExpressionPathResolver : IPathResolver<AttributeOperatorExpressionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: AttributeOperatorExpressionNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        input.leftExpression.annotate(input.getGraphID(), Annotations.graphId)
        input.rightExpression.annotate(input.getGraphID(), Annotations.graphId)

        val sourceTypePath = pathResolverUtil.resolve(input.leftExpression, pass, environment, graph)
        val targetTypePath = pathResolverUtil.resolve(input.rightExpression, IPathResolver.Pass.Initial, environment, graph)

        input.leftExpression.annotate(sourceTypePath.asSuccess().path, Annotations.path)
        input.rightExpression.annotate(targetTypePath.asSuccess().path, Annotations.path)

        return sourceTypePath
    }
}

object AttributeArrowPathResolver : IPathResolver<AttributeArrowNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: AttributeArrowNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        return environment.withScope {
            input.constraint.annotate(input.getGraphID(), Annotations.graphId)
            input.parameters.forEach {
                it.annotate(input.getGraphID(), Annotations.graphId)
                environment.bind(Binding.Kind.Type, it.getTypeName(), Path(it.getTypeName()))
            }

            pathResolverUtil.resolve(input.constraint, pass, environment, graph).also {
                input.annotate(it.asSuccess().path, Annotations.path)
                input.parameters.forEach { p ->
                    environment.unbind(Binding.Kind.Type, p.getTypeName(), Path(p.getTypeName()))
                }
            }
        }
    }
}

class AttributeDefPathResolver(private val parentPath: Path) : IPathResolver<AttributeDefNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: AttributeDefNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = parentPath + input.identifier.value

        if (pass == IPathResolver.Pass.Initial) {
            input.annotate(path, Annotations.path)

            environment.bind(Binding.Kind.Attribute, input.identifier.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.identifier.value)

            graph.link(parentGraphID, graphID)

            input.annotate(graphID, Annotations.graphId)
            input.arrow.annotate(graphID, Annotations.graphId)
        } else {
            pathResolverUtil.resolve(input.arrow, IPathResolver.Pass.Initial, environment, graph)
        }

        return IPathResolver.Result.Success(path)
    }
}