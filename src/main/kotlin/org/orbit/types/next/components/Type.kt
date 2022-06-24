package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

interface Entity : DeclType

interface MemberAwareType : Entity {
    fun getMembers(): List<Member>
}

interface IType : MemberAwareType {
    fun deriveTrait(ctx: Ctx) : ITrait
}

data class Type(override val fullyQualifiedName: String, internal val members: List<Member> = emptyList(), override val isSynthetic: Boolean = false) : IType, ConstructableType {
    companion object {
        val hole = Type("_")
    }

    constructor(path: Path, members: List<Member> = emptyList(), isSynthetic: Boolean = false)
        : this(OrbitMangler.mangle(path), members, isSynthetic)

    override val kind: Kind = IntrinsicKinds.Type

    private var _trait: ITrait? = null

    override fun getPrimaryConstructor(): Constructor
        = Constructor(this, members.filterIsInstance<Field>().map { it.type })

    override fun isWeakenedBy(other: DeclType): Boolean = when (other) {
        is Type -> other.fullyQualifiedName == fullyQualifiedName && other.members.count() < members.count()
        else -> false
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is Type -> fullyQualifiedName == other.fullyQualifiedName
        else -> false
    }

    override fun getMembers(): List<Member> = members

    override fun deriveTrait(ctx: Ctx): ITrait = (when (_trait) {
        null -> InterfaceSynthesiser.synthesise(ctx, this)
        else -> _trait!!
    }).also { _trait = it }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Trait -> other.compare(ctx, this)

        is Type -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}
