package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeCardinality

object OrbCoreNumbers : IOrbModule {
    private val intIntArrow get() = IType.Arrow2(intType, intType, intType)

    val intType = IType.Type("Orb::Core::Numbers::Int", explicitCardinality = ITypeCardinality.Infinite)
    val infixAddIntInt = IType.InfixOperator("+", "infixPlus", intIntArrow)
    val infixSubIntInt = IType.InfixOperator("-", "infixSubtract", intIntArrow)
    val infixMulIntInt = IType.InfixOperator("*", "infixMultiply", intIntArrow)
    val infixModIntInt = IType.InfixOperator("%", "infixModulo", intIntArrow)
    val pow = IType.InfixOperator("**", "infixPow", intIntArrow)

    override fun getPublicTypes() : List<AnyType> = listOf(intType)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
        = listOf(infixAddIntInt, infixSubIntInt, infixMulIntInt, infixModIntInt, pow)
}

object OrbCoreTypes : IOrbModule {
    val unitType = IType.Unit //IType.Type("Orb::Core::Types::Unit")
    val tupleType = IType.Type("Orb::Core::Types::Tuple")

    override fun getPublicTypes(): List<AnyType> = listOf(unitType, tupleType)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}

object OrbCoreErrors : IOrbModule {
    val errorTrait = IType.Trait("Orb::Core::Errors::Error", emptyList(), emptyList())

    override fun getPublicTypes(): List<AnyType> = listOf(errorTrait)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}

