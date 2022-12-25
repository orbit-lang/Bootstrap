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

class ContainerPathResolver<C: ContainerNode> : IPathResolver<C> {
	override val invocation: Invocation by inject()
	private val pathResolverUtil: PathResolverUtil by inject()

	// First, we resolve the simple name path
	private fun resolveFirstPass(input: ContainerNode, environment: Environment) : IPathResolver.Result {
		val path = OrbitMangler.unmangle(input.identifier.value)

		environment.setCurrentContainerPath(path)

		environment.withScope {
			input.annotateByKey(it.identifier, Annotations.scope, true)
			input.annotateByKey(path, Annotations.path)

			val kind = when (input) {
				is ApiDefNode -> Binding.Kind.Api
				else -> Binding.Kind.Module
			}

			environment.bind(kind, input.identifier.value, path)
		}

		return IPathResolver.Result.Success(path)
	}

	private fun completeBinding(input: ContainerNode, environment: Environment, simplePath: Path, fullyQualifiedPath: FullyQualifiedPath) {
		input.annotateByKey(fullyQualifiedPath, Annotations.path, true)
		environment.unbind(Binding.Kind.Module, input.identifier.value, simplePath)
		environment.bind(Binding.Kind.Module, input.identifier.value, fullyQualifiedPath)
	}

	// Next, we resolve the containers "within" paths
	private fun resolveSecondPass(input: C, environment: Environment, cycles: Int = 0, context: Path? = null) : IPathResolver.Result {
		val simplePath = input.getPath()

		environment.setCurrentContainerPath(simplePath)

		return environment.withScope(input) {
			if (cycles > 5) {
				// This is almost certainly a circular reference
				val message = "Circular reference detected between containers '${simplePath.toString(OrbitMangler)}' and '${context?.toString(OrbitMangler)}'"

				throw invocation.make<ContainerPathResolver<*>>(message, input.identifier.firstToken.position)
			}

			val within = input.within

			if (within == null) {
				// If the container does not declare a parent, it is already fully resolved
				val fullyQualifiedPath = FullyQualifiedPath(simplePath)

				completeBinding(input, environment, simplePath, fullyQualifiedPath)

				return@withScope IPathResolver.Result.Success(fullyQualifiedPath)
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
					IPathResolver.Result.Success(fullyQualifiedPath)
				}

				else -> {
					val parentResult = resolveSecondPass(parentNode as C, environment, cycles + 1, simplePath)

					if (parentResult !is IPathResolver.Result.Success) {
						TODO("@ContainerResolver:91")
					}

					val fullyQualifiedPath = FullyQualifiedPath(parentResult.path + simplePath)

					completeBinding(input, environment, simplePath, fullyQualifiedPath)

					IPathResolver.Result.Success(fullyQualifiedPath)
				}
			}
		}
	}

	private fun <N: INode> resolveAll(resolver: IPathResolver<N>, nodes: List<N>, pass: IPathResolver.Pass) {
		for (node in nodes) {
			resolver.execute(IPathResolver.InputType(node, pass))
		}
	}

	private fun resolveLastPass(input: ContainerNode, environment: Environment, graph: Graph) : IPathResolver.Result {
		val containerPath = input.getPath()

		environment.setCurrentContainerPath(containerPath)

		environment.withScope(input) {
			// TODO - Would be nice to inject these but the parentPath property makes it tricky
			val typeResolver = TypeDefPathResolver(containerPath)
			val traitResolver = TraitDefPathResolver(containerPath)
			val extensionResolver = ExtensionPathResolver(containerPath)
			val familyResolver = FamilyPathResolver(containerPath)
			val contextResolver = ContextPathResolver(containerPath)
			val operatorResolver = OperatorDefPathResolver(containerPath)
			val attributeResolver = AttributeDefPathResolver(containerPath)
			val typeEffectResolver = TypeEffectPathResolver(containerPath)

			val traitDefs = input.entityDefs.filterIsInstance<TraitDefNode>()
			val typeDefs = input.entityDefs.filterIsInstance<TypeDefNode>()
			val extensions = input.search<ExtensionNode>()
			val families = input.search<FamilyNode>()
			val contexts = input.contexts
			val opDefs = input.operatorDefs
			val attributeDefs = input.attributeDefs
			val effects = input.effects

			// Run a first pass over all types & traits that resolves just their own paths
			// (ignoring properties and trait conformance etc)
			// NOTE - We need to do 2 passes over types to avoid order-of-definition problems
			resolveAll(traitResolver, traitDefs, IPathResolver.Pass.Initial)
			resolveAll(typeResolver, typeDefs, IPathResolver.Pass.Initial)
			resolveAll(familyResolver, families, IPathResolver.Pass.Initial)
			resolveAll(typeEffectResolver, effects, IPathResolver.Pass.Initial)
			resolveAll(attributeResolver, attributeDefs, IPathResolver.Pass.Initial)
			resolveAll(familyResolver, families, IPathResolver.Pass.Last)
			resolveAll(contextResolver, contexts, IPathResolver.Pass.Initial)

			if (input is ModuleNode) {
				val typeAliasResolver = TypeAliasPathResolver(containerPath)
				for (typeAlias in input.typeAliasNodes) {
					typeAliasResolver.resolve(typeAlias, IPathResolver.Pass.Initial, environment, graph)
				}
			}

			resolveAll(contextResolver, contexts, IPathResolver.Pass.Last)
			resolveAll(traitResolver, traitDefs, IPathResolver.Pass.Last)
			resolveAll(typeResolver, typeDefs, IPathResolver.Pass.Last)
			resolveAll(operatorResolver, opDefs, IPathResolver.Pass.Last)
			resolveAll(typeEffectResolver, effects, IPathResolver.Pass.Last)
			resolveAll(attributeResolver, attributeDefs, IPathResolver.Pass.Last)

			if (input is ModuleNode) {
				for (typeProjection in input.projections) {
					typeProjection.annotate(input.getGraphID(), Annotations.graphId)
					typeProjection.typeIdentifier.annotate(input.getGraphID(), Annotations.graphId)
					ProjectionPathResolver.resolve(typeProjection, IPathResolver.Pass.Initial, environment, graph)
				}
			}

			for (methodDef in input.methodDefs) {
				methodDef.annotateByKey(input.getGraphID(), Annotations.graphId)
				pathResolverUtil.resolve(methodDef, IPathResolver.Pass.Initial, environment, graph)
			}

			resolveAll(extensionResolver, extensions, IPathResolver.Pass.Initial)
		}

		return IPathResolver.Result.Success(containerPath)
	}

	override fun resolve(input: C, pass: IPathResolver.Pass, environment: Environment, graph: Graph) : IPathResolver.Result {
		return when (pass) {
			is IPathResolver.Pass.Initial -> resolveFirstPass(input, environment)
			is IPathResolver.Pass.Subsequent -> resolveSecondPass(input, environment)
			is IPathResolver.Pass.Last -> resolveLastPass(input, environment, graph)
		}
	}
}
