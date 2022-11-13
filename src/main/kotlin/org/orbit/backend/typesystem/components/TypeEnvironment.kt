package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.Token
import org.orbit.core.nodes.OperatorFixity
import org.orbit.core.phase.flatMapNotNull
import org.orbit.util.Invocation
import org.orbit.util.getKoinInstance
import kotlin.math.exp
import kotlin.math.sin

interface IContextualComponent

data class Specialisation(val abstract: IType.TypeVar, val concrete: AnyType) {
    constructor(abstract: IType.TypeVar) : this(abstract, IType.Never("Type Variable `$abstract` has not been specialised in this Context"))
    constructor(path: Path) : this(IType.TypeVar(path.toString(OrbitMangler)))
    constructor(pair: Pair<IType.TypeVar, AnyType>) : this(pair.first, pair.second)

    private val uniqueId: String get() = "${abstract.name}__${concrete.id}"

    override fun equals(other: Any?): Boolean = when (other) {
        is Specialisation -> other.uniqueId == uniqueId
        else -> false
    }

    fun prettyPrint(depth: Int = 0) : String
        = "${"\t".repeat(depth)}$abstract => $concrete"

    override fun toString(): String
        = prettyPrint()
}

operator fun IType.TypeVar.times(type: AnyType) : Specialisation
    = Specialisation(this, type)

infix fun IType.TypeVar.to(type: AnyType) : Specialisation
    = Specialisation(this, type)

data class ContextualDeclaration<C: IContextualComponent>(val context: Context, val component: C)

sealed interface ITypeEnvironment {
    val name: String

    fun getAllTypes() : List<ContextualDeclaration<AnyType>>
    fun getTypeOrNull(name: String, env: ITypeEnvironment) : ContextualDeclaration<AnyType>?
    fun getProjections(type: AnyType) : List<ContextualDeclaration<Projection>>
    fun getContextOrNull(name: String) : Context?
    fun getBinding(name: String) : IRef?
    fun getCurrentContext() : Context
    fun getKnownContexts() : List<Context>

    fun getSpecialisationEvidence(context: Context) : Set<Specialisation> = emptySet()
}

interface IPanicEnvironment : ITypeEnvironment

fun ITypeEnvironment.getTypeOrNull(name: String) : ContextualDeclaration<AnyType>?
    = getTypeOrNull(name, this)

fun ITypeEnvironment.getSignatures() : List<ContextualDeclaration<IType.Signature>>
    = getAllTypes().filter { it.component is IType.Signature } as List<ContextualDeclaration<IType.Signature>>

fun ITypeEnvironment.getSignatures(name: String) : List<ContextualDeclaration<IType.Signature>> {
    val signatures = mutableListOf<ContextualDeclaration<IType.Signature>>()
    for (signature in getSignatures()) {
        if (signature.component.name == name) signatures.add(signature)
    }

    return signatures
}

fun ITypeEnvironment.getSignatures(name: String, receiver: AnyType) : List<ContextualDeclaration<IType.Signature>> {
    val signatures = mutableListOf<ContextualDeclaration<IType.Signature>>()

    for (signature in getSignatures(name)) {
        var specialisations = GlobalEnvironment.getSpecialisations(signature.context)
            .map { Pair(it, it.specialise(signature.component)) }

        if (specialisations.isEmpty()) {
            specialisations = listOf(Pair(signature.context, signature.component))
        }

        for (specialisation in specialisations) {
            if (TypeUtils.checkEq(this, receiver, specialisation.second.receiver)) signatures.add(ContextualDeclaration(specialisation.first, specialisation.second))
        }
    }

    return signatures
}

fun ITypeEnvironment.getTypeOrNull(path: Path) : ContextualDeclaration<AnyType>?
    = getTypeOrNull(path.toString(OrbitMangler))

interface IMutableTypeEnvironment: ITypeEnvironment {
    fun add(type: AnyType)
    fun add(type: AnyType, explicitContext: Context)
    fun add(projection: Projection, type: AnyType)
    fun add(context: Context)
    fun bind(name: String, type: AnyType)
    fun localCopy() : IMutableTypeEnvironment
}

sealed interface ISelfTypeEnvironment : IMutableTypeEnvironment {
    fun getSelfType() : AnyType
}

data class SelfTypeEnvironment(private val parent: IMutableTypeEnvironment, private val self: AnyType) : ISelfTypeEnvironment, IMutableTypeEnvironment by LocalEnvironment(parent) {
    init {
        add(IType.Alias("Self", self))
    }

    override fun getSelfType(): AnyType = self
}

data class AnnotatedSelfTypeEnvironment(private val parent: IMutableTypeEnvironment, private val self: AnyType, val typeAnnotation: AnyType) : IMutableTypeEnvironment by parent, ISelfTypeEnvironment {
    override fun getSelfType(): AnyType = self
}

data class CaseTypeEnvironment(private val parent: IMutableTypeEnvironment, private val self: AnyType, val match: AnyType) : ISelfTypeEnvironment, IMutableTypeEnvironment by parent {
    override fun getSelfType(): AnyType = self
}

