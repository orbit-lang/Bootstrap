package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.util.Printer

// Containers (modules and apis) are value position types because
// they can be passed around in a similar way to types & traits
interface Container : ValuePositionType

// Apis can never be equal
object ApiEquality : Equality<TypeProtocol, TypeProtocol> {
    override fun isSatisfied(context: Context, source: TypeProtocol, target: TypeProtocol): Boolean = false
}

// Modules can only be compared to Apis. Equality here means the module implements the Api's contract
object ModuleEquality : Equality<TypeProtocol, TypeProtocol> {
    override fun isSatisfied(context: Context, source: TypeProtocol, target: TypeProtocol): Boolean {
        return if (source is Module && target is Api) {
            source.conforms(target, context)
        } else false
    }
}

data class Api(override val name: String, val requiredTypes: List<Type> = emptyList(), val requiredSignatures: List<SignatureProtocol<*>> = emptyList()) : Container {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = ApiEquality
    override val isEphemeral: Boolean = false

    fun monomorphise(substitutions: (Type) -> TypeAlias) : ConcreteApi {
        return ConcreteApi(this, requiredTypes.map(substitutions))
    }
}

data class ConcreteApi(private val virtualApi: Api, private val typeAliases: List<TypeAlias>) : Container {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = ApiEquality
    override val name: String = virtualApi.name
    override val isEphemeral: Boolean = false

    init {
        assert(typeAliases.count() == virtualApi.requiredTypes.count())

        val zipped = virtualApi.requiredTypes
            .sortedBy(Type::name)
            .zip(typeAliases.sortedBy(TypeAlias::name))
            .all { it.first.name == it.second.name }

        assert(zipped)
    }

    fun getType(requiredType: Type) : Type {
        return typeAliases.first { it.name == requiredType.name }.targetType
    }
}

data class Module(override val name: String, val typeAliases: List<TypeAlias> = emptyList(), val entities: List<Entity> = emptyList(), val signatures: List<SignatureProtocol<*>> = emptyList()) : Container {
    constructor(path: Path, typeAliases: List<TypeAlias> = emptyList(), entities: List<Entity> = emptyList(), signatures: List<SignatureProtocol<*>> = emptyList())
        : this(path.toString(OrbitMangler), typeAliases, entities, signatures)

    constructor(node: ModuleNode)
        : this(node.getPath())

    override val isEphemeral: Boolean = false

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = ModuleEquality

    override fun toString(printer: Printer): String {
        val ents = entities.joinToString(", ", transform = { it.toString(printer) })
        val sigs = signatures.joinToString(", ", transform = { it.toString(printer) })

        return """
            |        Entities: ($ents)
            |        Signatures: ($sigs)
        """.trimMargin()
    }
}

fun Type.conforms(to: Trait, module: Module) : Boolean {
    return false
}

fun Module.conforms(to: Api, context: Context) : Boolean {
    if (to.requiredTypes.isEmpty() && to.requiredSignatures.isEmpty()) return true

    val typeAliasConformance = to.requiredTypes
        .fold(true) { acc, next ->
            // TODO - Required type Trait conformance
            val matches = typeAliases.filter { it.name == next.name }

            acc && matches.count() == 1
        }

    val signatureConformance = to.requiredSignatures
        .fold(true) { acc, next ->
            val matches = signatures.count { sig ->
                (next.equalitySemantics as SignatureEquality).isSatisfied(context, next as SignatureProtocol<TypeProtocol>, sig as SignatureProtocol<TypeProtocol>)
            }

            acc && matches == 1
        }

    return typeAliasConformance && signatureConformance
}