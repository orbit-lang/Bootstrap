package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*

object OrbCoreCollections : IOrbModule {
    val collectionTrait = Trait("Orb::Core::Collections::Collection", emptyList(), emptyList())

    override fun getPublicTypes(): List<AnyType> = listOf(collectionTrait)
    override fun getPublicTypeAliases(): List<TypeAlias> = emptyList()
    override fun getPublicOperators(): List<IOperatorArrow<*, *>> = emptyList()
}