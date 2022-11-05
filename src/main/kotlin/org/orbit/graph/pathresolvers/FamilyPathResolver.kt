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

class FamilyPathResolver(private val parentPath: Path) : IPathResolver<FamilyNode> {
    override val invocation: Invocation by inject()

    override fun resolve(input: FamilyNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
        val path = if (pass == IPathResolver.Pass.Initial) {
            val path = parentPath + Path(input.familyIdentifierNode.value)

            input.annotateByKey(path, Annotations.path)
            environment.bind(Binding.Kind.Type, input.familyIdentifierNode.value, path)

            val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
            val graphID = graph.insert(input.familyIdentifierNode.value)

            graph.link(parentGraphID, graphID)

            input.annotateByKey(graphID, Annotations.graphId)
            input.properties.forEach {
                it.annotateByKey(graphID, Annotations.graphId)
                it.typeNode.annotateByKey(graphID, Annotations.graphId)
                it.defaultValue?.annotateByKey(graphID, Annotations.graphId)
            }

            path
        } else {
            val path = input.getPath()
            val typeResolver = TypeDefPathResolver(path)

            input.memberNodes.forEach {
                typeResolver.resolve(it, IPathResolver.Pass.Initial, environment, graph)
                val alias = Path(path.last()) + it.getPath().last()

                graph.alias(alias.toString(OrbitMangler), it.getGraphID())
                environment.bind(Binding.Kind.Type, alias.toString(OrbitMangler), alias)
            }
            input.memberNodes.forEach { typeResolver.resolve(it, IPathResolver.Pass.Last, environment, graph) }

            path
        }

        return IPathResolver.Result.Success(path)
    }
}