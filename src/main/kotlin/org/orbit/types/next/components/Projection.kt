package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.onlyOrNull
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.next.TypeMap

interface ProjectedProperty<P: TypeComponent, C: Contract<P>, M: Member> : TypeComponent {
    val propertyName: String

    override val fullyQualifiedName: String
        get() = propertyName
    override val isSynthetic: Boolean
        get() = false
    override val kind: Kind
        get() = IntrinsicKinds.Type

    fun satisfies(ctx: Ctx, contract: C) : ContractResult
    fun project() : M

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}

data class StoredProjectedProperty(val field: Field) : ProjectedProperty<Field, FieldContract, Field> {
    override val propertyName: String = field.memberName

    override fun satisfies(ctx: Ctx, contract: FieldContract): ContractResult {
        val nameEq = contract.input.memberName == field.memberName
        val typeEq = AnyEq.eq(ctx, contract.input.type, field.type)

        return when (nameEq && typeEq) {
            true -> ContractResult.Success(field.type, contract)
            else -> ContractResult.Failure(field.type, contract)
        }
    }

    override fun project(): Field = field

    override fun toString(printer: Printer): String
        = field.toString(printer)
}

data class ComputedProjectedProperty(val field: Field, val lambda: Func) : ProjectedProperty<Field, FieldContract, Property> {
    override val propertyName: String = field.memberName

    override fun satisfies(ctx: Ctx, contract: FieldContract): ContractResult {
        val nameEq = contract.input.memberName == field.memberName
        // TODO - lambda.takes has to eq exactly (self Self)
        val typeEq = AnyEq.eq(ctx, contract.input.type, lambda.returns)

        return when (nameEq && typeEq) {
            true -> ContractResult.Success(field.type, contract)
            else -> ContractResult.Failure(field.type, contract)
        }
    }

    override fun project(): Property
        = Property(field.memberName, lambda)
}

data class ProjectedSignatureProperty(val name: String, val self: TypeComponent, val lambda: Func) : ProjectedProperty<ISignature, SignatureContract, ISignature> {
    override val propertyName: String = name

    override fun satisfies(ctx: Ctx, contract: SignatureContract): ContractResult {
        val nameEq = contract.input.getName() == name
        val typeEq = AnyEq.eq(ctx, contract.input.getReturnType(), lambda.returns) //lambda.toSignature(name, contract.input.getReceiverType()))

        return when (nameEq && typeEq) {
            true -> ContractResult.Success(lambda, contract)
            else -> ContractResult.Failure(lambda, contract)
        }
    }

    override fun project(): ISignature
        = lambda.toSignature(name, self)
}

data class Projection(val baseType: TypeComponent, val trait: ITrait, val projectedProperties: List<ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>>) : DeclType, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override val fullyQualifiedName: String = "(${baseType.fullyQualifiedName} ⥅ ${trait.fullyQualifiedName})"
    override val kind: Kind = IntrinsicKinds.Projection
    override val isSynthetic: Boolean = false

    fun project(inferenceUtil: InferenceUtil) : Type {
        inferenceUtil.addConformance(baseType, trait)
//        TypeMap.addGlobalConformance(baseType, trait)

        val fields = trait.contracts
            .zipWhere(projectedProperties) { a, b -> a.matches(b.propertyName)  }
            .map { it.second.project().substitute(trait, baseType) }

        val signatures = trait.contracts.filterIsInstance<SignatureContract>()
            .map {
                (it.input as Signature)
                    .substitute(Self, baseType, SignatureSubstitutor)
                    .substitute(trait, baseType, SignatureSubstitutor)
            }

        signatures.forEach { inferenceUtil.declare(it) }

        if (baseType !is IType) throw invocation.make<TypeSystem>("Projections on non-Types is not currently supported, found ${baseType.toString(printer)} (Kind: ${baseType.kind.toString(printer)})", SourcePosition.unknown)

        val nType = Type(baseType.fullyQualifiedName, baseType.getMembers() + fields, false)

        inferenceUtil.replace(baseType, nType)
        inferenceUtil.declare(this)

        return nType
    }

    fun implementsProjectedTrait(ctx: Ctx) : ContractResult {
        val traitContracts = trait.contracts
        val fieldContracts = traitContracts.filterIsInstance<FieldContract>()
        val signatureContracts = traitContracts.filterIsInstance<SignatureContract>()

        val implementsFields = fieldContracts.fold(ContractResult.None as ContractResult) { acc, next ->
            val projectedField = projectedProperties.onlyOrNull { b -> b.propertyName == next.input.memberName }
                ?: throw invocation.make<TypeSystem>("Projection ${toString(printer)} does not satisfy Field Contract ${next.input.toString(printer)}", SourcePosition.unknown)

            acc + projectedField.satisfies(ctx, next as Contract<TypeComponent>)
        }

        val implementsSignatures = signatureContracts.fold(ContractResult.None as ContractResult) { acc, next ->
            val projectedField = projectedProperties.onlyOrNull { b -> b.propertyName == next.input.getName() }
                ?: throw invocation.make<TypeSystem>("Projection ${toString(printer)} does not satisfy Signature Contract ${next.input.toString(printer)}", SourcePosition.unknown)

            if (projectedField.project() !is ISignature) {
                throw invocation.make<TypeSystem>("Signature ${next.input.toString(printer)} cannot be projected as Field ${projectedField.toString(printer)}", SourcePosition.unknown)
            }

            acc + projectedField.satisfies(ctx, next as Contract<TypeComponent>)
        }

        return implementsFields + implementsSignatures
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}
