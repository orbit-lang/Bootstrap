package org.orbit.types.next.intrinsics

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.components.IType
import org.orbit.types.next.components.Type

object Native {
    enum class Module(val path: Path) {
        Intrinsics(OrbitMangler.unmangle("Orb::Types::Intrinsics")),
        Main(OrbitMangler.unmangle("Orb::Core::Main"))
    }

    enum class Type(val module: Native.Module) {
        Unit(Native.Module.Intrinsics),
        Int(Native.Module.Intrinsics),
        Bool(Native.Module.Intrinsics),
        Symbol(Native.Module.Intrinsics);

        val type: IType
            get() = Type(OrbitMangler.mangle(module.path + name))
    }
}