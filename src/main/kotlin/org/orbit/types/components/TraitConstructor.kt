package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeConstructorNode

data class TraitConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList(),
                            override val partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList()) : EntityConstructor {
    // TODO - We need a separate 'TraitConstructorEquality' because of method signatures in trait constructors
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality
    override val kind: TypeKind = EntityConstructorKind

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList(), partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList()) : this(path.toString(OrbitMangler), typeParameters, properties, partiallyResolvedTraitConstructors)
    constructor(node: TraitConstructorNode, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList(), partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor> = emptyList()) : this(node.getPath(), typeParameters, properties, partiallyResolvedTraitConstructors)
}