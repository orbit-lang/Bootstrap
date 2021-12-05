package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation

data class MetaType(val entityConstructor: EntityConstructor, val concreteTypeParameters: List<ValuePositionType>, val properties: List<Property>, val traitConformance: List<Trait> = emptyList()) : ValuePositionType, TypeExpression {
    companion object : KoinComponent {
        val invocation: Invocation by inject()
    }

    override val isEphemeral: Boolean = true
    override val kind: TypeKind = NullaryType

    override val name: String
        get() = entityConstructor.name

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol>
        get() = entityConstructor.equalitySemantics

    override fun evaluate(context: ContextProtocol): TypeProtocol {
        val typeParams = concreteTypeParameters.toMutableList()
        val cCount = concreteTypeParameters.count()
        val eCount = entityConstructor.typeParameters.count()

        if (cCount > eCount) {
            throw invocation.make<TypeSystem>("Type constructor ${entityConstructor.name} expects ${entityConstructor.typeParameters.count()} type parameters, found ${concreteTypeParameters.count()}",
                SourcePosition.unknown
            )
        } else if (eCount > cCount) {
            val diff = eCount - cCount

            for (i in IntRange(0, diff - 1)) {
                typeParams.add(IntrinsicTypes.AnyType.type)
            }

            val typeParamsString = typeParams.joinToString(", ") { it.name }

            // TODO - is this a good idea? Do we need an explicit wildcard?
            invocation.warn("Type constructor invocation found without explicit type parameters. Type is inferred to be ${entityConstructor.name}<$typeParamsString>",
                SourcePosition.unknown
            )
        }

        val paramsPath = Path(entityConstructor.name) + typeParams.map { Path(it.name) }

        // NOTE - Ephemerality propagates upwards
        // i.e. if a Type Constructor has at least one ephemeral Type Parameters, it too is ephemeral
        // NOTE - Ephemeral means compiler-generated in this context
        val isEphemeral = concreteTypeParameters.any(ValuePositionType::isEphemeral)

        return when (entityConstructor) {
            is TypeConstructor -> Type(paramsPath, concreteTypeParameters, properties, isEphemeral = isEphemeral, traitConformance = traitConformance, typeConstructor = entityConstructor)
            is TraitConstructor -> Trait(paramsPath, concreteTypeParameters, properties, isEphemeral = isEphemeral, traitConformance = traitConformance, traitConstructor = entityConstructor)
            else -> TODO("???")
        }
    }
}