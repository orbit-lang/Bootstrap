package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeChecker
import org.orbit.util.*
import java.io.Serializable

interface Equality<S: TypeProtocol, T: TypeProtocol> : Serializable {
    fun isSatisfied(context: Context, source: S, target: T) : Boolean
}

typealias AnyEquality = Equality<TypeProtocol, TypeProtocol>

object NominalEquality : Equality<Entity, Entity> {
    override fun isSatisfied(context: Context, source: Entity, target: Entity): Boolean {
        return source.name == target.name
    }
}

object StructuralEquality : Equality<Trait, Type>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun isSatisfied(context: Context, source: Trait, target: Type): Boolean {
        val explicitDeclaration = when (source.implicit) {
            true -> true
            else -> {
                if (target.traitConformance.contains(source)) {
                    true
                } else {
                    val projection = context.getTypeProjectionOrNull(target, source)

                    when (projection) {
                        null -> throw invocation.error<TypeChecker>(MissingTypeProjection(source, target))
                        else -> true
                    }
                }
            }
        }

        // TODO - Signatures
        val propertyContracts = source.drawPropertyContracts()

        return explicitDeclaration && target.executeContracts(context, propertyContracts)
    }
}

object TypeConstructorEquality : Equality<TypeConstructor, TypeConstructor> {
    override fun isSatisfied(context: Context, source: TypeConstructor, target: TypeConstructor): Boolean {
        return source.typeParameters.count() == target.typeParameters.count()
            && source.typeParameters.zip(target.typeParameters).all {
                (source.equalitySemantics as AnyEquality)
                    .isSatisfied(context, it.first, it.second)
            }
    }
}

object SignatureEquality : Equality<SignatureProtocol<TypeProtocol>, SignatureProtocol<TypeProtocol>>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun isSatisfied(context: Context, source: SignatureProtocol<TypeProtocol>, target: SignatureProtocol<TypeProtocol>) : Boolean {
        if (target.parameters.size != source.parameters.size) return false

        val receiversEqual = if (source.receiver is Parameter) {
            if (target.receiver !is Parameter) return false
            // This is an instance signature
            val typeA = context.refresh((source.receiver as Parameter).type)
            val typeB = context.refresh((target.receiver as Parameter).type)

            typeA.isSatisfied(context, typeB)
        } else {
            source.receiver.isSatisfied(context, target.receiver)
        }

        val returnEqual = source.returnType.isSatisfied(context, target.returnType)
        // TODO - Method params are currently expected to match on name, type AND position
        val paramsEqual = source.parameters.zip(target.parameters).withIndex().all {
            val nameA = it.value.first.name
            val nameB = it.value.second.name

            // We have to get a fresh pointer to the receiver types
            val typeA = context.refresh(it.value.first.type)
            val typeB = context.refresh(it.value.second.type)

            if (!typeA.isSatisfied(context, typeB)) {
                throw invocation.make<TypeChecker>("Method '${source.name}' declares a parameter '(${it.value.first.name} ${it.value.first.type.name})' at position ${it.index}, found '(${it.value.second.name} ${it.value.second.type.name})'", SourcePosition.unknown)
            }

            if (nameA != nameB) {
                throw invocation.make<TypeChecker>("Method '${source.name}' declares a parameter '(${it.value.first.name} ${it.value.first.type.name})' at position ${it.index}, found '(${it.value.second.name} ${it.value.second.type.name})'", SourcePosition.unknown)
            }

            true
        }

        return receiversEqual && returnEqual && paramsEqual
    }
}

interface Clause {
    fun isSatisfied() : Boolean
}

interface TypeContract {
    fun isSatisfiedBy(context: Context, type: TypeProtocol) : Boolean
}

inline fun <reified T: TypeProtocol, reified U: TypeEqualityUtil<T>> Collection<T>.containsOne(context: Context, element: T, typeEqualityUtil: U, using: Equality<out TypeProtocol, out TypeProtocol>) : Boolean {
    return filter { typeEqualityUtil.equal(context, using as Equality<TypeProtocol, TypeProtocol>, element, it) }.size == 1
}

data class PropertyContract(val mandatoryProperty: Property) : TypeContract {
    override fun isSatisfiedBy(context: Context, type: TypeProtocol): Boolean {
        if (type !is Entity) return false

        return type.properties.containsOne(context, mandatoryProperty,
            Property, mandatoryProperty.type.equalitySemantics)
    }
}
