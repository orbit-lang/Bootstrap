package org.orbit.graph.pathresolvers

import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.core.nodes.Annotations
import org.orbit.frontend.extensions.annotate
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.util.Invocation

class ContainerPathResolver<C: ContainerNode> : PathResolver<C> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	// First, we resolve the simple name path
	private fun resolveFirstPass(input: ContainerNode, environment: Environment) : PathResolver.Result {
		val path = OrbitMangler.unmangle(input.identifier.value)

		environment.withScope {
			input.annotateByKey(it.identifier, Annotations.scope, true)
			input.annotateByKey(path, Annotations.path)

			val kind = when (input) {
				is ApiDefNode -> Binding.Kind.Api
				else -> Binding.Kind.Module
			}

			environment.bind(kind, input.identifier.value, path)
		}

		return PathResolver.Result.Success(path)
	}

	private fun completeBinding(input: ContainerNode, environment: Environment, simplePath: Path, fullyQualifiedPath: FullyQualifiedPath) {
		input.annotateByKey(fullyQualifiedPath, Annotations.path, true)
		environment.unbind(Binding.Kind.Module, input.identifier.value, simplePath)
		environment.bind(Binding.Kind.Module, input.identifier.value, fullyQualifiedPath)
	}

	// Next, we resolve the containers "within" paths
	private fun resolveSecondPass(input: C, environment: Environment, cycles: Int = 0, context: Path? = null) : PathResolver.Result {
		return environment.withScope(input) {
			val simplePath = input.getPath()

			if (cycles > 5) {
				// This is almost certainly a circular reference
				val message =
					"Circular reference detected between containers '${simplePath.toString(OrbitMangler)}' and '${
						context?.toString(OrbitMangler)
					}'"

				throw invocation.make<ContainerPathResolver<*>>(message, input.identifier.firstToken.position)
			}

			val within = input.within

			if (within == null) {
				// If the container does not declare a parent, it is already fully resolved
				val fullyQualifiedPath = FullyQualifiedPath(simplePath)

				completeBinding(input, environment, simplePath, fullyQualifiedPath)

				return@withScope PathResolver.Result.Success(fullyQualifiedPath)
			}

			val parent = environment.searchAllScopes {
				(it.simpleName == within.value || it.path.toString(OrbitMangler) == within.value)
						&& it.kind == Binding.Kind.Module
			}.unwrap(this, within.firstToken.position)

			val parentNode = environment.ast.search(ContainerNode::class.java)
				.find { it.getPathOrNull() == parent.path }!!

			input.within?.annotateByKey(parent.path, Annotations.path)

			return@withScope when (parent.path) {
				is FullyQualifiedPath -> {
					// TODO - This check shouldn't be necessary. Something is wrong with this algo
					val fullyQualifiedPath = when (simplePath.containsSubPath(parent.path)) {
						true -> FullyQualifiedPath(simplePath)
						else -> FullyQualifiedPath(parent.path + simplePath)
					}

					completeBinding(input, environment, simplePath, fullyQualifiedPath)
					PathResolver.Result.Success(fullyQualifiedPath)
				}

				else -> {
					val parentResult = resolveSecondPass(parentNode as C, environment, cycles + 1, simplePath)

					if (parentResult !is PathResolver.Result.Success) {
						TODO("@ContainerResolver:91")
					}

					val fullyQualifiedPath = FullyQualifiedPath(parentResult.path + simplePath)

					completeBinding(input, environment, simplePath, fullyQualifiedPath)

					PathResolver.Result.Success(fullyQualifiedPath)
				}
			}
		}
	}

	private fun <N: INode> resolveAll(resolver: PathResolver<N>, nodes: List<N>, pass: PathResolver.Pass) {
		for (node in nodes) {
			resolver.execute(PathResolver.InputType(node, pass))
		}
	}

	private fun resolveLastPass(input: ContainerNode, environment: Environment, graph: Graph) : PathResolver.Result {
		val containerPath = input.getPath()

		environment.withScope(input) {
			it.setContainerPath(containerPath)

			// TODO - Would be nice to inject these but the parentPath property makes it tricky
			val typeResolver = TypeDefPathResolver(containerPath)
			val traitResolver = TraitDefPathResolver(containerPath)
			val extensionResolver = ExtensionPathResolver(containerPath)
			val familyResolver = FamilyPathResolver(containerPath)
			val contextResolver = ContextPathResolver(containerPath)
			val operatorResolver = OperatorDefPathResolver(containerPath)

			val traitDefs = input.entityDefs.filterIsInstance<TraitDefNode>()
			val typeDefs = input.entityDefs.filterIsInstance<TypeDefNode>()
			val extensions = input.search<ExtensionNode>()
			val families = input.search<FamilyNode>()
			val contexts = input.contexts
			val opDefs = input.operatorDefs

			// Run a first pass over all types & traits that resolves just their own paths
			// (ignoring properties and trait conformance etc)
			// NOTE - We need to do 2 passes over types to avoid order-of-definition problems
			resolveAll(traitResolver, traitDefs, PathResolver.Pass.Initial)
			resolveAll(typeResolver, typeDefs, PathResolver.Pass.Initial)
			resolveAll(familyResolver, families, PathResolver.Pass.Initial)
			resolveAll(familyResolver, families, PathResolver.Pass.Last)
			resolveAll(contextResolver, contexts, PathResolver.Pass.Initial)

			if (input is ModuleNode) {
				val typeAliasResolver = TypeAliasPathResolver(containerPath)
				for (typeAlias in input.typeAliasNodes) {
					typeAliasResolver.resolve(typeAlias, PathResolver.Pass.Initial, environment, graph)
				}
			}

			resolveAll(contextResolver, contexts, PathResolver.Pass.Last)
			resolveAll(traitResolver, traitDefs, PathResolver.Pass.Last)
			resolveAll(typeResolver, typeDefs, PathResolver.Pass.Last)
			resolveAll(operatorResolver, opDefs, PathResolver.Pass.Last)

			if (input is ModuleNode) {
				for (typeProjection in input.projections) {
					ProjectionPathResolver.resolve(typeProjection, PathResolver.Pass.Initial, environment, graph)
				}
			}

			for (methodDef in input.methodDefs) {
				methodDef.annotateByKey(input.getGraphID(), Annotations.graphId)
				pathResolverUtil.resolve(methodDef, PathResolver.Pass.Initial, environment, graph)
			}

			resolveAll(extensionResolver, extensions, PathResolver.Pass.Initial)
		}

		return PathResolver.Result.Success(containerPath)
	}

	override fun resolve(input: C, pass: PathResolver.Pass, environment: Environment, graph: Graph) : PathResolver.Result {
		return when (pass) {
			is PathResolver.Pass.Initial -> resolveFirstPass(input, environment)
			is PathResolver.Pass.Subsequent -> resolveSecondPass(input, environment)
			is PathResolver.Pass.Last -> resolveLastPass(input, environment, graph)
		}
	}
}
