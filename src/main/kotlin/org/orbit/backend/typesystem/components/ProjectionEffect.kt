package org.orbit.backend.typesystem.components

data class ProjectionEffect(val type: AnyType, val trait: AnyType) : ITypeEffect {
    override val id: String = "${type.id} ⥅ ${trait.id}"

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType
        = ProjectionEffect(type.substitute(substitution), trait.substitute(substitution))

    override fun invoke(env: IMutableTypeEnvironment) : AnyMetaType {
        if (trait !is Trait) return Never("Projection Effect cannot be guaranteed because $trait is not of Kind Trait")

        env.add(Projection(type, trait), type)

        if (type is TypeVar) {
            val nType = TypeVar(type.name, listOf(ConformanceConstraint(type, trait)))

            env.replace(type, nType)
        }

        return Always
    }
}