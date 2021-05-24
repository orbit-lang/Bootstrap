package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.ContainerNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.annotate
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ContainerResolver<C: ContainerNode> : PathResolver<C> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	// First, we resolve the simple name path
	private fun resolveFirstPass(input: ContainerNode) : PathResolver.Result {
		val environment = inject<Environment>().value
		val path = OrbitMangler.unmangle(input.identifier.value)

		input.annotate(path, Annotations.Path)

		environment.bind(Binding.Kind.Module, input.identifier.value, path)

		return PathResolver.Result.Success(path)
	}

	private fun completeBinding(input: ContainerNode, simplePath: Path, fullyQualifiedPath: FullyQualifiedPath) {
		val environment = inject<Environment>().value

		input.annotate(fullyQualifiedPath, Annotations.Path, true)
		environment.unbind(Binding.Kind.Module, input.identifier.value, simplePath)
		environment.bind(Binding.Kind.Module, input.identifier.value, fullyQualifiedPath)
	}

	// Next, we resolve the containers "within" paths
	private fun resolveSecondPass(input: C, cycles: Int = 0, context: Path? = null) : PathResolver.Result {
		val environment = inject<Environment>().value

		return environment.withNewScope(input) {
			val simplePath = input.getPath()

			if (cycles > 5) {
				// This is almost certainly a circular reference
				val message =
					"Circular reference detected between containers '${simplePath.toString(OrbitMangler)}' and '${
						context?.toString(OrbitMangler)
					}'"

				throw invocation.make<ContainerResolver<*>>(message, input.identifier.firstToken.position)
			}

			val within = input.within

			if (within == null) {
				// If the container does not declare a parent, it is already fully resolved
				val fullyQualifiedPath = FullyQualifiedPath(simplePath)

				completeBinding(input, simplePath, fullyQualifiedPath)

				return@withNewScope PathResolver.Result.Success(fullyQualifiedPath)
			}

			val parent = environment.searchAllScopes {
				(it.simpleName == within.value || it.path.toString(OrbitMangler) == within.value)
						&& it.kind == Binding.Kind.Module
			}.unwrap(this, within.firstToken.position)

			val parentNode = environment.ast.search(ContainerNode::class.java)
				.find { it.getPathOrNull() == parent.path }!!

			input.within?.annotate(parent.path, Annotations.Path)

			return@withNewScope when (parent.path) {
				is FullyQualifiedPath -> {
					// TODO - This check shouldn't be necessary. Something is wrong with this algo
					val fullyQualifiedPath = when (simplePath.containsSubPath(parent.path)) {
						true -> FullyQualifiedPath(simplePath)
						else -> FullyQualifiedPath(parent.path + simplePath)
					}

					completeBinding(input, simplePath, fullyQualifiedPath)
					PathResolver.Result.Success(fullyQualifiedPath)
				}

				else -> {
					val parentResult = resolveSecondPass(parentNode as C, cycles + 1, simplePath)

					if (parentResult !is PathResolver.Result.Success) {
						TODO("@ContainerResolver:91")
					}

					val fullyQualifiedPath = FullyQualifiedPath(parentResult.path + simplePath)

					completeBinding(input, simplePath, fullyQualifiedPath)

					PathResolver.Result.Success(fullyQualifiedPath)
				}
			}
		}
	}

	private fun resolveLastPass(input: ContainerNode, environment: Environment, graph: Graph) : PathResolver.Result {
		val containerPath = input.getPath()

		// TODO - Would be nice to inject these but the parentPath property makes it tricky
		val typeResolver = TypeDefPathResolver(containerPath)
		val traitResolver = TraitDefPathResolver(containerPath)

		val traitDefs = input.entityDefs.filterIsInstance<TraitDefNode>()
		val typeDefs = input.entityDefs.filterIsInstance<TypeDefNode>()

		// Run a first pass over all types & traits that resolves just their own paths
		// (ignoring properties and trait conformance etc)
		for (traitDef in traitDefs) {
			traitResolver.execute(PathResolver.InputType(traitDef, PathResolver.Pass.Initial))
		}

		// We need to do 2 passes over types to avoid order-of-definition problems
		for (typeDef in typeDefs) {
			typeResolver.execute(PathResolver.InputType(typeDef, PathResolver.Pass.Initial))
		}

		for (traitDef in traitDefs) {
			traitResolver.execute(PathResolver.InputType(traitDef, PathResolver.Pass.Last))
		}

		for (typeDef in typeDefs) {
			typeResolver.execute(PathResolver.InputType(typeDef, PathResolver.Pass.Last))
		}

		for (methodDef in input.methodDefs) {
			pathResolverUtil.resolve(methodDef, PathResolver.Pass.Initial, environment, graph)
		}

		return PathResolver.Result.Success(containerPath)
	}

	override fun resolve(input: C, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return when (pass) {
			is PathResolver.Pass.Initial -> resolveFirstPass(input)
			is PathResolver.Pass.Subsequent -> resolveSecondPass(input)
			is PathResolver.Pass.Last -> resolveLastPass(input, environment, graph)
		}
	}
}