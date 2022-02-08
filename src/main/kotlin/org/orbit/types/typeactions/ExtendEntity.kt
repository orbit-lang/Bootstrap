package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.getFullyQualifiedPath
import org.orbit.core.getPath
import org.orbit.core.injectResult
import org.orbit.core.nodes.*
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.types.typeresolvers.MethodSignatureTypeResolver
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object TypeIndexTypeInference : TypeInference<TypeIndexNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(context: Context, node: TypeIndexNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        typeAnnotation ?: TODO("Contextual `Self` type is required to resolve TypeIndex expression")

        if (typeAnnotation !is EntityConstructor)
            throw invocation.make<TypeSystem>("Cannot index the type parameters of something that is not an Entity Constructor type", node)

        val entityConstructorPath = typeAnnotation.getFullyQualifiedPath()
        val fullyQualifiedIndexPath = entityConstructorPath + node.index.value

        return typeAnnotation.typeParameters.find { it.getFullyQualifiedPath() == fullyQualifiedIndexPath }
            ?: throw invocation.make<TypeSystem>("Entity Constructor ${typeAnnotation.toString(printer)} has no Type Parameters named '${fullyQualifiedIndexPath.toString(OrbitMangler)}'", node.index)
    }
}

interface WhereClauseExpressionTypeInference<N: WhereClauseExpressionNode> {
    fun infer(context: Context, node: N, constrainedEntityConstructor: ConstrainedEntityConstructor) : ConstrainedEntityConstructor
}

object WhereClauseTypeBoundsTypeInference : WhereClauseExpressionTypeInference<WhereClauseTypeBoundsExpressionNode> {
    override fun infer(context: Context, node: WhereClauseTypeBoundsExpressionNode, constrainedEntityConstructor: ConstrainedEntityConstructor): ConstrainedEntityConstructor {
        val sourceType = TypeInferenceUtil.infer(context, node.sourceTypeExpression, constrainedEntityConstructor.entityConstructor)
            as TypeParameter

        val targetType = TypeInferenceUtil.infer(context, node.targetTypeExpression, constrainedEntityConstructor.entityConstructor)
            as Entity

        val typeParameterBoundsConstraint = TypeParameterBoundsConstraint(node, sourceType, targetType)

        return typeParameterBoundsConstraint.refine(context, constrainedEntityConstructor)
    }
}

object WhereClauseTypeInference : KoinComponent {
    private val invocation: Invocation by inject()

    fun infer(context: Context, node: WhereClauseNode, entityConstructor: ConstrainedEntityConstructor): ConstrainedEntityConstructor = when (node.whereExpression) {
        is WhereClauseTypeBoundsExpressionNode -> {
            WhereClauseTypeBoundsTypeInference.infer(context, node.whereExpression, entityConstructor)
        }

        else -> throw invocation.make<TypeSystem>("Expression is not a valid where clause", node.whereExpression)
    }
}

class ExtendEntity(private val node: ExtensionNode, private val moduleNode: ModuleNode) : TypeAction, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()
    private lateinit var entity: Entity
    private val globalContext: Context by injectResult(CompilationSchemeEntry.typeSystem)

    private fun extendEntityConstructor(context: Context, type: TypeProtocol) : Entity {
        val resolved = TypeInferenceUtil.infer(context, node.targetTypeNode)

        return when (resolved) {
            is Entity -> resolved
            is EntityConstructor -> {
                // By default, `extension SomeEntityConstructor` is shorthand for
                // `extension SomeEntityConstructor where Self[*] : Any`
                val wrapper = node.whereClauses
                    .fold(ConstrainedEntityConstructor(resolved, emptyList())) { acc, next ->
                        WhereClauseTypeInference.infer(context, next, acc)
                    }

                val syntheticType = wrapper.synthesise(context)

                // TODO - Traits
                context.registerSyntheticType(syntheticType as Type)

                syntheticType
            }

            else -> throw invocation.make<TypeSystem>("Only Entity types may be extended, found ${printer.apply(node.getPath().toString(OrbitMangler), PrintableKey.Italics, PrintableKey.Bold)}", node)
        }
    }

    override fun execute(context: Context) {
        // TODO - Entity Constructors/Meta Types
        val type = context.getTypeByPath(node.getPath())
        entity = when (type) {
            is Entity -> type
            is EntityConstructor -> extendEntityConstructor(context, type)
            else -> throw invocation.make<TypeSystem>("Only Entity types may be extended, found ${printer.apply(node.getPath().toString(OrbitMangler), PrintableKey.Italics, PrintableKey.Bold)}", node)
        }

        val localContext = Context(context)

        localContext.add(SelfType)

        if (entity is Trait) {
            // Any methods defined in a Trait Extension are templates for potential future monomorphisation
            for (methodDef in node.methodDefNodes) {
                val receiverPath = methodDef.signature.receiverTypeNode.getPath()

                if (receiverPath != Binding.Self.path) {
                    throw invocation.make<TypeSystem>("Trait Extensions may only define methods on receiver type Self", methodDef.signature.receiverTypeNode)
                }

                val typeResolver = MethodSignatureTypeResolver(methodDef.signature, Binding.Self, null)
                val result = typeResolver.resolve(nameResolverResult.environment, localContext)

                context.registerMonomorphisation(MethodTemplate(entity as Trait, result, methodDef.body))
            }
        } else if (entity is Type) {
            val trait = (entity as Type).synthesiseTrait()

            for (methodDef in node.methodDefNodes) {
                val receiverPath = methodDef.signature.receiverTypeNode.getPath()

                if (receiverPath != Binding.Self.path) {
                    throw invocation.make<TypeSystem>("Type Extensions may only define methods on receiver type Self", methodDef.signature.receiverTypeNode)
                }

                val typeResolver = MethodSignatureTypeResolver(methodDef.signature, Binding.Self, null)
                val result = typeResolver.resolve(nameResolverResult.environment, localContext)

                if (type is EntityConstructor && entity.shouldRegisterExtensionTemplate) {
                    globalContext.registerExtension(ExtensionTemplate(type, result, methodDef.body))
                } else {
                    context.registerMonomorphisation(MethodTemplate(trait, result, methodDef.body))
                }
            }
        }
    }

    override fun describe(printer: Printer): String {
        return "Resolving extension methods for entity ${printer.apply(entity.name, PrintableKey.Italics, PrintableKey.Bold)}"
    }
}