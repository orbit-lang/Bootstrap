package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.OperatorFixity

data class Specialisation(val abstract: IType.TypeVar, val concrete: AnyType) {
    constructor(abstract: IType.TypeVar) : this(abstract, IType.Never("Type Variable `$abstract` has not been specialised in this Context"))

    override fun equals(other: Any?): Boolean = when (other) {
        is Specialisation -> other.abstract == abstract && other.concrete == concrete
        else -> false
    }
}

operator fun IType.TypeVar.times(type: AnyType) : Specialisation
    = Specialisation(this, type)

infix fun IType.TypeVar.to(type: AnyType) : Specialisation
    = Specialisation(this, type)

sealed interface ITypeEnvironment {
    fun getAllTypes() : List<AnyType>
    fun getTypeOrNull(name: String) : AnyType?
    fun getProjections(type: AnyType) : List<Projection>
    fun getContextOrNull(name: String) : Context?
    fun getBinding(name: String) : AnyType?
}

interface IMutableTypeEnvironment: ITypeEnvironment {
    fun add(type: AnyType)
    fun add(projection: Projection, type: AnyType)
    fun bind(name: String, type: AnyType)
}

sealed interface ISelfTypeEnvironment : IMutableTypeEnvironment {
    fun getSelfType() : AnyType
}

data class SelfTypeEnvironment(private val parent: IMutableTypeEnvironment, private val self: AnyType) : ISelfTypeEnvironment, IMutableTypeEnvironment by parent {
    override fun getSelfType(): AnyType = self
}

data class CaseTypeEnvironment(private val parent: IMutableTypeEnvironment, private val self: AnyType, val match: AnyType) : ISelfTypeEnvironment, IMutableTypeEnvironment by parent {
    override fun getSelfType(): AnyType = self
}

data class AnnotatedTypeEnvironment(private val parent: IMutableTypeEnvironment, val typeAnnotation: AnyType = IType.Always): IMutableTypeEnvironment by parent

data class ProjectionEnvironment(private val parent: ITypeEnvironment, val projectedType: AnyType, val projectedTrait: IType.Trait, val projectedSignature: IType.Signature) : ITypeEnvironment by parent

fun ITypeEnvironment.fork() : LocalEnvironment
    = LocalEnvironment(this)

class LocalEnvironment(private val parent: ITypeEnvironment) : IMutableTypeEnvironment {
    private val storage = TypeEnvironmentStorage()

    override fun add(type: AnyType) {
        storage.add(type)
    }

    override fun add(projection: Projection, type: AnyType) {
        storage.add(projection, type)
    }

    override fun bind(name: String, type: AnyType) {
        storage.bind(name, type)
    }

    override fun getAllTypes(): List<AnyType>
        = storage.getAllTypes() + parent.getAllTypes()

    override fun getTypeOrNull(name: String): AnyType?
        = storage.getTypeOrNull(name) ?: parent.getTypeOrNull(name)

    override fun getProjections(type: AnyType): List<Projection> = when (val ps = storage.getProjections(type)) {
        emptyList<Projection>() -> parent.getProjections(type)
        else -> ps
    }

    override fun getContextOrNull(name: String): Context?
        = storage.getContextOrNull(name)

    override fun getBinding(name: String): AnyType?
        = storage.getBinding(name) ?: parent.getBinding(name)
}

object GlobalEnvironment : IMutableTypeEnvironment by TypeEnvironmentStorage()

data class Context private constructor(val name: String, val bindings: List<Specialisation>) {
    companion object {
        val root = Context("\uD835\uDF92", emptyList())

        fun build(name: String, unknownTypes: List<IType.TypeVar>) : Context
            = Context(name, unknownTypes.map { Specialisation(it) })

        fun build(name: String, unknownType: IType.TypeVar) : Context
            = build(name, listOf(unknownType))
    }

    fun specialise(subs: List<Specialisation>) : Context
        = subs.fold(this) { acc, next -> acc.solving(next.abstract, next.concrete) }

    fun specialise(vararg subs: Specialisation) : Context
        = specialise(subs.toList())

    private fun solving(abstract: IType.TypeVar, concrete: AnyType) : Context = Context(name, bindings.map {
        when (it.abstract.name == abstract.name) {
            true -> it.abstract to concrete
            else -> it
        }
    })

    fun solving(specialisation: Specialisation) : Context
        = solving(specialisation.abstract, specialisation.concrete)

    fun isComplete() : Boolean = getUnsolved().isEmpty()

    fun getUnsolved() : List<IType.TypeVar> = bindings.mapNotNull { when (it.concrete) {
        is IType.Always -> it.abstract
        else -> null
    }}

    override fun equals(other: Any?): Boolean = when (other) {
        is Context -> other.name == name && other.bindings == bindings
        else -> false
    }
}

private class TypeEnvironmentStorage : IMutableTypeEnvironment {
    private val types = mutableListOf<AnyType>()
    private val projections = mutableMapOf<String, List<Projection>>()
    private val contexts = mutableListOf<Context>()
    private val specialisations = mutableMapOf<String, List<Context>>()
    private val bindings = mutableMapOf<String, AnyType>()

    override fun add(type: AnyType) {
        types.add(type)
    }

    override fun add(projection: Projection, type: AnyType) {
        val pProjections = projections[type.id] ?: emptyList()

        projections[type.id] = pProjections + projection
    }

    fun add(context: Context) = contexts.add(context)

    override fun bind(name: String, type: AnyType) {
        // TODO - Error on conflict
        bindings[name] = type
    }

    fun specialise(context: Context, subs: List<Specialisation>) {
        val pSpecialisations = specialisations[context.name] ?: emptyList()

        specialisations[context.name] = pSpecialisations + context.specialise(subs)
    }

    override fun getAllTypes(): List<AnyType>
        = types

    override fun getTypeOrNull(name: String) : AnyType?
        = types.firstOrNull { it.getCanonicalName() == name }

    override fun getProjections(type: AnyType): List<Projection>
        = projections[type.id] ?: emptyList()

    override fun getContextOrNull(name: String) : Context? {
        val matches = contexts.filter { it.name == name }

        return when (matches.count()) {
            1 -> matches[0]
            else -> null
        }
    }

    override fun getBinding(name: String): AnyType?
        = bindings[name]
}

fun ITypeEnvironment.getPrefixOperators() : List<IType.PrefixOperator> = getOperators()
fun ITypeEnvironment.getInfixOperators() : List<IType.InfixOperator> = getOperators()
fun ITypeEnvironment.getPostfixOperators() : List<IType.PostfixOperator> = getOperators()

fun ITypeEnvironment.getOperators(fixity: OperatorFixity) : List<AnyOperator> = when (fixity) {
    OperatorFixity.Prefix -> getPrefixOperators()
    OperatorFixity.Infix -> getInfixOperators()
    OperatorFixity.Postfix -> getPostfixOperators()
}

inline fun <reified O: IType.IOperatorArrow<*, *>> ITypeEnvironment.getOperators() : List<O>
    = getAllTypes().filterIsInstance<IType.Alias>()
        .map { it.type }
        .filterIsInstance<O>()
