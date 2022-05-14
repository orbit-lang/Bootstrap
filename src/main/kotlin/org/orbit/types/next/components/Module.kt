package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

class Module(override val fullyQualifiedName: String, val imports: List<Module> = emptyList()) : DeclType, IContext {
    constructor(path: Path, imports: List<Module> = emptyList())
        : this(OrbitMangler.mangle(path), imports)

    override val kind: Kind = IntrinsicKinds.Type

    override val isSynthetic: Boolean = false
    private val context: Ctx = imports.map { it.context }
        .fold(Ctx()) { acc, next -> acc.merge(next) }

    override fun getTypes(): List<TypeComponent> = context.getTypes()
    override fun getConformanceMap(): Map<TypeComponent, List<ITrait>> = context.getConformanceMap()
    override fun getContexts(type: TypeComponent): List<ContextInstantiation> = context.getContexts(type)

    override fun getType(name: String): TypeComponent? = context.getType(name)
    override fun <T : TypeComponent> getTypeAs(name: String): T? = context.getTypeAs(name)

    override fun extend(type: TypeComponent) = context.extend(type)
    override fun map(key: TypeComponent, value: ITrait) = context.map(key, value)
    override fun map(type: String, contexts: List<ContextInstantiation>) = this.context.map(type, contexts)

    fun extendAll(types: List<TypeComponent>) : Module {
        types.forEach(::extend)

        return this
    }

    // NOTE - To avoid outsiders modifying `context`, expose only its read-only interface
    fun getContext() : IContextRead = context

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Module -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}
