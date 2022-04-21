package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation
import org.orbit.util.dispose
import org.orbit.util.partial

class MethodSignaturePathResolver : PathResolver<MethodSignatureNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MethodSignatureNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		// We need to resolve any type parameters before we can look at the rest of the signature
		val graphID = input.getGraphID()
		val tp = input.typeParameters
		val receiver = input.receiverTypeNode.value

		if (tp != null) {
			val mPath = Path(input.identifierNode.identifier)
			tp.typeParameters.forEach {
				val nPath = mPath + it.value
				val nGraphID = graph.insert(nPath.toString(OrbitMangler))

				it.annotate(nPath, Annotations.Path)
				// TODO - Recursively resolve nested type parameters
				environment.bind(Binding.Kind.Type, it.value, nPath, nGraphID)
				graph.link(graphID, nGraphID)
			}
		}

		val receiverBinding = environment.getBinding(receiver, Binding.Kind.Union.receiver, graph, graphID)
			.unwrap(this, input.receiverTypeNode.firstToken.position)

		input.receiverTypeNode.annotate(graphID, Annotations.GraphID)

		TypeExpressionPathResolver.resolve(input.receiverTypeNode, pass, environment, graph)
			.asSuccess()

		input.receiverTypeNode.annotate(receiverBinding.path, Annotations.Path)

		val name = input.identifierNode.identifier
		val ret = input.returnTypeNode?.value ?: IntrinsicTypes.Unit.type.name

		if (input.returnTypeNode?.value != null) {
			try {
				input.returnTypeNode.annotate(graphID, Annotations.GraphID)
				TypeExpressionPathResolver.resolve(input.returnTypeNode, pass, environment, graph)
			} catch (e: Exception) {
				println("HERE")
			}
		}

		val retResult = environment.getBinding(ret, Binding.Kind.Union.entityMethodOrConstructor, graph, input.getGraphIDOrNull())
		val retPath = retResult.unwrap(this, input.returnTypeNode?.firstToken?.position ?: SourcePosition.unknown)
		// TODO - Should method names contain parameter names as well as/instead of types?
		// i.e. Are parameter names important/overloadable?

		input.returnTypeNode?.annotate(retPath.path, Annotations.Path)

		val argPaths = input.parameterNodes.mapIndexed { idx, it ->
			val result = TypeExpressionPathResolver.resolve(it.typeExpressionNode, pass, environment, graph)
				.asSuccess()

			input.annotateParameter(idx, result.path, Annotations.Path)

			result.path
		}

		var path = receiverBinding.path + Path(name)

		for (arg in argPaths) {
			path += arg
		}

		path += retPath.path

		input.annotate(path, Annotations.Path)
		environment.bind(Binding.Kind.Method, name, path)

		input.typeConstraints.forEach(dispose(partial(TypeConstraintWhereClausePathResolver::resolve, pass, environment, graph)))

		return PathResolver.Result.Success(path)
	}
}