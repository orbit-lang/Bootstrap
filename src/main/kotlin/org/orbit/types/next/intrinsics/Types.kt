package org.orbit.types.next.intrinsics

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.components.*

object Native {
    sealed class Modules(val path: Path) {
        object Intrinsics : Modules(OrbitMangler.unmangle("Orb::Types::Intrinsics"))
        object Main : Modules(OrbitMangler.unmangle("Orb::Core::Main"))
        object Meta : Modules(OrbitMangler.unmangle("Orb::Meta"))
        object Kinds : Modules(OrbitMangler.unmangle("Orb::Meta::Kinds"))

        val module: Module
            get() = Module(path)
    }

    sealed class Types(val module: Modules, val name: String) {
        object Unit : Types(Modules.Intrinsics, "Unit")
        object Int : Types(Modules.Intrinsics, "Int")
        object Bool : Types(Modules.Intrinsics, "Bool")
        object Symbol : Types(Modules.Intrinsics, "Symbol")
        object Main : Types(Modules.Main, "Main")
        object Array : Types(Modules.Intrinsics, "Array")
        data class Mirror(val reflectedType: TypeComponent) : Types(Modules.Meta, "Mirror::${reflectedType.getPath(OrbitMangler).toString(OrbitMangler)}")

        val type: org.orbit.types.next.components.Type = Type(module.path + name)
        val path: Path get() = OrbitMangler.unmangle(name)
    }

    sealed class Traits(val module: Modules, val name: String) {
        object AnyType : Traits(Modules.Intrinsics, "Orb::Core::Types::AnyType")
        object Kind : Traits(Modules.Kinds, "Kind")
        object Type : Traits(Modules.Meta, "Type")

        val trait: Trait = Trait(module.path + name)
    }
}