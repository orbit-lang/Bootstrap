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

object TypeConstraintPathResolver : PathResolver<TraitConformanceTypeConstraintNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TraitConformanceTypeConstraintNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val constrainedTypePath = environment.getBinding(input.constrainedTypeNode.value, Binding.Kind.TypeParameter).unwrap(this, input.constrainedTypeNode.firstToken.position)
		// NOTE - We were using the context Binding.Kind.Union(Binding.Kind.Trait, Binding.Kind.TraitConstructor) here,
		//  but maybe its best to let the Type System limit what can appear on the right-hand side here
		val constraintTraitPath = environment.getBinding(input.constraintTraitNode.value, Binding.Kind.Union.entityOrConstructorOrParameter).unwrap(this, input.constraintTraitNode.firstToken.position)

		input.constrainedTypeNode.annotateByKey(constrainedTypePath.path, Annotations.Path)
		input.constraintTraitNode.annotateByKey(constraintTraitPath.path, Annotations.Path)
		input.annotateByKey(constrainedTypePath.path, Annotations.Path)

		pathResolverUtil.resolve(input.constraintTraitNode, pass, environment, graph)

		return PathResolver.Result.Success(constrainedTypePath.path)
	}
}