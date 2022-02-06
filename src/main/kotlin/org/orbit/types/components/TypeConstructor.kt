package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeConstructorNode
import java.io.Serializable

data class PartiallyResolvedTraitConstructor(val traitConstructor: TraitConstructor, val typeParameterMap: Map<TypeParameter, TypeParameter>) : Serializable

data class TypeConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList(), override val partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList(), private val signatures: List<TypeSignature> = emptyList()) : EntityConstructor {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality
    override val kind: TypeKind = EntityConstructorKind

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList(), partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList()) : this(path.toString(OrbitMangler), typeParameters, properties, partiallyResolvedTraitConstructors)

    constructor(other: TypeConstructor, partiallyResolvedTraitConstructor: List<PartiallyResolvedTraitConstructor>)
        : this(other.name, other.typeParameters, other.properties, partiallyResolvedTraitConstructor)

    constructor(node: TypeConstructorNode, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList(), partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList()) : this(node.getPath(), typeParameters, properties, partiallyResolvedTraitConstructors)
}