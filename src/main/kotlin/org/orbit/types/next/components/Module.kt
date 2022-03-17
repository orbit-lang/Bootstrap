package org.orbit.types.next.components

class Module(override val fullyQualifiedName: String, imports: List<Module>) : IType, IContext {
    override val isSynthetic: Boolean = false
    private val context: Ctx = imports.map { it.context }
        .fold(Ctx()) { acc, next -> acc.merge(next) }

    override fun getTypes(): List<Type> = context.getTypes()
    override fun getTraits(): List<Trait> = context.getTraits()
    override fun getSignatureMap(): Map<Type, List<Signature>> = context.getSignatureMap()
    override fun getConformanceMap(): Map<IType, List<Trait>> = context.getConformanceMap()
    override fun extend(type: IType) = context.extend(type)
    override fun map(key: Type, value: Signature) = context.map(key, value)
    override fun map(key: IType, value: Trait) = context.map(key, value)

    // NOTE - To avoid outsiders modifying `context`, expose only its read-only interface
    fun getContext() : IContextRead = context

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Module -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

object ModuleSerialiser {

}