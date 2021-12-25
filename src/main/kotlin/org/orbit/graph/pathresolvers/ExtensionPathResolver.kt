package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.nodes.ExtensionNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ExtensionPathResolver(private val parentPath: Path) : PathResolver<ExtensionNode> {
    override val invocation: Invocation by inject()
    private val pathResolverUtil: PathResolverUtil by inject()

    override fun resolve(input: ExtensionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
        val targetTypePath = environment.getBinding(input.targetTypeNode.value, Binding.Kind.Union.entity, graph)
            .unwrap(this, input.firstToken.position)
        val graphID = graph.find(targetTypePath)

        input.annotate(graphID, Annotations.GraphID)
        input.annotate(targetTypePath.path, Annotations.Path)

        input.methodDefNodes.forEach {
            it.annotate(graphID, Annotations.GraphID)
            pathResolverUtil.resolve(it, pass, environment, graph)
        }

        return PathResolver.Result.Success(targetTypePath.path)
    }
}