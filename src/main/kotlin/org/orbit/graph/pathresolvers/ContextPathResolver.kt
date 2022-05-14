package org.orbit.graph.pathresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.SerialIndex
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ContextNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

data class ContextPathResolver(val parentPath: Path) : PathResolver<ContextNode>, KoinComponent {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ContextNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val path = parentPath + input.contextIdentifier.value

        if (pass == PathResolver.Pass.Initial) {
            input.annotate(path, Annotations.Path)

            environment.bind(Binding.Kind.Context, input.contextIdentifier.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.contextIdentifier.value)

            graph.link(parentGraphID, graphID)

            input.annotate(graphID, Annotations.GraphID)
            input.clauses.forEach { it.annotate(graphID, Annotations.GraphID) }

            for (typeParameter in input.typeVariables.withIndex()) {
                val nPath = path + typeParameter.value.value

                typeParameter.value.annotate(nPath, Annotations.Path)
                typeParameter.value.annotate(graphID, Annotations.GraphID)
                typeParameter.value.annotate(SerialIndex(typeParameter.index), Annotations.Index)

                val vertexID = graph.insert(typeParameter.value.value)

                graph.link(graphID, vertexID)

                environment.bind(Binding.Kind.TypeParameter, typeParameter.value.value, nPath, vertexID)
            }
        } else {
            val parentGraphID = input.getGraphID()

            input.clauses.forEach { it.annotate(parentGraphID, Annotations.GraphID) }
            pathResolverUtil.resolveAll(input.clauses, pass, environment, graph)
        }

        return PathResolver.Result.Success(path)
    }
}