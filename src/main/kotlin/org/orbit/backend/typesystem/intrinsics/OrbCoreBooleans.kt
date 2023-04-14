package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*

object OrbCoreBooleans : IOrbModule {
    val trueType = Forward("Orb::Core::Booleans::Bool::True")
    val falseType = Forward("Orb::Core::Booleans::Bool::False")
    val boolType = Forward("Orb::Core::Booleans::Bool")

    override fun getPublicTypes(): List<AnyType> = listOf(trueType, falseType, boolType)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
}