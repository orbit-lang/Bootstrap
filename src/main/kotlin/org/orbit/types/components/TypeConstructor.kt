package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeConstructorNode

data class TypeConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList()) :
    EntityConstructor {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(path.toString(
        OrbitMangler
    ), typeParameters, properties)

    constructor(node: TypeConstructorNode, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(node.getPath(), typeParameters, properties)
}