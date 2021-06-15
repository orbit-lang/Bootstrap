package org.orbit.types.components

// Containers (modules and apis) are value position types because
// they can be passed around in a similar way to types & traits
interface Container : ValuePositionType

// Apis can never be equal
object ApiEquality : Equality<TypeProtocol> {
    override fun isSatisfied(context: Context, source: TypeProtocol, target: TypeProtocol): Boolean = false
}

// Modules can only be compared to Apis. Equality here means the module implements the Api's contract
object ModuleEquality : Equality<TypeProtocol> {
    override fun isSatisfied(context: Context, source: TypeProtocol, target: TypeProtocol): Boolean {
        if (source is Module && target is Api) {

        }

        return false
    }
}

data class Api(override val name: String, val requiredTypes: List<Type>) : Container {
    override val equalitySemantics: Equality<out TypeProtocol> = ApiEquality
}

data class Module(override val name: String, val typeAliases: List<TypeAlias>) : Container {
    override val equalitySemantics: Equality<out TypeProtocol> = ModuleEquality
}