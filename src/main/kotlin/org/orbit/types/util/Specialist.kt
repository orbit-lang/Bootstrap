package org.orbit.types.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.components.*
import org.orbit.types.phase.TraitEnforcer
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface Specialisation<T: TypeProtocol> {
    fun specialise(context: Context) : T
}

class TraitMonomorphisation(private val traitConstructor: TraitConstructor, private val concreteParameters: List<ValuePositionType>) : Specialisation<Trait> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    override fun specialise(context: Context): Trait {
        val abstractParameters = traitConstructor.typeParameters
        val aPCount = abstractParameters.count()
        val cPCount = concreteParameters.count()

        if (cPCount != aPCount)
            throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Trait Constructor ${traitConstructor.toString(printer)}. Expected $aPCount, found $cPCount", SourcePosition.unknown)

        val concreteProperties = traitConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOfFirst { t -> t.name == it.type.name }
                    val abstractType = traitConstructor.typeParameters[aIdx]
                    var concreteType = concreteParameters[aIdx]

                    concreteType = concreteType
                            as? Type
                        ?: throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${concreteType::class.java.simpleName} ${concreteType.toString(printer)}", SourcePosition.unknown)

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                else -> it
            }
        }

        val monomorphisedType = MetaType(traitConstructor, concreteParameters, concreteProperties, emptyList())

        return monomorphisedType.evaluate(context) as Trait
    }
}

class TypeMonomorphisation(private val typeConstructor: TypeConstructor, private val concreteParameters: List<ValuePositionType>) : Specialisation<Type> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    private fun specialiseTrait(context: Context, partiallyResolvedTraitConstructor: PartiallyResolvedTraitConstructor, abstractParameters: List<TypeParameter>, concreteParameters: List<ValuePositionType>) : Trait {
        val concreteTraitParameters = partiallyResolvedTraitConstructor.typeParameterMap
            .map { abstractParameters.indexOf(it.value) }
            .map { concreteParameters[it] }

        return TraitMonomorphisation(partiallyResolvedTraitConstructor.traitConstructor, concreteTraitParameters)
            .specialise(context)
    }

    override fun specialise(context: Context): Type {
        val nTypePath = concreteParameters.fold(OrbitMangler.unmangle(typeConstructor.name)) { acc, nxt ->
            acc + OrbitMangler.unmangle(nxt.name)
        }

        val nTypeName = nTypePath.toString(OrbitMangler)

        if (context.monomorphisedTypes.containsKey(nTypeName)) {
            return context.monomorphisedTypes[nTypeName]!!
        }

        val abstractParameters = typeConstructor.typeParameters
        val aPCount = abstractParameters.count()
        val cPCount = concreteParameters.count()

        if (cPCount != aPCount)
            throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Type Constructor ${typeConstructor.toString(printer)}. Expected $aPCount, found $cPCount", SourcePosition.unknown)

        val concreteProperties = typeConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOfFirst { t -> t.name == it.type.name }
                    val abstractType = typeConstructor.typeParameters[aIdx]
                    var concreteType = concreteParameters[aIdx]

                    concreteType = concreteType
                        as? Type
                        ?: throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${concreteType::class.java.simpleName} ${concreteType.toString(printer)}", SourcePosition.unknown)

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                else -> it
            }
        }

        val metaTraits = typeConstructor.partiallyResolvedTraitConstructors
            .map { specialiseTrait(context, it, abstractParameters, concreteParameters) }

        val monomorphisedType = MetaType(typeConstructor, concreteParameters, concreteProperties, metaTraits)
            .evaluate(context) as Type

        // We need to save a record of these specialised types to that we can code gen for them later on
        context.registerMonomorphisation(monomorphisedType)

        return monomorphisedType
    }
}
