package org.orbit.types.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.components.SourcePosition
import org.orbit.core.injectResult
import org.orbit.types.components.*
import org.orbit.types.phase.TraitEnforcer
import org.orbit.types.phase.TypeSystem
import org.orbit.types.typeactions.SpecialisedMethodReturnTypeCheck
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.partial

interface Specialisation<T: TypeProtocol> {
    fun specialise(context: ContextProtocol) : T
}

class SignatureSelfSpecialisation(private val signatureTemplate: TypeSignature, private val selfType: Type) : Specialisation<TypeSignature> {
    override fun specialise(context: ContextProtocol): TypeSignature {
        val receiverType = when (signatureTemplate.receiver) {
            SelfType -> selfType
            else -> signatureTemplate.receiver
        }

        val returnType = when (signatureTemplate.returnType) {
            SelfType -> selfType
            else -> signatureTemplate.returnType
        }

        val parameters = signatureTemplate.parameters.map {
            val nType = when (it.type) {
                SelfType -> selfType
                else -> it.type
            }

            Parameter(it.name, nType)
        }

        val nSignature = TypeSignature(signatureTemplate.name, receiverType, parameters, returnType, signatureTemplate.typeParameters)

        return nSignature
    }
}

class TraitConstructorMonomorphisation(private val traitConstructor: TraitConstructor, private val concreteParameters: List<ValuePositionType>) : Specialisation<Trait> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    override fun specialise(context: ContextProtocol): Trait {
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
                    var concreteType = when (val c = concreteParameters[aIdx]) {
                        is Type -> c
                        is Trait -> c.synthesise()
                        else -> throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${c::class.java.simpleName} ${c.toString(printer)}", SourcePosition.unknown)
                    }

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral, typeConstructor = (concreteType as? Type)?.typeConstructor)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                is TypeConstructor -> {
                    val specialiser = TypeMonomorphisation(it.type, concreteParameters)
                    val nType = specialiser.specialise(context)

                    Property(it.name, nType)
                }

                is TraitConstructor -> {
                    val specialiser = TraitConstructorMonomorphisation(it.type, concreteParameters)
                    val nTrait = specialiser.specialise(context)

                    Property(it.name, nTrait)
                }

                else -> it
            }
        }

        val monomorphisedType = MetaType(traitConstructor, concreteParameters, concreteProperties, emptyList())
        val trait = monomorphisedType.evaluate(context) as Trait
        val nContext = Context(context as Context)

        nContext.add(SelfType)

        return trait
    }
}

class TypeMonomorphisation(private val typeConstructor: TypeConstructor, private val concreteParameters: List<ValuePositionType>, private val producesEphemeralInstances: Boolean = false, private val omitTypeParameters: Boolean = false) : Specialisation<Type> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
        private val globalContext: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }


    private fun specialiseTrait(context: ContextProtocol, partiallyResolvedTraitConstructor: PartiallyResolvedTraitConstructor, abstractParameters: List<TypeParameter>, concreteParameters: List<ValuePositionType>) : Trait {
        val concreteTraitParameters = partiallyResolvedTraitConstructor.typeParameterMap
            .map { abstractParameters.indexOf(it.value) }
            .map { concreteParameters[it] }

        return TraitConstructorMonomorphisation(partiallyResolvedTraitConstructor.traitConstructor, concreteTraitParameters)
            .specialise(context)
    }

    override fun specialise(context: ContextProtocol): Type {
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

        var isComplete = true
        var monomorphisedType = Type(nTypePath)

        val concreteProperties = typeConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOfFirst { t -> t.name == it.type.name }
                    val abstractType = typeConstructor.typeParameters[aIdx]
                    val concreteType = when (val t = concreteParameters[aIdx]) {
                        is Type -> t
                        is TypeParameter -> t.synthesise()
                        is SelfType -> monomorphisedType
                        else -> throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${t::class.java.simpleName} ${t.toString(printer)}", SourcePosition.unknown)
                    }

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral, typeConstructor = (concreteType as? Type)?.typeConstructor)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                is TypeConstructor -> {
                    val specialiser = TypeMonomorphisation(it.type, concreteParameters)
                    val nType = specialiser.specialise(context)

                    Property(it.name, nType)
                }

                is TraitConstructor -> {
                    isComplete = false
                    val specialiser = TraitConstructorMonomorphisation(it.type, concreteParameters)
                    val nTrait = specialiser.specialise(context)

                    Property(it.name, nTrait)
                }

                else -> it
            }
        }

        val metaTraits = typeConstructor.partiallyResolvedTraitConstructors
            .map { specialiseTrait(context, it, abstractParameters, concreteParameters) }

        monomorphisedType = MetaType(typeConstructor, concreteParameters, concreteProperties, metaTraits, producesEphemeralInstances, omitTypeParameters)
            .evaluate(context) as Type

        // NOTE - This is terrifying!
        val associatedExtensionMethods = globalContext.extensionMethods
            .filter {
                val ec = it.value.entityConstructor
                val eq = ec.equalitySemantics as AnyEquality
                val tc = monomorphisedType.typeConstructor ?: return@filter false

                eq.isSatisfied(context, ec, tc)
            }

        for (ext in associatedExtensionMethods) {
            val specialist = SignatureSelfSpecialisation(ext.value.signature, monomorphisedType)
            val signature = specialist.specialise(context)
            val checkReturnType = SpecialisedMethodReturnTypeCheck(signature, ext.value.body)

            checkReturnType.execute(globalContext)

            globalContext.bind(OrbitMangler.mangle(signature), signature)
            globalContext.registerSpecialisedExtensionMethod(ExtensionTemplate(ext.value.entityConstructor, signature, ext.value.body))
        }

        // We need to save a record of these specialised types to that we can code gen for them later on
        if (isComplete) {
            context.registerMonomorphisation(monomorphisedType)
        }

        return monomorphisedType
    }
}
