package org.orbit.backend.typesystem.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.Phase
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.util.Invocation

interface IOrbModule {
    fun getPublicTypes() : List<IType.Type>
    fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
}

private fun IOrbModule.getTypeDecls() : List<Decl.Type>
    = getPublicTypes().map { Decl.Type(it, emptyMap()) }

private fun IOrbModule.getOperatorDecls() : List<Decl.Operator>
    = getPublicOperators().map { Decl.Operator(it) }

fun IOrbModule.getPublicAPI() : Env
    = Env().extendAll(getTypeDecls()).extendAll(getOperatorDecls())

object OrbCoreNumbers : IOrbModule {
    private val intType = IType.Type("Orb::Core::Numbers::Int")
    private val infixMultiplyIntInt = IType.InfixOperator("*", "infixPlus", IType.Arrow2(intType, intType, intType))

    override fun getPublicTypes() : List<IType.Type>
        = listOf(intType)

    override fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
        = listOf(infixMultiplyIntInt)
}

object TypeSystem : Phase<ProgramNode, IType.IMetaType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun execute(input: ProgramNode): IType.IMetaType<*> {
        val env = Env()
            .import(OrbCoreNumbers)

        val result = TypeSystemUtils.inferAs<ProgramNode, IType.IMetaType<*>>(input, env)

        println(env)

        return result
    }
}