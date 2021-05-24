package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.partial

interface Equality<T: TypeProtocol> {
    fun isSatisfied(context: Context, source: T, target: T) : Boolean
}

typealias AnyEquality = Equality<TypeProtocol>

object NominalEquality : Equality<Entity> {
    override fun isSatisfied(context: Context, source: Entity, target: Entity): Boolean {
        return source.name == target.name
    }
}

object StructuralEquality : Equality<Entity> {
    // TODO - Enable equality checking on properties
    override fun isSatisfied(context: Context, source: Entity, target: Entity): Boolean {
        val propertyContracts = source.drawPropertyContracts()

        return target.executeContracts(context, propertyContracts)
    }
}

object SignatureEquality : Equality<SignatureProtocol<TypeProtocol>>, KoinComponent {
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

inline fun <reified T: TypeProtocol, reified U: TypeEqualityUtil<T>> Collection<T>.containsOne(context: Context, element: T, typeEqualityUtil: U, using: Equality<out TypeProtocol>) : Boolean {
    return filter { typeEqualityUtil.equal(context, using as Equality<TypeProtocol>, element, it) }.size == 1
}

data class PropertyContract(val mandatoryProperty: Property) : TypeContract {
    override fun isSatisfiedBy(context: Context, type: TypeProtocol): Boolean {
        if (type !is Entity) return false

        return type.properties.containsOne(context, mandatoryProperty,
            Property, mandatoryProperty.type.equalitySemantics)
    }
}
