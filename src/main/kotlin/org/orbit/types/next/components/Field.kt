package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ExpressionNode
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

interface WhereClauseType : TypeComponent

interface Member : WhereClauseType {
    val memberName: String
    val type: TypeComponent
}

private object PropertySubstitutor : Substitutor<Property> {
    override fun substitute(target: Property, old: TypeComponent, new: TypeComponent): Property = when (target.lambda.returns.fullyQualifiedName) {
        old.fullyQualifiedName -> Property(target.memberName, Func(target.lambda.takes, new))
        else -> target
    }
}

private object FieldSubstituor : Substitutor<Field> {
    override fun substitute(target: Field, old: TypeComponent, new: TypeComponent): Field = when (target.type.fullyQualifiedName) {
        old.fullyQualifiedName -> Field(target.memberName, new, target.defaultValue)
        else -> target
    }
}

object MemberSubstituor : Substitutor<Member> {
    override fun substitute(target: Member, old: TypeComponent, new: TypeComponent): Member = when (target) {
        is Property -> PropertySubstitutor.substitute(target, old, new)
        is Field -> FieldSubstituor.substitute(target, old, new)
        else -> TODO("!!!")
    }
}

data class Property(override val memberName: String, val lambda: Func) : Member {
    constructor(path: Path, lambda: Func) : this(OrbitMangler.mangle(path), lambda)

    override val type: TypeComponent = lambda.returns
    override val kind: Kind = IntrinsicKinds.Type

    override val fullyQualifiedName: String = "($memberName: ${lambda.fullyQualifiedName})"
    override val isSynthetic: Boolean = false

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = TypeRelation.Unrelated(this, other)
    override fun inferenceKey(): String = lambda.fullyQualifiedName
    fun toField() : Field = Field(memberName, lambda.returns)
}

data class Field(override val memberName: String, override val type: TypeComponent, val defaultValue: TypeComponent? = null) : Member {
    constructor(path: Path, type: TypeComponent) : this(OrbitMangler.mangle(path), type)

    init {
        if (defaultValue != null) {
            val inferenceUtil = getKoinInstance<InferenceUtil>()

            if (!AnyEq.eq(inferenceUtil.toCtx(), type, defaultValue)) {
                val invocation = getKoinInstance<Invocation>()
                val printer = getKoinInstance<Printer>()

                throw invocation.make<TypeSystem>("Default value has wrong type. Expected ${type.toString(printer)}, found ${defaultValue.toString(printer)}", SourcePosition.unknown)
            }
        }
    }

    override val kind: Kind = IntrinsicKinds.Type

    override val fullyQualifiedName: String = "($memberName: ${type.fullyQualifiedName})"
    override val isSynthetic: Boolean = type.isSynthetic

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = TypeRelation.Unrelated(this, other)

    override fun inferenceKey(): String = type.fullyQualifiedName
}