package org.orbit.graph

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.SourcePosition
import org.orbit.core.getPath
import org.orbit.core.nodes.ContainerNode
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

	override fun resolve(input: ContainerNode, pass: PathResolver.Pass) : PathResolver.Result {
		if (pass == PathResolver.Pass.Last) {
			val path = input.getPath()

			val typeResolver = TypeDefPathResolver(invocation, environment, graph, path)
			val traitResolver = TraitDefPathResolver(invocation, environment, graph, path)

			for (traitDef in input.traitDefs) {
				traitResolver.execute(PathResolver.InputType(traitDef, pass))
			}

			for (typeDef in input.typeDefs) {
				typeResolver.execute(PathResolver.InputType(typeDef, pass))
			}

			val methodDefs = input.methodDefs
			val methodSignatureResolver = MethodSignaturePathResolver(invocation, environment, graph)

			methodDefs.forEach {
				methodSignatureResolver.execute(PathResolver.InputType(it.signature, pass))
			}

			return PathResolver.Result.Success(path)
		} else {
			val path = OrbitMangler.unmangle(input.identifier.value)

			input.annotate(path, Annotations.Path)

			environment.bind(Binding.Kind.Api, input.identifier.value, path)

			return PathResolver.Result.Success(path)
		}
	}
}