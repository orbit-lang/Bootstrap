package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*

object OrbCoreNumbers : IOrbModule {
    private val intIntArrow get() = IType.Arrow2(intType, intType, intType, emptyList())

    val intType = IType.Type("Orb::Core::Numbers::Int", explicitCardinality = ITypeCardinality.Infinite)
    val realType = IType.Type("Orb::Core::Numbers::Real", explicitCardinality = ITypeCardinality.Infinite)

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

object OrbMoreFx : IOrbModule {
    val flowType = IType.Type("Orb::More::Fx::Flow")
    val flowResultType = IType.TypeVar("Orb::More::Fx::FlowCtx::ResultType")
    val flowCtx = Context("Orb::More::Fx::FlowCtx", Specialisation(IType.TypeVar("Orb::More::Fx::FlowCtx::ResultType")))
    val flowResume = IType.Signature(flowType, "resume", listOf(IType.TypeVar("Orb::More::Fx::FlowCtx::ResultType")), IType.Unit, true)

    override fun getContexts(): List<Context> = listOf(flowCtx)
    override fun getPublicTypes(): List<AnyType> = listOf(flowType, flowResume)
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
}

object OrbCoreTypes : IOrbModule {
    val unitType = IType.Unit
    val tupleType = IType.Type("Orb::Core::Types::Tuple")
    val anyType = IType.Always

    override fun getPublicTypes(): List<AnyType> = listOf(unitType, tupleType, anyType)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}

object OrbCoreErrors : IOrbModule {
    val errorTrait = IType.Trait("Orb::Core::Errors::Error", emptyList(), emptyList())

    override fun getPublicTypes(): List<AnyType> = listOf(errorTrait)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}

