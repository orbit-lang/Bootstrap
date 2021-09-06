package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.EntityConstructorWhereClauseNode
import org.orbit.core.nodes.TypeConstraintNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation
import org.orbit.util.dispose
import org.orbit.util.partial

object TypeConstraintPathResolver : PathResolver<TypeConstraintNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeConstraintNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val constrainedTypePath = environment.getBinding(input.constrainedTypeNode.value, Binding.Kind.Type).unwrap(this, input.constrainedTypeNode.firstToken.position)
		val constraintTraitPath = environment.getBinding(input.constraintTraitNode.value, Binding.Kind.Trait).unwrap(this, input.constraintTraitNode.firstToken.position)

		input.constrainedTypeNode.annotate(constrainedTypePath.path, Annotations.Path)
		input.constraintTraitNode.annotate(constraintTraitPath.path, Annotations.Path)
		input.annotate(constrainedTypePath.path, Annotations.Path)

		return PathResolver.Result.Success(constrainedTypePath.path)
	}
}

object EntityConstructorWhereClausePathResolver : PathResolver<EntityConstructorWhereClauseNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: EntityConstructorWhereClauseNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input.statementNode) {
		is TypeConstraintNode -> TypeConstraintPathResolver.resolve(input.statementNode, pass, environment, graph)
		else -> TODO("EntityConstructorWhereClausePathResolver")
	}
}

class TypeConstructorPathResolver(
	private val parentPath: Path
) : PathResolver<TypeConstructorNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeConstructorNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val path = parentPath + Path(input.typeIdentifierNode.value)

		if (pass == PathResolver.Pass.Initial) {
			input.annotate(path, Annotations.Path)
			input.typeIdentifierNode.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.TypeConstructor, input.typeIdentifierNode.value, path)

			val parentGraphID = graph.find(parentPath.toString(OrbitMangler))
			val graphID = graph.insert(input.typeIdentifierNode.value)

			graph.link(parentGraphID, graphID)

			for (typeParameter in input.typeParameterNodes) {
				val nPath = path + typeParameter.value

				typeParameter.annotate(nPath, Annotations.Path)
				// TODO - Recursively resolve nested type parameters
				environment.bind(Binding.Kind.Type, typeParameter.value, nPath)
			}
		} else {
			input.properties.forEach(dispose(partial(pathResolverUtil::resolve, pass, environment, graph)))
			input.clauses.forEach(dispose(partial(EntityConstructorWhereClausePathResolver::resolve, pass, environment, graph)))
		}

		return PathResolver.Result.Success(path)
	}
}