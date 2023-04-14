package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Unit

object OrbCoreNumbers : IOrbModule {
    private val intIntArrow get() = Arrow2(intType, intType, intType, emptyList())

    val intType = Type("Orb::Core::Numbers::Int", explicitCardinality = ITypeCardinality.Infinite)
    val realType = Type("Orb::Core::Numbers::Real", explicitCardinality = ITypeCardinality.Infinite)

    val infixAddIntInt = InfixOperator("+", "infixPlus", intIntArrow)
    val infixSubIntInt = InfixOperator("-", "infixSubtract", intIntArrow)
    val infixMulIntInt = InfixOperator("*", "infixMultiply", intIntArrow)
    val infixDivIntInt = InfixOperator("/", "infixDivide", intIntArrow)
    val infixModIntInt = InfixOperator("%", "infixModulo", intIntArrow)
    val pow = InfixOperator("**", "infixPow", intIntArrow)

    override fun getPublicTypes() : List<AnyType> = listOf(intType)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators() : List<IOperatorArrow<*, *>>
        = listOf(infixAddIntInt, infixSubIntInt, infixMulIntInt, infixDivIntInt, infixModIntInt, pow)
}

object OrbMoreFx : IOrbModule {
    val flowType = Type("Orb::More::Fx::Flow")
    val flowResultType = TypeVar("Orb::More::Fx::FlowCtx::ResultType")
    val flowCtx = Context("Orb::More::Fx::FlowCtx", Specialisation(TypeVar("Orb::More::Fx::FlowCtx::ResultType")))
    val flowResume = Signature(flowType, "resume", listOf(TypeVar("Orb::More::Fx::FlowCtx::ResultType")), Unit, true)

    override fun getContexts(): List<Context> = listOf(flowCtx)
    override fun getPublicTypes(): List<AnyType> = listOf(flowType, flowResume)
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
}

object OrbCoreTypes : IOrbModule {
    val unitType = Unit
    val tupleType = Type("Orb::Core::Types::Tuple")
    val anyType = Always

    override fun getPublicTypes(): List<AnyType> = listOf(unitType, tupleType, anyType)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
}

object OrbCoreErrors : IOrbModule {
    val errorTrait = Trait("Orb::Core::Errors::Error", emptyList(), emptyList())

    override fun getPublicTypes(): List<AnyType> = listOf(errorTrait)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
}

