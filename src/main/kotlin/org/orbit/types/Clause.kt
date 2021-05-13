package org.orbit.types

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
    // TODO - Enable equality checking on properties, signatures, or both
    override fun isSatisfied(context: Context, source: Entity, target: Entity): Boolean {
        val propertyContracts = source.drawPropertyContracts()
        val propertyContractsSatisfied = target.executeContracts(context, propertyContracts)

        return propertyContractsSatisfied
    }
}

object SignatureEquality : Equality<SignatureProtocol<TypeProtocol>> {
    override fun isSatisfied(context: Context, source: SignatureProtocol<TypeProtocol>, target: SignatureProtocol<TypeProtocol>) : Boolean {
        return (source.receiver.equalitySemantics as Equality<SignatureProtocol<TypeProtocol>>).isSatisfied(context, source, target)
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
            Property.Companion, mandatoryProperty.type.equalitySemantics)
    }
}

//data class SignatureContract(val mandatorySignature: )



