package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Enum

object OrbCoreBooleans : IOrbModule {
//    val trueType = Forward("Orb::Core::Booleans::Bool::True")
//    val falseType = Forward("Orb::Core::Booleans::Bool::False")
    private val boolBase = Type("Orb::Core::Booleans::Bool")
    private val forwardBool = Enum(boolBase, emptyList())
    private val trueCase = EnumCase(forwardBool, "true")
    private val falseCase = EnumCase(forwardBool, "false")
    val boolType = Enum(boolBase, listOf(trueCase, falseCase))

    override fun getPublicTypes(): List<AnyType> = listOf(boolType)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
}