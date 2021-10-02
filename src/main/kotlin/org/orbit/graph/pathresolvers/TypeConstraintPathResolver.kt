package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.TraitConformanceTypeConstraintNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.util.Invocation

object TypeConstraintPathResolver : PathResolver<TraitConformanceTypeConstraintNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TraitConformanceTypeConstraintNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val constrainedTypePath = environment.getBinding(input.constrainedTypeNode.value, Binding.Kind.Type).unwrap(this, input.constrainedTypeNode.firstToken.position)
		val constraintTraitPath = environment.getBinding(input.constraintTraitNode.value, Binding.Kind.Trait).unwrap(this, input.constraintTraitNode.firstToken.position)

		input.constrainedTypeNode.annotate(constrainedTypePath.path, Annotations.Path)
		input.constraintTraitNode.annotate(constraintTraitPath.path, Annotations.Path)
		input.annotate(constrainedTypePath.path, Annotations.Path)

		return PathResolver.Result.Success(constrainedTypePath.path)
	}
}