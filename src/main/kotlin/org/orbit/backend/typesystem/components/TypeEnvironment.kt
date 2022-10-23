package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.OperatorFixity

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
}

operator fun IType.TypeVar.times(type: AnyType) : Specialisation
    = Specialisation(this, type)

infix fun IType.TypeVar.to(type: AnyType) : Specialisation
    = Specialisation(this, type)

data class ContextualDeclaration<C: IContextualComponent>(val context: Context, val component: C)

sealed interface ITypeEnvironment {
    val name: String

    fun getAllTypes() : List<ContextualDeclaration<AnyType>>
    fun getTypeOrNull(name: String) : ContextualDeclaration<AnyType>?
    fun getProjections(type: AnyType) : List<ContextualDeclaration<Projection>>
    fun getContextOrNull(name: String) : Context?
    fun getBinding(name: String) : IRef?
    fun getCurrentContext() : Context
}

fun ITypeEnvironment.getSignatures() : List<ContextualDeclaration<IType.Signature>>
    = getAllTypes().filterIsInstance<ContextualDeclaration<IType.Signature>>()

fun ITypeEnvironment.getSignatures(name: String) : List<ContextualDeclaration<IType.Signature>> {
    val signatures = mutableListOf<ContextualDeclaration<IType.Signature>>()
    for (signature in getSignatures()) {
        if (signature.component.name == name) signatures.add(signature)
    }

    return signatures
}

fun ITypeEnvironment.getSignatures(name: String, receiver: AnyType) : List<ContextualDeclaration<IType.Signature>> {
    val signatures = mutableListOf<ContextualDeclaration<IType.Signature>>()
    for (signature in getSignatures()) {
        if (signature.component.name == name && TypeUtils.checkEq(this, receiver, signature.component.receiver)) signatures.add(signature)
    }

    return signatures
}

fun ITypeEnvironment.getTypeOrNull(path: Path) : ContextualDeclaration<AnyType>?
    = getTypeOrNull(path.toString(OrbitMangler))

interface IMutableTypeEnvironment: ITypeEnvironment {
    fun add(type: AnyType)
    fun add(projection: Projection, type: AnyType)
    fun add(context: Context)
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

data class SelectTypeEnvironment(private val parent: IMutableTypeEnvironment, val match: AnyType) : IMutableTypeEnvironment by parent

data class ConstructorTypeEnvironment(private val parent: IMutableTypeEnvironment, val constructorArgs: List<AnyType>) : IMutableTypeEnvironment by parent
data class AnnotatedTypeEnvironment(private val parent: IMutableTypeEnvironment, val typeAnnotation: AnyType = IType.Always): IMutableTypeEnvironment by parent
data class ProjectionEnvironment(private val parent: IMutableTypeEnvironment, val projection: Projection) : IMutableTypeEnvironment by parent
data class ProjectedSignatureEnvironment(val parent: ProjectionEnvironment, val projectedSignature: IType.Signature) : ITypeEnvironment by parent
data class ContextualTypeEnvironment(private val parent: IMutableTypeEnvironment, private val context: Context) : IMutableTypeEnvironment by parent {
    override fun getCurrentContext(): Context = context
}

fun IMutableTypeEnvironment.fork(name: String = this.name) : LocalEnvironment
    = LocalEnvironment(this, name)

fun IMutableTypeEnvironment.fork(path: Path) : LocalEnvironment
    = LocalEnvironment(this, path.toString(OrbitMangler))

class LocalEnvironment(private val parent: IMutableTypeEnvironment, override val name: String = parent.name) : IMutableTypeEnvironment {
    private val storage = TypeEnvironmentStorage(parent.getCurrentContext())

    override fun add(type: AnyType) {
        storage.add(type)
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

    override fun getTypeOrNull(name: String): ContextualDeclaration<AnyType>?
        = storage.getTypeOrNull(name) ?: parent.getTypeOrNull(name)

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
}

object GlobalEnvironment : IMutableTypeEnvironment by TypeEnvironmentStorage(Context.root) {
    override val name: String = "𝜞"
    override fun getCurrentContext(): Context = Context.root

    private val specialisations = mutableMapOf<String, List<Context>>()

    fun registerSpecialisation(context: Context) {
        if (!context.isComplete()) return

        val pSpecialisations = specialisations[context.name] ?: emptyList()

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

    override fun add(type: AnyType) {
        types.add(ContextualDeclaration(getCurrentContext(), type))
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

    override fun getTypeOrNull(name: String) : ContextualDeclaration<AnyType>?
        = types.firstOrNull { it.component.getCanonicalName() == name }

    override fun getProjections(type: AnyType): List<ContextualDeclaration<Projection>>
        = projections[type.id] ?: emptyList()

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