data class StructuralPatternEnvironment(private val parent: ITypeEnvironment, val structuralType: IType.Struct) : ITypeEnvironment by parent

data class ConstructorTypeEnvironment(private val parent: IMutableTypeEnvironment, val constructorArgs: List<AnyType>) : IMutableTypeEnvironment by parent {
    override fun getSpecialisationEvidence(context: Context): Set<Specialisation> {
        val evidence = context.bindings.zip(constructorArgs)
            .filter { it.second == it.first.concrete }
            .map { it.first }
            .toSet()

        return evidence + parent.getSpecialisationEvidence(context)
    }
}

data class AnnotatedTypeEnvironment(private val parent: IMutableTypeEnvironment, val typeAnnotation: AnyType = IType.Always): IMutableTypeEnvironment by parent
data class ProjectionEnvironment(private val parent: IMutableTypeEnvironment, val projection: Projection) : IMutableTypeEnvironment by parent {
    init {
        add(IType.Alias("Self", projection.source))
    }
}
data class ProjectedSignatureEnvironment(val parent: ProjectionEnvironment, val projectedSignature: IType.Signature) : IMutableTypeEnvironment by parent {
    override fun localCopy(): IMutableTypeEnvironment
        = ProjectedSignatureEnvironment(ProjectionEnvironment(LocalEnvironment(parent), parent.projection), projectedSignature)
}
data class ContextualTypeEnvironment(private val parent: IMutableTypeEnvironment, private val context: Context) : IMutableTypeEnvironment by parent {
    override fun getCurrentContext(): Context = context

    override fun add(type: AnyType) {
        parent.add(type, context)
    }
}

fun IMutableTypeEnvironment.fork(name: String = this.name) : LocalEnvironment
    = LocalEnvironment(this, name)

fun IMutableTypeEnvironment.fork(path: Path) : LocalEnvironment
    = LocalEnvironment(this, path.toString(OrbitMangler))

class LocalEnvironment(private val parent: IMutableTypeEnvironment, override val name: String = parent.name) : IMutableTypeEnvironment {
    private val storage = TypeEnvironmentStorage(parent.getCurrentContext())

    override fun localCopy(): IMutableTypeEnvironment = this

    override fun add(type: AnyType) {
        storage.add(type)
    }

    override fun add(type: AnyType, explicitContext: Context) {
        storage.add(type, explicitContext)
    }

    override fun add(projection: Projection, type: AnyType) {
        storage.add(projection, type)
    }

    override fun add(context: Context) = GlobalEnvironment.add(context)

    override fun bind(name: String, type: AnyType) {
        storage.bind(name, type)
    }

    override fun getAllTypes(): List<ContextualDeclaration<AnyType>>
        = storage.getAllTypes() + parent.getAllTypes()

    override fun getTypeOrNull(name: String, env: ITypeEnvironment): ContextualDeclaration<AnyType>?
        = storage.getTypeOrNull(name, env) ?: parent.getTypeOrNull(name, env)

    override fun getProjections(type: AnyType): List<ContextualDeclaration<Projection>> = when (val ps = storage.getProjections(type)) {
        emptyList<Projection>() -> parent.getProjections(type)
        else -> ps
    }

    override fun getCurrentContext(): Context
        = parent.getCurrentContext()

    override fun getContextOrNull(name: String): Context?
        = storage.getContextOrNull(name)

    override fun getBinding(name: String): IRef?
        = storage.getBinding(name) ?: parent.getBinding(name)

    override fun getKnownContexts(): List<Context>
        = storage.getKnownContexts() + parent.getKnownContexts()
}

object GlobalEnvironment : IMutableTypeEnvironment by TypeEnvironmentStorage(Context.root) {
    override val name: String = "𝜞"
    override fun getCurrentContext(): Context = Context.root

    private val specialisations = mutableMapOf<String, List<Context>>()
    private val tags = mutableMapOf<String, List<String>>()
    private val singletonPool = mutableMapOf<String, IValue<*, *>>()

    fun register(singleton: IValue<*, *>) {
        singletonPool[singleton.type.id] = singleton
    }

    fun getSingletonValue(type: AnyType) : IValue<*, *>?
        = singletonPool[type.id]

    fun tag(type: AnyType, tag: String) {
        val pTags = tags[type.getCanonicalName()] ?: emptyList()

        if (pTags.contains(tag)) return

        tags[type.getCanonicalName()] = pTags + tag
    }

    fun getTags(type: AnyType) : List<String>
        = tags[type.getCanonicalName()] ?: emptyList()

    fun getProjectedTags(type: AnyType) : List<ContextualDeclaration<Projection>> {
        val tags = getTags(type)

        if (tags.isEmpty()) return emptyList()

        return tags.flatMap {
            val rType = getTypeOrNull(it) ?: return@flatMap emptyList<ContextualDeclaration<Projection>>()

            getProjections(rType.component)
        }
    }

    fun registerSpecialisation(context: Context) {
        if (!context.isComplete()) return

        val pSpecialisations = specialisations[context.name] ?: emptyList()

        if (pSpecialisations.contains(context)) return

        specialisations[context.name] = pSpecialisations + context
    }

