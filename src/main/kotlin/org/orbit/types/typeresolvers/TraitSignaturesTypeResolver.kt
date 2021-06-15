package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.nodes.TraitDefNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.Context
import org.orbit.types.components.SignatureProtocol
import org.orbit.types.components.Trait
import org.orbit.util.Invocation

class TraitSignaturesTypeResolver(override val node: TraitDefNode, override val binding: Binding) : EntityTypeResolver<TraitDefNode, Trait>,
    KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<TraitDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context) : Trait {
        var partialTrait = node.getType() as Trait

        val signatures = mutableListOf<SignatureProtocol<*>>()
        for (signature in node.signatures) {
            val signatureType = MethodSignatureTypeResolver(signature, binding, partialTrait)
                .resolve(environment, context)

            signatures.add(signatureType)
        }

        partialTrait = Trait(node.getPath(), partialTrait.properties, signatures)

        node.annotate(partialTrait, Annotations.Type)

        return partialTrait
    }
}