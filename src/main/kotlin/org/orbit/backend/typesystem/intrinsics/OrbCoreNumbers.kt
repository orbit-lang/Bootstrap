package org.orbit.backend.typesystem.intrinsics

import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.ITypeCardinality

object OrbCoreNumbers : IOrbModule {
    private val intIntArrow get() = IType.Arrow2(intType, intType, intType)

    val intType = IType.Type("Orb::Core::Numbers::Int", explicitCardinality = ITypeCardinality.Infinite)
    val infixAddIntInt = IType.InfixOperator("+", "infixPlus", intIntArrow)
    val infixSubIntInt = IType.InfixOperator("+", "infixSubtract", intIntArrow)
    val infixMulIntInt = IType.InfixOperator("*", "infixMultiply", intIntArrow)
    val infixModIntInt = IType.InfixOperator("%", "infixModulo", intIntArrow)

    override fun getPublicTypes() : List<IType.Type>
        = listOf(intType)

    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()

    override fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
        = listOf(infixAddIntInt, infixSubIntInt, infixMulIntInt, infixModIntInt)
}

object OrbCoreBooleans : IOrbModule {
    val trueType = IType.Type("Orb::Core::Booleans::Bool::True")
    val falseType = IType.Type("Orb::Core::Booleans::Bool::False")

    val boolType = IType.Union(trueType, falseType)

    override fun getPublicTypes(): List<IType.Type> = listOf(trueType, falseType)
    override fun getPublicTypeAliases(): List<IType.Alias> = listOf(IType.Alias("Orb::Core::Booleans::Bool", boolType))
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}

object OrbCoreTypes : IOrbModule {
    val unitType = IType.Type("Orb::Core::Types::Unit")

    override fun getPublicTypes(): List<IType.Type> = listOf(unitType)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}