package org.orbit.types.next.intrinsics

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.components.Module
import org.orbit.types.next.components.Type

object Native {
    sealed class Modules(val path: Path) {
        object Intrinsics : Modules(OrbitMangler.unmangle("Orb::Types::Intrinsics"))
        object Main : Modules(OrbitMangler.unmangle("Orb::Core::Main"))

        val module: Module
            get() = Module(path)
    }

    sealed class Types(val module: Native.Modules, val name: String) {
        object Unit : Types(Native.Modules.Intrinsics, "Unit")
        object Int : Types(Native.Modules.Intrinsics, "Int")
        object Bool : Types(Native.Modules.Intrinsics, "Bool")
        object Symbol : Types(Native.Modules.Intrinsics, "Symbol")

        val type: Type = Type(module.path + name)
    }
}