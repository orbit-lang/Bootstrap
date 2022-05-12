package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.ExpressionNode
import org.orbit.types.next.intrinsics.Native

interface WhereClauseType : TypeComponent

// TODO - The compiler should know about constant default values (dependent types?)
data class Field(val name: String, val type: TypeComponent, val defaultValue: ExpressionNode? = null) : WhereClauseType {
    constructor(path: Path, type: TypeComponent) : this(OrbitMangler.mangle(path), type)

    override val kind: Kind = IntrinsicKinds.Type

    override val fullyQualifiedName: String = "($name: ${type.fullyQualifiedName})"
    override val isSynthetic: Boolean = type.isSynthetic

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = TypeRelation.Unrelated(this, other)

    override fun inferenceKey(): String = type.fullyQualifiedName
}