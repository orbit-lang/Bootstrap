package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType

object OrbCoreBooleans : IOrbModule {
    val trueType = IType.Forward("Orb::Core::Booleans::Bool::True")
    val falseType = IType.Forward("Orb::Core::Booleans::Bool::False")
    val boolType = IType.Forward("Orb::Core::Booleans::Bool")

    override fun getPublicTypes(): List<AnyType> = listOf(trueType, falseType, boolType)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}