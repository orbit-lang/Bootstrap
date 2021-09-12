package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.types.phase.TypeSystem
import org.orbit.types.util.TypeMonomorphisation
import org.orbit.util.Invocation
import org.orbit.util.Printer

object MetaTypeInference : TypeInference<MetaTypeNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(context: Context, node: MetaTypeNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val typeConstructor = context.getTypeByPath(node.getPath())
            // TODO - Trait Constructors
            as? TypeConstructor
            ?: TODO("")

        // TODO - Recursive inference on type parameters
        val typeParameters = node.typeParameters
            .map { TypeExpressionInference.infer(context, it, null) }
            .map {
                it as? ValuePositionType
                    ?: throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete types, found ${it::class.java.simpleName} ${it.toString(printer)}", node)
            }

        val specialisation = TypeMonomorphisation(typeConstructor, typeParameters)

        return specialisation.specialise(context)
    }
}