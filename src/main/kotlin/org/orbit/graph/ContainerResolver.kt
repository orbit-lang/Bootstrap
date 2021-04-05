package org.orbit.graph

import org.orbit.core.*
import org.orbit.core.nodes.ContainerNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.util.Fatal
import org.orbit.util.Invocation

class ContainerResolver(
    override val invocation: Invocation,
    override val environment: Environment,
	override val graph: Graph
) : PathResolver<ContainerNode> {
	private data class RecursiveDependency(
		override val sourcePosition: SourcePosition,
		val path: Path,
		override val phaseClazz: Class<out ContainerResolver> = ContainerResolver::class.java
	) : Fatal<ContainerResolver> {
		override val message: String
			get() = "Container has a recursive 'within' clause: '${path.toString(OrbitMangler)}'"
	}

	// First, we resolve the simple name path
	private fun resolveFirstPass(input: ContainerNode) : PathResolver.Result {
		val path = OrbitMangler.unmangle(input.identifier.value)
		input.annotate(path, Annotations.Path)

		environment.bind(Binding.Kind.Module, input.identifier.value, path)

		return PathResolver.Result.Success(path)
	}

	private fun completeBinding(input: ContainerNode, simplePath: Path, fullyQualifiedPath: FullyQualifiedPath) {
		input.annotate(fullyQualifiedPath, Annotations.Path, true)
		environment.unbind(Binding.Kind.Module, input.identifier.value, simplePath)
		environment.bind(Binding.Kind.Module, input.identifier.value, fullyQualifiedPath)
	}

	// Next, we resolve the containers "within" paths where the
	private fun resolveSecondPass(input: ContainerNode, cycles: Int = 0, context: Path? = null) : PathResolver.Result {
		val simplePath = input.getPath()

		if (cycles > 5) {
			// This is almost certainly a circular reference
			var message = "Circular reference detected between containers '${simplePath.toString(OrbitMangler)}' and '${context?.toString(OrbitMangler)}'"

			throw invocation.make<ContainerResolver>(message, input.identifier.firstToken.position)
		}

		val within = input.within

		if (within == null) {
			// If the container does not declare a parent, it is already fully resolved
			val fullyQualifiedPath = FullyQualifiedPath(simplePath)

			completeBinding(input, simplePath, fullyQualifiedPath)

			return PathResolver.Result.Success(fullyQualifiedPath)
		}

		val parent = environment.getBinding(within.value, Binding.Kind.Module)
			.unwrap(this, within.firstToken.position)

		val parentNode = environment.ast.search(ContainerNode::class.java)
			.find { it.getPathOrNull() == parent.path }!!

		return when (parent.path) {
			is FullyQualifiedPath -> {
				val fullyQualifiedPath = FullyQualifiedPath(simplePath)
				completeBinding(input, simplePath, fullyQualifiedPath)

				PathResolver.Result.Success(fullyQualifiedPath)
			}

			else -> {
				val parentResult = resolveSecondPass(parentNode, cycles + 1, simplePath)

				if (parentResult !is PathResolver.Result.Success) {
					TODO()
				}

				val fullyQualifiedPath = FullyQualifiedPath(parentResult.path + simplePath)

				completeBinding(input, simplePath, fullyQualifiedPath)

				PathResolver.Result.Success(fullyQualifiedPath)
			}
		}
	}

	private fun resolveLastPass(input: ContainerNode) : PathResolver.Result {
		val containerPath = input.getPath()
		val typeResolver = TypeDefPathResolver(invocation, environment, graph, containerPath)
		val traitResolver = TraitDefPathResolver(invocation, environment, graph, containerPath)

		val traitDefs = input.entityDefs.filterIsInstance<TraitDefNode>()
		val typeDefs = input.entityDefs.filterIsInstance<TypeDefNode>()

		// Run a first pass over all types & traits that resolves just their own paths
		// (ignoring properties and conformances etc)
		for (traitDef in traitDefs) {
			traitResolver.execute(PathResolver.InputType(traitDef, PathResolver.Pass.Initial))
		}

		for (typeDef in typeDefs) {
			typeResolver.execute(PathResolver.InputType(typeDef, PathResolver.Pass.Initial))
		}

		return PathResolver.Result.Success(containerPath)
	}

	override fun resolve(input: ContainerNode, pass: PathResolver.Pass) : PathResolver.Result {
		return when (pass) {
			is PathResolver.Pass.Initial -> resolveFirstPass(input)
			is PathResolver.Pass.Subsequent -> resolveSecondPass(input)
			is PathResolver.Pass.Last -> resolveLastPass(input)
		}
//		val path = when (pass) {
//			is PathResolver.Pass.Initial -> OrbitMangler.unmangle(input.identifier.value)
//			is PathResolver.Pass.Subsequent -> input.getPath()
//		}
//
//		input.annotate(path, Annotations.Path)
//
//		environment.bind(Binding.Kind.Module, input.identifier.value, path)
//
//		if (pass == PathResolver.Pass.Last) {
//			val path = input.getPath()
//
//			val typeResolver = TypeDefPathResolver(invocation, environment, graph, path)
//			val traitResolver = TraitDefPathResolver(invocation, environment, graph, path)
//
//			val traitDefs = input.entityDefs.filterIsInstance<TraitDefNode>()
//			val typeDefs= input.entityDefs.filterIsInstance<TypeDefNode>()
//
//			for (traitDef in traitDefs) {
//				traitResolver.execute(PathResolver.InputType(traitDef, pass))
//			}
//
//			for (typeDef in typeDefs) {
//				typeResolver.execute(PathResolver.InputType(typeDef, pass))
//			}
//
////			val methodDefs = input.methodDefs
////			val methodSignatureResolver = MethodSignaturePathResolver(invocation, environment, graph)
////
////			methodDefs.forEach {
////				methodSignatureResolver.execute(PathResolver.InputType(it.signature, pass))
////			}
//
//			return PathResolver.Result.Success(path)
//		} else {
//			val path = OrbitMangler.unmangle(input.identifier.value)
//
//			input.annotate(path, Annotations.Path)
//
//			environment.bind(Binding.Kind.Module, input.identifier.value, path)
//
//			val methodDefs = input.methodDefs
//			val methodSignatureResolver = MethodSignaturePathResolver(invocation, environment, graph)
//
//			methodDefs.forEach {
//				methodSignatureResolver.execute(PathResolver.InputType(it.signature, pass))
//			}
//
//			return PathResolver.Result.Success(path)
//		}
	}
}