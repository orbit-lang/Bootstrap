package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Struct(override val members: List<Pair<String, AnyType>>) : IStructuralType,
    IProductType<String, Struct>,
    IAlgebraicType<Struct>, IAccessibleType<String> {
    override val id: String = "{${members.joinToString("; ") { it.second.id }}}"

    fun getProperties() : List<Property>
        = members.map { Property(it.first, it.second) }

    override fun access(at: String): AnyType = when (val member = members.firstOrNull { it.first == at }) {
        null -> Never("Unknown Member `${at}` for Type $this")
        else -> member.second
    }

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = members.flatMap { it.second.getUnsolvedTypeVariables() }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType {
        if (from is TypeAlias) {
            val projections = env.getProjections(from)
            for (projection in projections) {
                val nProjection = Projection(from, projection.component.target)

                GlobalEnvironment.add(nProjection, from)
            }
        }

        return this
    }

    override fun isSpecialised(): Boolean
        = members.any { it.second is ISpecialisedType && (it.second as ISpecialisedType).isSpecialised() }

    override fun getElement(at: String): AnyType = members.first { it.first == at }.second
    override fun getConstructors(): List<IConstructor<Struct>> = listOf(
        StructConstructor(
            this,
            members.map { it.second })
    )

    override fun substitute(substitution: Substitution): Struct {
        val nStruct = Struct(members.map { Pair(it.first, it.second.substitute(substitution)) })

        val tags = GlobalEnvironment.getTags(this)

        tags.forEach { GlobalEnvironment.tag(nStruct, it) }

        return nStruct
    }

    override fun getCardinality(): ITypeCardinality = when (members.isEmpty()) {
        true -> ITypeCardinality.Mono
        else -> members.map { it.second.getCardinality() }.reduce(ITypeCardinality::plus)
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is Struct -> other.members.count() == members.count() && other.members.zip(members).all {it.first == it.second }
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val prettyMembers = members.joinToString(", ") {
            val prettyName = printer.apply(it.first, PrintableKey.Italics)

            "$prettyName : ${it.second}"
        }

        return "$indent{ $prettyMembers }"
    }

    override fun toString(): String = prettyPrint()
}