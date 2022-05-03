package org.orbit.types.next.intrinsics

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.components.Module
import org.orbit.types.next.components.Type
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.components.getPath

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
        data class Mirror(val reflectedType: TypeComponent) : Types(Native.Modules.Intrinsics, "Mirror::${reflectedType.getPath(OrbitMangler).toString(OrbitMangler)}")

        val type: Type = Type(module.path + name)
    }

    sealed class Traits(val module: Native.Modules, val name: String) {
        object AnyType : Traits(Native.Modules.Intrinsics, "Orb::Core::Types::AnyType")
    }
}