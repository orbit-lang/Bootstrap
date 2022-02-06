package org.orbit.types.typeactions

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.getType
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.components.*
import org.orbit.types.util.SignatureSelfSpecialisation
import org.orbit.util.Printer

class AssembleMonoExtensions(private val type: Entity, private val moduleNode: ModuleNode) : TypeAction {
    override fun execute(context: Context) {
        val module = context.getTypeByPath(moduleNode.getPath()) as Module
        val templates = context.monomorphisedMethods.values.mapNotNull {
            if (type is Type && it.trait == type.synthesiseTrait()) {
                return@mapNotNull it
            }

            when (val tc = it.trait.traitConstructor) {
                null -> when (it.trait in type.traitConformance) {
                    true -> it
                    else -> null
                }
                else -> when (tc in type.traitConformance.mapNotNull { mt -> mt.traitConstructor }) {
                    true -> it
                    else -> null
                }
            }
        }

        val nSignatures = templates.map {
            val specialist = SignatureSelfSpecialisation(it.signature, type as Type)
            val signature = specialist.specialise(context)
            val checkReturnType = SpecialisedMethodReturnTypeCheck(signature, it.body)

            checkReturnType.execute(context)

            context.bind(OrbitMangler.mangle(signature), signature)

            context.registerSpecialisedMethod(MethodTemplate(it.trait, signature, it.body))

            signature
        }

        val nModule = Module(module.name, entities = module.entities, signatures = module.signatures + nSignatures)

        context.remove(nModule.name)
        context.add(nModule)
    }

    override fun describe(printer: Printer): String {
        TODO("Not yet implemented")
    }
}

class AssembleExtensions(private val typeDefNode: TypeDefNode, private val moduleNode: ModuleNode) : TypeAction {
    private lateinit var type: Entity

    override fun execute(context: Context) {
        val module = context.getTypeByPath(moduleNode.getPath()) as Module

        val path = typeDefNode.getPath()
        type = context.getTypeByPath(path) as? Entity
            ?: TODO("@AssembleExtensions:16")

        type = context.refreshOrNull<Entity>(type)
            ?: TODO("@AssembleExtensions:19")

        val templates = context.monomorphisedMethods.values.mapNotNull {
            when (val tc = it.trait.traitConstructor) {
                null -> when (it.trait in type.traitConformance) {
                    true -> it
                    else -> null
                }
                else -> when (tc in type.traitConformance.mapNotNull { mt -> mt.traitConstructor }) {
                    true -> it
                    else -> null
                }
            }
        }

        val nSignatures = templates.map {
            val specialist = SignatureSelfSpecialisation(it.signature, type as Type)
            val signature = specialist.specialise(context)
            val checkReturnType = SpecialisedMethodReturnTypeCheck(signature, it.body)

            checkReturnType.execute(context)

            context.bind(OrbitMangler.mangle(signature), signature)

            context.registerSpecialisedMethod(MethodTemplate(it.trait, signature, it.body))

            signature
        }

        val nModule = Module(module.name, entities = module.entities, signatures = module.signatures + nSignatures)

        context.remove(nModule.name)
        context.add(nModule)
    }

    override fun describe(printer: Printer): String {
        TODO("Not yet implemented")
    }
}