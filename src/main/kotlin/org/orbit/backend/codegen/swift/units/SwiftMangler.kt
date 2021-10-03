package org.orbit.backend.codegen.swift.units

import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.plus
import org.orbit.types.components.Parameter
import org.orbit.types.components.TypeProtocol
import org.orbit.types.components.TypeSignature

object SwiftMangler : Mangler {
    override fun mangle(path: Path): String {
        return path.relativeNames.joinToString("_")
    }

    override fun unmangle(name: String): Path {
        return Path(name.split("_"))
    }

    override fun mangle(signature: TypeSignature): String {
        val mang = (OrbitMangler + this)
        val receiver = mang(signature.receiver.name)
        val ret = mang(signature.returnType.name)


        val params = when (signature.parameters.isEmpty()) {
            true -> ""
            else -> "_" + signature.parameters.map(Parameter::type)
                .map(TypeProtocol::name).joinToString("_", transform = mang)
        }


        return "${receiver}_${signature.name}${params}_$ret"
    }
}