package org.orbit.graph

import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.SourcePosition
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation

class MethodSignaturePathResolver : PathResolver<MethodSignatureNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MethodSignatureNode, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		// A method's canonical path is `<ReceiverType>::<MethodName>[::ArgType1, ::ArgType2, ...]::<ReturnType>`
		val receiver = input.receiverTypeNode.typeIdentifierNode.value
		val receiverResult = environment.getBinding(receiver, Binding.Kind.Type)
		val receiverBinding = receiverResult
			.unwrap(this, input.receiverTypeNode.typeIdentifierNode.firstToken.position)

		input.receiverTypeNode.annotate(receiverBinding.path, Annotations.Path)

		val name = input.identifierNode.identifier
		val ret = input.returnTypeNode?.value ?: IntrinsicTypes.Unit.type.name
		val retResult = environment.getBinding(ret, Binding.Kind.Type)
		val retPath = retResult.unwrap(this, input.returnTypeNode?.firstToken?.position ?: SourcePosition.unknown)
		// TODO - Should method names contain parameter names as well as/instead of types?
		// i.e. Are parameter names important/overloadable?

		input.returnTypeNode?.annotate(retPath.path, Annotations.Path)

		val argPaths = input.parameterNodes.mapIndexed { idx, it ->
			val result = environment.getBinding(it.typeIdentifierNode.value, Binding.Kind.Type)
			val binding = result.unwrap(this, it.typeIdentifierNode.firstToken.position)

			input.annotateParameter(idx, binding.path, Annotations.Path)

			binding.path
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

		val receiverID = graph.find(receiverBinding.path.toString(OrbitMangler))
		val vertexID = graph.insert(methodName)

		graph.link(receiverID, vertexID)

		return PathResolver.Result.Success(path)
	}
}