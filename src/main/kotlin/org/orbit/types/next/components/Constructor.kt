package org.orbit.types.next.components

data class Constructor(val type: TypeComponent, val parameters: List<TypeComponent>) : DeclType {
    override val fullyQualifiedName: String get() {
        val pretty = parameters.joinToString("::") { it.fullyQualifiedName }

        return "${type.fullyQualifiedName}::__init__::$pretty"
    }

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Operator

    fun toFunc() : Func = Func(parameters, type)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}
