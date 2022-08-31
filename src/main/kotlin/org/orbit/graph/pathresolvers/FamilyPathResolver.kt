package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.FamilyNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.util.Invocation

class FamilyPathResolver(private val parentPath: Path) : PathResolver<FamilyNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: FamilyNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val path = if (pass == PathResolver.Pass.Initial) {
            val path = parentPath + Path(input.familyIdentifierNode.value)

            input.annotateByKey(path, Annotations.Path)
            environment.bind(Binding.Kind.Type, input.familyIdentifierNode.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.familyIdentifierNode.value)

            graph.link(parentGraphID, graphID)

            input.annotateByKey(graphID, Annotations.GraphID)
            input.properties.forEach {
                it.annotateByKey(graphID, Annotations.GraphID)
                it.typeNode.annotateByKey(graphID, Annotations.GraphID)
                it.defaultValue?.annotateByKey(graphID, Annotations.GraphID)
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