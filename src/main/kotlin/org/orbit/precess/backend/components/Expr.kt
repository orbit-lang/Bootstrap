package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.*

sealed interface Expr<Self : Expr<Self>> : Substitutable<Self>, Inf<Self> {
    data class Var(val name: String) : Expr<Var> {
        override fun substitute(substitution: Substitution): Var = Var(name)

        override fun infer(env: Env): IType<*> =
            env.getRef(name)?.type ?: IType.Never("$name is undefined in the current context")

        override fun toString(): String = name
    }

    data class TypeLiteral(val name: String) : Expr<TypeLiteral> {
        override fun substitute(substitution: Substitution): TypeLiteral = this
        override fun infer(env: Env): IType<*> = env.getElement(name)!!
        override fun toString(): String = "`Type<$name>`"
    }

    data class ArrowLiteral(val arrow: AnyArrow) : Expr<ArrowLiteral> {
        override fun substitute(substitution: Substitution): ArrowLiteral = this
        override fun infer(env: Env): IType<*> = arrow
        override fun toString(): String = "Type<${arrow.id}>"
    }

    data class Block(val body: List<AnyExpr>) : Expr<Block> {
        override fun substitute(substitution: Substitution): Block = Block(body.map { it.substitute(substitution) })

        override fun infer(env: Env): IType<*> = when (body.isEmpty()) {
            true -> IType.Unit
            else -> body.last().infer(env)
        }

        override fun toString(): String = """
                `{
                    ${body.joinToString("\n\t") { it.toString() }}
                }`
            """.trimIndent()
    }

    data class Return(val expr: AnyExpr) : Expr<Return> {
        override fun substitute(substitution: Substitution): Return = Return(expr.substitute(substitution))

        override fun toString(): String = "`return $expr`"

        override fun infer(env: Env): IType<*> = expr.infer(env)
    }

    sealed interface MatchResult {
        data class ReachablePattern(val env: Env) : MatchResult
        data class UnreachablePattern(val reason: IType.Never) : MatchResult
    }

    sealed interface IPattern : Expr<IPattern> {
        fun match(env: Env, target: AnyExpr): MatchResult
    }

    object ElsePattern : IPattern {
        override fun substitute(substitution: Substitution): IPattern = this
        override fun infer(env: Env): IType<*> = IType.Unit
        override fun match(env: Env, target: AnyExpr): MatchResult = MatchResult.ReachablePattern(env)

        override fun toString(): String = "`else`"
    }

    data class EqPattern(val expr: AnyExpr) : IPattern {
        override fun substitute(substitution: Substitution): IPattern = this
        override fun infer(env: Env): AnyType = expr.infer(env)
        override fun match(env: Env, target: AnyExpr): MatchResult =
            when (TypeUtils.check(env, expr, target.infer(env))) {
                true -> MatchResult.ReachablePattern(env)
                else -> MatchResult.UnreachablePattern(IType.Never("$expr will never match against $target"))
            }

        override fun toString(): String = "`case ? == $expr`"
    }

//        data class ConstructorPattern(val type: IType.Type, val) {
//        }

    data class Case(val pattern: IPattern, val block: Block) : Expr<Case> {
        override fun substitute(substitution: Substitution): Case =
            Case(pattern.substitute(substitution), block.substitute(substitution))

        override fun infer(env: Env): IType<*> = IType.Arrow1(pattern.infer(env), block.infer(env))
    }

    data class Select(val target: AnyExpr, val cases: List<Case>) : Expr<Select> {
        override fun substitute(substitution: Substitution): Select =
            Select(target.substitute(substitution), cases.map { it.substitute(substitution) })

        override fun infer(env: Env): IType<*> = cases.map { it.block.infer(env) as IType.UnifiableType<*> }
            .reduce { acc, next -> TypeUtils.unify(env, acc, next) }

        fun verify(env: Env): Select {
            val unreachable = cases.map { it.pattern.match(env, target) }
                .filterIsInstance<MatchResult.UnreachablePattern>()

            if (unreachable.isEmpty()) return this

            unreachable.fold(IType.Never("The following errors were found while verifying Select expression:")) { acc, next ->
                acc.unify(env, next.reason) as IType.Never
            }.panic()
        }
    }

    data class Invoke(val arrow: AnyArrow, val args: List<AnyExpr>) : Expr<Invoke> {
        constructor(arrow: AnyArrow, arg: AnyExpr) : this(arrow, listOf(arg))

        override fun substitute(substitution: Substitution): Invoke =
            Invoke(arrow.substitute(substitution), args.map { it.substitute(substitution) })

        override fun toString(): String = "${arrow.id}(${args.joinToString(", ") { it.toString() }})"

        override fun infer(env: Env): IType<*> {
            val exit = { arrow.never(args.map { it.infer(env) }) }
            val domain = arrow.getDomain()

            if (args.count() != domain.count()) return exit()

            val checked = args.zip(domain).fold(true) { acc, next ->
                if (!acc) return exit()

                acc && TypeUtils.check(env, next.first, next.second)
            }

            if (!checked) return exit()

            return arrow.getCodomain()
        }
    }

    data class Symbol(val name: String) : Expr<Symbol> {
        override fun substitute(substitution: Substitution): Symbol = this
        override fun infer(env: Env): IType<*> = IType.Never("TODO - Symbol")
        override fun toString(): String = name
    }
}