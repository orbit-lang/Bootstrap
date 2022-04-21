package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.injectResult
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.typeresolvers.MethodSignatureTypeResolver
import org.orbit.util.Printer

class CreateEntityConstructorStub<N: EntityConstructorNode, C: EntityConstructor>(override val node: N, override val constructor: (N) -> C) : CreateStub<N, C>

class CreateTypeConstructorStub(
    override val node: TypeConstructorNode
) : CreateStub<TypeConstructorNode, TypeConstructor> {
    override val constructor: (TypeConstructorNode) -> TypeConstructor = { TypeConstructor(node = it) }
}

class CreateTraitConstructorStub(
    override val node: TraitConstructorNode
) : CreateStub<TraitConstructorNode, TraitConstructor> {
    override val constructor: (TraitConstructorNode) -> TraitConstructor = {
        val typeParameters = it.typeParameterNodes
            .map { tp -> tp.getPath() }
            .map { path -> TypeParameter(path)  }

        val tc = TraitConstructor(node = it, typeParameters)

//        it.annotate(tc, Annotations.Type)

        tc
    }
}

class ResolveTraitConstructorSignatures(private val node: TraitConstructorNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)

    override fun execute(context: Context) = context.withSubContext { ctx ->
        val traitConstructor = node.getType() as TraitConstructor

        ctx.add(SelfType)
        traitConstructor.typeParameters.forEach { ctx.add(it) }

        val signatures = node.signatureNodes
            .map {
                MethodSignatureTypeResolver(it, Binding.Self, null)
                    .resolve(nameResolverResult.environment, ctx)
            }

        val nTraitConstructor = TraitConstructor(traitConstructor.name, traitConstructor.typeParameters, traitConstructor.properties, traitConstructor.partiallyResolvedTraitConstructors, signatures)

//        node.annotate(nTraitConstructor, Annotations.Type, mergeOnConflict = true)

        context.add(nTraitConstructor)
    }

    override fun describe(printer: Printer): String {
        return ""
    }
}
