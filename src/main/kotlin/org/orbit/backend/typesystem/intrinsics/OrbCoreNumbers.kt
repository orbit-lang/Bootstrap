package org.orbit.backend.typesystem.intrinsics

import org.orbit.precess.backend.components.IType

object OrbCoreNumbers : IOrbModule {
    private val intIntArrow get() = IType.Arrow2(intType, intType, intType)

    val intType = IType.Type("Orb::Core::Numbers::Int")
    val infixAddIntInt = IType.InfixOperator("+", "infixPlus", intIntArrow)
    val infixSubIntInt = IType.InfixOperator("+", "infixSubtract", intIntArrow)
    val infixMulIntInt = IType.InfixOperator("*", "infixMultiply", intIntArrow)
    val infixModIntInt = IType.InfixOperator("%", "infixModulo", intIntArrow)

    override fun getPublicTypes() : List<IType.Type>
        = listOf(intType)

    override fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
        = listOf(infixAddIntInt, infixSubIntInt, infixMulIntInt, infixModIntInt)
}

object OrbCoreTypes : IOrbModule {
    val unitType = IType.Type("Orb::Core::Types::Unit")

    override fun getPublicTypes(): List<IType.Type> = listOf(unitType)
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}