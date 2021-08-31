package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.*
import org.orbit.graph.components.*
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.types.components.IntrinsicTypes
import org.orbit.util.Invocation

object TypeProjectionPathResolver : PathResolver<TypeProjectionNode> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	override fun resolve(input: TypeProjectionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val typeResult = TypeExpressionPathResolver.resolve(input.typeIdentifier, pass, environment, graph)
			.asSuccess()

		val traitResult = TypeExpressionPathResolver.resolve(input.traitIdentifier, pass, environment, graph)
			.asSuccess()

		input.typeIdentifier.annotate(typeResult.path, Annotations.Path)
		input.traitIdentifier.annotate(traitResult.path, Annotations.Path)

		input.annotate(typeResult.path, Annotations.Path)

		// TODO - Resolve where clauses
		input.whereNodes
			.forEach { pathResolverUtil.resolve(it.whereStatement, pass, environment, graph) }

		return typeResult
	}
}

object MetaTypePathResolver : PathResolver<MetaTypeNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: MetaTypeNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result {
		val typeConstructorResult = TypeExpressionPathResolver.resolve(input.typeConstructorIdentifier,
			pass, environment, graph).asSuccess()

		input.typeParameters.forEach { TypeExpressionPathResolver.resolve(it, pass, environment, graph) }

		input.annotate(typeConstructorResult.path, Annotations.Path)
		input.typeConstructorIdentifier.annotate(typeConstructorResult.path, Annotations.Path)

		return PathResolver.Result.Success(typeConstructorResult.path)
	}
}

object TypeExpressionPathResolver : PathResolver<TypeExpressionNode> {
	override val invocation: Invocation by inject()

	override fun resolve(input: TypeExpressionNode, pass: PathResolver.Pass, environment: Environment, graph: Graph): PathResolver.Result = when (input) {
		is TypeIdentifierNode -> {
			val binding = environment.getBinding(input.value, Binding.Kind.Union.entityOrConstructor)
				.unwrap(this, input.firstToken.position)

			input.annotate(binding.path, Annotations.Path)

			PathResolver.Result.Success(binding.path)
		}

		is MetaTypeNode -> MetaTypePathResolver.resolve(input, pass, environment, graph)

		else -> TODO("???")
	}
}

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
			val result = environment.getBinding(it.typeExpressionNode.value, Binding.Kind.Union.entityOrMethod)
			val binding = result.unwrap(this, it.typeExpressionNode.firstToken.position)

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

		//val receiverID = graph.find(receiverBinding)
		val vertexID = graph.insert(methodName)

		//graph.link(receiverID, vertexID)

		return PathResolver.Result.Success(path)
	}
}