package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.annotateByKey
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.extensions.getGraphIDOrNull
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation
import org.orbit.util.dispose
import org.orbit.util.partial

class MethodSignaturePathResolver : IPathResolver<MethodSignatureNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: MethodSignatureNode, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		// We need to resolve any type parameters before we can look at the rest of the signature
		val graphID = input.getGraphID()
		val tp = input.typeParameters
		val receiver = input.receiverTypeNode.value

		if (tp != null) {
			val mPath = Path(input.identifierNode.identifier)
			tp.typeParameters.forEachIndexed { idx, tp ->
				val nPath = mPath + tp.value
				val nGraphID = graph.insert(nPath.toString(OrbitMangler))

				tp.annotateByKey(idx, Annotations.index)
				tp.annotateByKey(nPath, Annotations.path)
				// TODO - Recursively resolve nested type parameters
				environment.bind(Binding.Kind.Type, tp.value, nPath, nGraphID)
				graph.link(graphID, nGraphID)
			}
		}

		val receiverBinding = environment.getBinding(receiver, Binding.Kind.Union.entityOrMethod, graph, graphID)
			.unwrap(this, input.receiverTypeNode.firstToken.position)

		input.receiverTypeNode.annotateByKey(graphID, Annotations.graphId)

		TypeExpressionPathResolver.resolve(input.receiverTypeNode, pass, environment, graph)
			.asSuccess()

		input.receiverTypeNode.annotateByKey(receiverBinding.path, Annotations.path)

		val name = input.identifierNode.identifier
		val ret = input.returnTypeNode?.value ?: "Orb::Core::Types::Unit"

		if (input.returnTypeNode?.value != null) {
			try {
				input.returnTypeNode.annotateByKey(graphID, Annotations.graphId)
				TypeExpressionPathResolver.resolve(input.returnTypeNode, pass, environment, graph)
			} catch (e: Exception) {}
		}

		val retResult = environment.getBinding(ret, Binding.Kind.Union.entityOrMethod, graph, input.getGraphIDOrNull())
		val retPath = retResult.unwrap(this, input.returnTypeNode?.firstToken?.position ?: SourcePosition.unknown)
		// TODO - Should method names contain parameter names as well as/instead of types?
		// i.e. Are parameter names important/overloadable?

		pathResolverUtil.resolveAll(input.effects, pass, environment, graph)

		input.returnTypeNode?.annotateByKey(retPath.path, Annotations.path)

		val argPaths = input.parameterNodes.mapIndexed { idx, it ->
			it.typeExpressionNode.annotateByKey(graphID, Annotations.graphId)
			val result = TypeExpressionPathResolver.resolve(it.typeExpressionNode, pass, environment, graph)
				.asSuccess()

			input.annotateParameter(idx, result.path, Annotations.path)

			result.path
		}

		var path = receiverBinding.path + Path(name)

		for (arg in argPaths) {
			path += arg
		}

		path += retPath.path

		input.annotateByKey(path, Annotations.path)
		environment.bind(Binding.Kind.Method, name, path)

		input.typeConstraints.forEach(dispose(partial(TypeConstraintWhereClausePathResolver::resolve, pass, environment, graph)))

		return IPathResolver.Result.Success(path)
	}
}