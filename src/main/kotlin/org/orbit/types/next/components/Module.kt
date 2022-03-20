package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

class Module(override val fullyQualifiedName: String, imports: List<Module> = emptyList()) : DeclType, IContext {
    constructor(path: Path, imports: List<Module> = emptyList())
        : this(OrbitMangler.mangle(path), imports)

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

    fun extendAll(types: List<IType>) : Module {
        types.forEach(::extend)

        return this
    }

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