    fun getSpecialisations(context: Context) : List<Context>
        = specialisations[context.name] ?: emptyList()
}

private class TypeEnvironmentStorage(private val context: Context) : IMutableTypeEnvironment {
    override val name: String = ""
    private val types = mutableListOf<ContextualDeclaration<AnyType>>()
    private val projections = mutableMapOf<String, List<ContextualDeclaration<Projection>>>()
    private val contexts = mutableListOf<Context>()
    private val bindings = mutableListOf<IRef>()

    override fun localCopy(): IMutableTypeEnvironment {
        val nStorage = TypeEnvironmentStorage(context)

        nStorage.types.addAll(types)
        nStorage.projections.putAll(projections)
        nStorage.contexts.addAll(contexts)
        nStorage.bindings.addAll(bindings)

        return nStorage
    }

    override fun add(type: AnyType) {
        types.add(ContextualDeclaration(getCurrentContext(), type))
    }

    override fun add(type: AnyType, explicitContext: Context) {
        types.add(ContextualDeclaration(explicitContext, type))
    }

    override fun add(projection: Projection, type: AnyType) {
        val pProjections = projections[type.id] ?: emptyList()

        projections[type.id] = pProjections + ContextualDeclaration(getCurrentContext(), projection)
    }

    override fun add(context: Context) {
        contexts.add(context)
    }

    override fun bind(name: String, type: AnyType) {
        // TODO - Error on conflict
        bindings.add(Ref(name, type))
    }

    override fun getAllTypes(): List<ContextualDeclaration<AnyType>>
        = types

    override fun getTypeOrNull(name: String, env: ITypeEnvironment) : ContextualDeclaration<AnyType>? {
        val type = types.firstOrNull { it.component.getCanonicalName() == name }
            ?: return null

        val specialisations = GlobalEnvironment.getSpecialisations(type.context)

        return when (specialisations.count()) {
            0 -> type
            1 -> ContextualDeclaration(specialisations[0], specialisations[0].specialise(type.component))
            else -> {
                val allEvidence = mutableListOf<Pair<Context, Set<Specialisation>>>()
                for (specialisation in specialisations) {
                    val evidence = env.getSpecialisationEvidence(specialisation)

                    if (evidence.isNotEmpty()) {
                        allEvidence.add(specialisation to evidence)
                    }
                }

                if (allEvidence.count() == 1) {
                    val ctx = allEvidence[0].first

                    println("Found evidence of Type ${type.component} within specialisation: ${allEvidence[0].first} solved by ${allEvidence[0].second}")

                    return ContextualDeclaration(ctx, ctx.specialise(type.component))
                }

                val invocation = getKoinInstance<Invocation>()

                if (allEvidence.isNotEmpty()) {
                    val solved = mutableListOf<Pair<Context, AnyType>>()
                    for (specialisation in specialisations) {
                        for (evidence in allEvidence.map { it.second }) {
                            if (specialisation.isSolvedBy(evidence)) {
                                println("Found evidence of Type ${type.component} within specialisation: $evidence solved by $specialisation")
                                solved.add(specialisation to specialisation.specialise(type.component))
                            }
                        }
                    }

                    val pretty = allEvidence.withIndex().joinToString("\n\t") { "${it.index + 1}. ${it.value.first} where ${it.value.second.joinToString(", ")}" }

                    if (solved.isEmpty()) {
                        // TODO - Construct a real error message for this case, preferably referencing whatever evidence is available
                        return null
                    }

                    if (solved.count() == 1) {
                        println("Found evidence of Type ${type.component} within specialisation: ${solved[0].first} solved by ${solved[0].second}")
                        return ContextualDeclaration(solved[0].first, solved[0].second)
                    }

                    val complete = solved.filter { it.first.isComplete() }

                    if (complete.count() == 1) {
                        return ContextualDeclaration(complete[0].first, complete[0].second)
                    }

                    // TODO - We have possible solutions here, so let's prompt the user for input
                    throw invocation.make<TypeSystem>("Conflicting evidence found for specialised Type ${type.component}:\n\t$pretty")
                }

                val allTypes = specialisations.map { it.specialise(type.component) }
                val pretty = allTypes.joinToString("\n\t")

                throw invocation.make<TypeSystem>("Multiple specialisations found for Type ${type.component}:\n\t$pretty", Token.empty)
            }
        }
    }

    override fun getProjections(type: AnyType): List<ContextualDeclaration<Projection>> = when (type) {
        is IType.Always -> projections.flatMap { it.value }
        else -> projections[type.id] ?: emptyList()
    }

    override fun getContextOrNull(name: String) : Context? {
        val matches = contexts.filter { it.name == name }

        return when (matches.count()) {
            1 -> matches[0]
            else -> null
        }
    }

    override fun getBinding(name: String): IRef?
        = bindings.firstOrNull { it.name == name }

    override fun getCurrentContext(): Context = context
    override fun getKnownContexts(): List<Context> = contexts
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
    = (getAllTypes().filter { it.component is O } as List<ContextualDeclaration<O>>)
        .map { it.component }
