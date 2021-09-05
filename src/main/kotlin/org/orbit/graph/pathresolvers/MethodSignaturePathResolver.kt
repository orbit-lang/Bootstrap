package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation

class MethodSignaturePathResolver : PathResolver<MethodSignatureNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MethodSignatureNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		// A method's canonical path is `<ReceiverType>::<MethodName>[::ArgType1, ::ArgType2, ...]::<ReturnType>`
		val receiver = input.receiverTypeNode.typeExpressionNode.value

		TypeExpressionPathResolver.resolve(input.receiverTypeNode.typeExpressionNode, pass, environment, graph)
			.asSuccess()

		val receiverBinding = environment.getBinding(receiver, Binding.Kind.Union.receiver)
			.unwrap(this, input.receiverTypeNode.typeExpressionNode.firstToken.position)

		input.receiverTypeNode.annotate(receiverBinding.path, Annotations.Path)
		input.receiverTypeNode.typeExpressionNode.annotate(receiverBinding.path, Annotations.Path)

		val name = input.identifierNode.identifier
		val ret = input.returnTypeNode?.value ?: IntrinsicTypes.Unit.type.name

		if (input.returnTypeNode?.value != null) {
			TypeExpressionPathResolver.resolve(input.returnTypeNode, pass, environment, graph)
		}

		val retResult = environment.getBinding(ret, Binding.Kind.Union.entityMethodOrConstructor)
		val retPath = retResult.unwrap(this, input.returnTypeNode?.firstToken?.position ?: SourcePosition.unknown)
		// TODO - Should method names contain parameter names as well as/instead of types?
		// i.e. Are parameter names important/overloadable?

		input.returnTypeNode?.annotate(retPath.path, Annotations.Path)

		val argPaths = input.parameterNodes.mapIndexed { idx, it ->
			val result = TypeExpressionPathResolver.resolve(it.typeExpressionNode, pass, environment, graph)
				.asSuccess()

//			val result = environment.getBinding(it.typeExpressionNode.value, Binding.Kind.Union.entityMethodOrConstructor)
//			val binding = result.unwrap(this, it.typeExpressionNode.firstToken.position)

			input.annotateParameter(idx, result.path, Annotations.Path)
//			it.annotate(binding.path, Annotations.Path)
//			it.typeExpressionNode.annotate(binding.path, Annotations.Path)

			result.path
		}

		var path = receiverBinding.path + Path(name)

		for (arg in argPaths) {
			path += arg
		}

		path += retPath.path

		input.annotate(path, Annotations.Path)
		environment.bind(Binding.Kind.Method, name, path)

		val methodName = path.relativeNames
			.slice(IntRange(path.relativeNames.indexOf(name), path.relativeNames.size - 1))
			.joinToString("::")

		return PathResolver.Result.Success(path)
	}
}