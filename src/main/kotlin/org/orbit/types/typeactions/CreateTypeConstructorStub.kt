package org.orbit.types.typeactions

import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.types.components.Context
import org.orbit.types.components.TypeConstructor
import org.orbit.types.components.TypeParameter
import org.orbit.util.Printer

class CreateTypeConstructorStub(override val node: TypeConstructorNode) : CreateStub<TypeConstructorNode, TypeConstructor> {
    override val constructor: (TypeConstructorNode) -> TypeConstructor = ::TypeConstructor
}

// TODO - Resolve complex Type Parameters, e.g. type constructor C<T: SomeTrait, N Int>
class ResolveTypeConstructorTypeParameters(private val node: TypeConstructorNode) : TypeAction {
    private lateinit var typeConstructor: TypeConstructor

    override fun execute(context: Context) {
        typeConstructor = context.getTypeByPath(node.getPath()) as TypeConstructor

        val typeParameters = node.typeParameterNodes
            .map(::TypeParameter)

        typeConstructor = TypeConstructor(typeConstructor.name, typeParameters)

        context.remove(typeConstructor.name)
        context.add(typeConstructor)
    }

    override fun describe(printer: Printer): String {
        return "Resolve Type Parameters for Type Constructor..."
    }
}