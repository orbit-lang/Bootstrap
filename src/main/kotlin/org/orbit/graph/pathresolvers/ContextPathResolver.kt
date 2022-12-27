package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
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

data class ContextPathResolver(val parentPath: Path) : IPathResolver<ContextNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ContextNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = parentPath + input.contextIdentifier.value

        if (pass == IPathResolver.Pass.Initial) {
            input.annotateByKey(path, Annotations.path)

            environment.bind(Binding.Kind.Context, input.contextIdentifier.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.contextIdentifier.value)

            graph.link(parentGraphID, graphID)

            input.annotateByKey(graphID, Annotations.graphId)
            input.clauses.forEach { it.annotateByKey(graphID, Annotations.graphId) }
        } else {
            val graphID = input.getGraphID()

            for (typeParameter in input.typeVariables.withIndex()) {
                val nPath = path + typeParameter.value.value

                typeParameter.value.annotateByKey(nPath, Annotations.path)
                typeParameter.value.annotateByKey(graphID, Annotations.graphId)
                typeParameter.value.annotateByKey(typeParameter.index, Annotations.index)

                val vertexID = graph.insert(typeParameter.value.value)

                graph.link(graphID, vertexID)
                graph.alias(nPath.toString(OrbitMangler), vertexID)

                environment.bind(Binding.Kind.Type, typeParameter.value.value, nPath, vertexID)
            }

            input.variables.forEach {
                it.annotate(graphID, Annotations.graphId)
                pathResolverUtil.resolve(it, pass, environment, graph)
            }

            input.clauses.forEach { it.annotateByKey(graphID, Annotations.graphId) }
            pathResolverUtil.resolveAll(input.clauses, pass, environment, graph)

            input.body.forEach { it.annotate(graphID, Annotations.graphId) }

            val typeResolver = TypeDefPathResolver(parentPath)
            val traitResolver = TraitDefPathResolver(parentPath)
            val methodResolver = MethodDefPathResolver()

            for (decl in input.body) {
                val resolver = when (decl) {
                    is TypeDefNode -> typeResolver
                    is TraitDefNode -> traitResolver
                    is MethodDefNode -> methodResolver
                    is ProjectionNode -> ProjectionPathResolver
                    is OperatorDefNode -> OperatorDefPathResolver(parentPath)
                    is TypeAliasNode -> TypeAliasPathResolver(parentPath)
                    else -> pathResolverUtil.resolve(decl, pass, environment, graph)
                } as IPathResolver<IContextDeclarationNode>

                resolver.resolve(decl, IPathResolver.Pass.Initial, environment, graph)
                resolver.resolve(decl, IPathResolver.Pass.Last, environment, graph)
            }
        }

        for (typeParameter in input.typeVariables.withIndex()) {
            val nPath = path + typeParameter.value.value
            environment.unbind(Binding.Kind.Type, typeParameter.value.value, nPath)
        }

        return IPathResolver.Result.Success(path)
    }
}