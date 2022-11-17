package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType

object OrbCoreCollections : IOrbModule {
    val collectionTrait = IType.Trait("Orb::Core::Collections::Collection", emptyList(), emptyList())

    override fun getPublicTypes(): List<AnyType> = listOf(collectionTrait)
    override fun getPublicTypeAliases(): List<IType.Alias> = emptyList()
    override fun getPublicOperators(): List<IType.IOperatorArrow<*, *>> = emptyList()
}