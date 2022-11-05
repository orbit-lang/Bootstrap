package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.TraitConformanceTypeConstraintNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

object TypeConstraintPathResolver : IPathResolver<TraitConformanceTypeConstraintNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TraitConformanceTypeConstraintNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph): IPathResolver.Result {
		val constrainedTypePath = environment.getBinding(input.constrainedTypeNode.value, Binding.Kind.Type)
			.unwrap(this, input.constrainedTypeNode.firstToken.position)
		// NOTE - We were using the context Binding.Kind.Union(Binding.Kind.Trait, Binding.Kind.TraitConstructor) here,
		//  but maybe its best to let the Type System limit what can appear on the right-hand side here
		val constraintTraitPath = environment.getBinding(input.constraintTraitNode.value, Binding.Kind.Union.entity).unwrap(this, input.constraintTraitNode.firstToken.position)

		input.constrainedTypeNode.annotateByKey(constrainedTypePath.path, Annotations.path)
		input.constraintTraitNode.annotateByKey(constraintTraitPath.path, Annotations.path)
		input.annotateByKey(constrainedTypePath.path, Annotations.path)

		pathResolverUtil.resolve(input.constraintTraitNode, pass, environment, graph)

		return IPathResolver.Result.Success(constrainedTypePath.path)
	}
}