package org.orbit.backend.typesystem.components

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.orbit.util.assertIs
import org.orbit.util.getKoinInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue


sealed interface Term

data class Var(val name: String) : Term {
    override fun toString(): String = name
}

data class Abs(val varName: String, val type: Type, val term: Term) : Term {
    val variable: Var = Var(varName)

    override fun toString(): String = "(λ($varName:$type).$term)"
}

data class App(val func: Term, val arg: Term) : Term {
    override fun toString(): String = "($func $arg)"
}

sealed interface Type

data class TyVar(val name: String) : Type {
    override fun toString(): String = name
}

data class TyAbs(val varName: String, val type: Type) : Type {
    val variable: Var = Var(varName)

    override fun toString(): String = "($varName $type)"
}

data class TyApp(val func: Type, val arg: Type) : Type {
    override fun toString(): String = "($func $arg)"
}

val x = Var("x")
val y = Var("y")
val z = Var("z")
val f = Var("f")
val g = Var("g")
val h = Var("h")

val int = TyVar("int")
val bool = TyVar("bool")
val nat = TyVar("nat")
val alpha = TyVar("alpha")
val beta = TyVar("beta")

val id = Abs("x", alpha, x)
val not = Abs("x", bool, Abs("y", bool, App(x, y)))
val zero = Abs("f", TyAbs("alpha", alpha), Abs("x", alpha, x))
val succ = Abs("n", nat, Abs("f", TyAbs("alpha", alpha), Abs("x", alpha, App(f, App(App(Var("n"), f), x)))))

val typeEnv = mapOf(
    x to alpha,
    y to beta,
    z to bool,
    f to TyAbs("alpha", beta),
    g to TyAbs("alpha", TyAbs("beta", alpha)),
    h to TyAbs("alpha", TyAbs("beta", beta))
)

fun typeOf(term: Term, typeEnv: Map<Var, Type>): Type {
    when (term) {
        is Var -> return typeEnv[term]!!
        is Abs -> {
            val varType = typeEnv[term.variable] ?: TyVar(term.varName)
            val subTypeEnv = typeEnv + (term.variable to varType)

            return TyAbs(term.varName, typeOf(term.term, subTypeEnv))
        }
        is App -> {
            val funcType = typeOf(term.func, typeEnv)

            if (funcType !is TyAbs) {
                throw Exception("Function term must be of type TyAbs, but got $funcType")
            }

            val argType = typeOf(term.arg, typeEnv)
            val subTypeEnv = typeEnv + (funcType.variable to argType)

            return typeOf(funcType.variable, subTypeEnv)
        }
    }
}

class Types4Tests {
    @Test
    fun `X`() {
        val res = typeOf(Var("f"), typeEnv)

        println(res)
    }
}