package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.toPath

enum class IntrinsicTypes(val type: ValuePositionType) {
    AnyType(Type(name = "Orb::Core::Types::AnyType", isRequired = false, equalitySemantics = AnyTypeEquality)),
    Unit(Type("Orb::Types::Intrinsics::Unit", isRequired = false)),
    Int(Type("Orb::Types::Intrinsics::Int", isRequired = false)),
    Symbol(Type("Orb::Types::Intrinsics::Symbol", isRequired = false)),
    Main(Type("Orb::Core::Main::Main", properties = listOf(Property("argc", Int.type)), isRequired = false)),
    BootstrapCoreStub(Type("Bootstrap::Core::Stub", isRequired = false)),
    Bool(Type("Orb::Types::Intrinsics::Bool", isRequired = false)),

    Type(Type("Orb::Meta::Entities::Type", isRequired = false));

    companion object {
        val allTypes: Set<TypeProtocol>
            get() = values().map { it.type }.toSet()

        fun isIntrinsicType(path: Path) : Boolean {
            val mangled = OrbitMangler.mangle(path)

            return values()
                .map { it.type.name }
                .contains(mangled)
        }
    }

    val path: Path
        get() = OrbitMangler.unmangle(type.name)
}