package org.orbit.types.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object KindEquality : Equality<TypeKind, TypeKind> {
    override fun isSatisfied(context: ContextProtocol, source: TypeKind, target: TypeKind): Boolean {
        return source.name == target.name
    }
}

interface TypeKind : TypeProtocol {
    val level: Int

    override fun toString(printer: Printer): String = printer.apply("($name)", PrintableKey.Bold, PrintableKey.Italics)
}

object NullaryType : TypeKind {
    override val level: Int = 0
    override val name: String = "*"
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = KindEquality
    override val kind: TypeKind = NullaryType
}
object EntityConstructorKind : TypeKind {
    override val level: Int = 1
    override val name: String = "* -> *"
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = KindEquality
    override val kind: TypeKind = EntityConstructorKind
}

object FunctionKind : TypeKind {
    override val level: Int = 2
    override val name: String = "* -> * -> *"
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = KindEquality
    override val kind: TypeKind = FunctionKind
}

object ContainerKind : TypeKind {
    override val level: Int = 2
    // NOTE - I have no idea what the Kind of a container is!
    //  I think a Module might be a type (*) that gives you a list of types/functions (* -> *). Maybe?
    //  An Orbit Api (or module constructor) might be a type constructor (* -> *) that gives you a list of types/functions???
    //  Module = * -> (* -> *)
    //  Api = (* -> *) -> (* -> *)
    override val name: String = "* -> (* -> *)"
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = KindEquality
    override val kind: TypeKind = ContainerKind
}

object ContainerConstructorKind : TypeKind {
    override val level: Int = 3
    override val name: String = "(* -> *) -> (* -> *)"
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = KindEquality
    override val kind: TypeKind = ContainerKind
}