package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Result
import java.util.UUID

object ExtensionStubPhase : TypePhase<ExtensionNode, Extension>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<ExtensionNode>): Extension {
        val extends = input.inferenceUtil.infer(input.node.targetTypeNode)
        val nInferenceUtil = input.inferenceUtil.derive(self = extends)

        TODO("")
//        val signatureNodes = input.node.bodyNodes.map { it.signature }
//        val signatures = nInferenceUtil.inferAllAs<MethodSignatureNode, Signature>(signatureNodes, AnyInferenceContext(MethodSignatureNode::class.java))
//
//        return when (extends) {
//            is PolymorphicType<*> -> Extension(extends, signatures, Context(UUID.randomUUID().toString(), emptyList(), emptyList()))
//            else -> throw invocation.make<TypeSystem>("Only Types of Kind level >= 1 may be extended (e.g. Kind: ${HigherKind.typeConstructor1.toString(printer)}), found ${extends.toString(printer)} (Kind: ${extends.kind.toString(printer)})", input.node.targetTypeNode)
//        }
    }
}

object ExtensionPhase : TypePhase<ExtensionNode, Extension>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<ExtensionNode>): Extension {
        val extendedType = input.inferenceUtil.infer(input.node.targetTypeNode)
        val nInferenceUtil = input.inferenceUtil.derive(self = extendedType)
        val extension = input.inferenceUtil.get(input.node) as Extension
        val nContext = input.inferenceUtil.getContexts(extension.extends)
            .map { it.context }
            .fold(Context.empty) { acc, next -> when (val r = acc.merge(next)) {
                is Result.Success -> r.value
                is Result.Failure -> throw invocation.make<TypeSystem>(r.reason.message, input.node)
            }}

        val result = nContext.apply(nInferenceUtil)

        if (result is Never) throw invocation.make<TypeSystem>(result.message, input.node.context?.firstToken?.position ?: SourcePosition.unknown)

        val nSignatures = MethodStubPhase.executeAll(nInferenceUtil, emptyList()) //input.node.bodyNodes)
            as List<Signature>

        val results = MethodBodyPhase.executeAll(nInferenceUtil, emptyList()) //input.node.bodyNodes)
        val failures = results.filterIsInstance<Never>()
            .fold(Anything as InternalControlType) { acc, next -> acc + next }

        if (failures is Never) throw invocation.make<TypeSystem>("Encountered the following issues in Extension ${extension.toString(printer)}:\n\t${failures.message}", SourcePosition.unknown)

        val nExtension = Extension(extension.extends, nSignatures, nContext)

        input.inferenceUtil.addExtension(extendedType, nExtension)

        return nExtension
    }
}