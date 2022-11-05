package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.util.Invocation

object MetaTypePathResolver : IPathResolver<MetaTypeNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MetaTypeNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		val typeConstructorResult = TypeExpressionPathResolver.resolve(
            input.typeConstructorIdentifier,
            pass, environment, graph
        ).asSuccess()

		input.typeParameters.forEach {
			val pid = input.getGraphIDOrNull()
			if (pid != null) {
				it.annotateByKey(pid, Annotations.graphId)
			}

			TypeExpressionPathResolver.resolve(it, pass, environment, graph)
		}

		input.annotateByKey(typeConstructorResult.path, Annotations.path)
		input.typeConstructorIdentifier.annotateByKey(typeConstructorResult.path, Annotations.path)

		return IPathResolver.Result.Success(typeConstructorResult.path)
	}
}