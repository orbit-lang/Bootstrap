package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.FamilyNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class FamilyPathResolver(private val parentPath: Path) : PathResolver<FamilyNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: FamilyNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val path = if (pass == PathResolver.Pass.Initial) {
            val path = parentPath + Path(input.familyIdentifierNode.value)

            input.annotate(path, Annotations.Path)

            val kind = when (input.isRequired) {
                true -> Binding.Kind.RequiredType
                false -> Binding.Kind.Type
            }

            environment.bind(kind, input.familyIdentifierNode.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.familyIdentifierNode.value)

            graph.link(parentGraphID, graphID)

            input.annotate(graphID, Annotations.GraphID)
            input.propertyPairs.forEach {
                it.annotate(graphID, Annotations.GraphID)
                it.typeExpressionNode.annotate(graphID, Annotations.GraphID)
            }

            path
        } else {
            val path = input.getPath()
            val typeResolver = TypeDefPathResolver(path)

            input.memberNodes.forEach {
                typeResolver.resolve(it, PathResolver.Pass.Initial, environment, graph)
                val alias = Path(path.last()) + it.getPath().last()

                graph.alias(alias.toString(OrbitMangler), it.getGraphID())
                environment.bind(Binding.Kind.Type, alias.toString(OrbitMangler), alias)
            }
            input.memberNodes.forEach { typeResolver.resolve(it, PathResolver.Pass.Last, environment, graph) }

            path
        }

        return PathResolver.Result.Success(path)
    }
}