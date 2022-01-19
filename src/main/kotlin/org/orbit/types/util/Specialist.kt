package org.orbit.types.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.components.*
import org.orbit.types.phase.TraitEnforcer
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.partial

interface Specialisation<T: TypeProtocol> {
    fun specialise(context: ContextProtocol) : T
}

class SignatureSelfSpecialisation(private val signatureTemplate: TypeSignature, private val selfType: Type) : Specialisation<TypeSignature> {
    override fun specialise(context: ContextProtocol): TypeSignature {
        val receiverType = when (signatureTemplate.receiver) {
            SelfType -> selfType
            else -> signatureTemplate.receiver
        }

        val returnType = when (signatureTemplate.returnType) {
            SelfType -> selfType
            else -> signatureTemplate.returnType
        }

        val parameters = signatureTemplate.parameters.map {
            val nType = when (it.type) {
                SelfType -> selfType
                else -> it.type
            }

            Parameter(it.name, nType)
        }

        val nSignature = TypeSignature(signatureTemplate.name, receiverType, parameters, returnType, signatureTemplate.typeParameters)

        return nSignature
    }
}

class TraitConstructorMonomorphisation(private val traitConstructor: TraitConstructor, private val concreteParameters: List<ValuePositionType>) : Specialisation<Trait> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    override fun specialise(context: ContextProtocol): Trait {
        val abstractParameters = traitConstructor.typeParameters
        val aPCount = abstractParameters.count()
        val cPCount = concreteParameters.count()

        if (cPCount != aPCount)
            throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Trait Constructor ${traitConstructor.toString(printer)}. Expected $aPCount, found $cPCount", SourcePosition.unknown)

        val concreteProperties = traitConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOfFirst { t -> t.name == it.type.name }
                    val abstractType = traitConstructor.typeParameters[aIdx]
                    var concreteType = concreteParameters[aIdx]

                    concreteType = concreteType
                            as? Type
                        ?: throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${concreteType::class.java.simpleName} ${concreteType.toString(printer)}", SourcePosition.unknown)

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral, typeConstructor = (concreteType as? Type)?.typeConstructor)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                is TypeConstructor -> {
                    val specialiser = TypeMonomorphisation(it.type, concreteParameters)
                    val nType = specialiser.specialise(context)

                    Property(it.name, nType)
                }

                is TraitConstructor -> {
                    val specialiser = TraitConstructorMonomorphisation(it.type, concreteParameters)
                    val nTrait = specialiser.specialise(context)

                    Property(it.name, nTrait)
                }

                else -> it
            }
        }

        val monomorphisedType = MetaType(traitConstructor, concreteParameters, concreteProperties, emptyList())

        return monomorphisedType.evaluate(context) as Trait
    }
}

object Never : TypeLike, Applicable {
    override val name = "_"

    override fun unfold(key: TypeLike): TypeLike = Never
    override fun evaluate(universe: Universe): TypeLike {
        return Never
//        throw Exception("Cannot evaluate Never")
    }

    override fun apply(universe: Universe, arguments: List<Expr>): TypeLike
        = throw Exception("Cannot apply Never")
}

interface Eq {
    fun eq(yes: TypeLike, no: TypeLike) : TypeLike
}

interface SymbolicEq : Eq {
    companion object {
        fun construct(a: TypeLike, b: TypeLike) = object : SymbolicEq {
            override val a = a
            override val b = b

            override fun eq(yes: TypeLike, no: TypeLike): TypeLike {
                return when (a.name == b.name) {
                    true -> yes
                    else -> no
                }
            }
        }
    }

    val a: TypeLike
    val b: TypeLike
}

interface RefEq : Eq {
    companion object {
        fun construct(universe: Universe, a: Expr, b: Expr) = object : RefEq {
            override val universe = universe
            override val a = a
            override val b = b

            override fun eq(yes: TypeLike, no: TypeLike): TypeLike {
                val aValue = evalRec(a, universe)
                val bValue = evalRec(b, universe)

                return SymbolicEq.construct(aValue, bValue)
                    .eq(yes, no)
            }
        }
    }

    val universe: Universe
    val a: Expr
    val b: Expr
}

interface TypeLike {
    companion object {
        fun construct(name: String) = object : TypeLike {
            override val name: String = name
        }
    }

    val name: String

    fun unfold(key: TypeLike = Never) : TypeLike = this
    fun infer(universe: Universe) : TypeLike = this
}

interface ConcreteType : TypeLike

interface Value : TypeLike {
    companion object {
        fun constructor(name: String, type: TypeLike) = when (type) {
            // Only inhabited types have values
            is Never, is Box -> Never
            else -> object : Value {
                override val name = "($name : ${type.name})"
                override val typeAnnotation: TypeLike = type
            }
        }
    }

    val typeAnnotation: TypeLike

    override fun unfold(key: TypeLike): TypeLike = typeAnnotation
}

interface Choice : TypeLike {
    companion object {
        fun construct(pValue: TypeLike, nValue: TypeLike) = object : Choice {
            override val name = "${pValue.name} | ${nValue.name}"
            override val positiveValue = pValue
            override val negativeValue = nValue
        }
    }

    val positiveValue: TypeLike
    val negativeValue: TypeLike

    override fun unfold(key: TypeLike): TypeLike = when (key) {
        is Bool.True, Bool.True -> positiveValue
        is Bool.False, Bool.False -> negativeValue
        else -> Never
    }
}

interface ClosedType : TypeLike {
    companion object {
        fun expandName(closedType: ClosedType) : String
            = "{" + closedType.all().map(TypeLike::name).joinToString(" ") + "}"
    }

    override val name: String
        get() = expandName(this)

    fun all() : List<TypeLike>
}

sealed interface Bool : ConcreteType, Value {
    companion object : ConcreteType, ClosedType {
        override val name: String = "Bool"

        override fun all(): List<TypeLike> = listOf(Bool.True, Bool.False)
    }

    object True : Bool, Value, Expr {
        override val name: String = "true"
        override val typeAnnotation: TypeLike = Bool.Companion

        override fun evaluate(universe: Universe): TypeLike = this
    }

    object False : Bool, Value, Expr {
        override val name: String = "false"
        override val typeAnnotation: TypeLike = Bool.Companion

        override fun evaluate(universe: Universe): TypeLike = this
    }
}

interface Func : TypeLike {
    companion object {
        fun construct(domain: TypeLike, codomain: TypeLike) = when (codomain) {
            is Never -> Never
            else -> object : Func {
                override val domain = domain
                override val codomain = codomain
                override val name = "${domain.name} -> ${codomain.name}"
            }
        }
    }

    val domain: TypeLike
    val codomain : TypeLike

    override fun unfold(key: TypeLike): TypeLike {
        val eq = SymbolicEq.construct(key, domain)

        return eq.eq(codomain.unfold(domain.unfold()), Never)
    }

    override fun infer(universe: Universe): TypeLike = codomain
}

interface Signature : TypeLike {
    companion object {
        fun construct(domain: ClosedType, codomain: TypeLike) : Signature = object : Signature {
            override val domain = domain
            override val codomain = codomain
            override val name = "${ClosedType.expandName(domain)} -> ${codomain.name}"
        }
    }

    val domain: ClosedType
    val codomain: TypeLike

    fun instantiate() : List<Func>
        = domain.all().map { Func.construct(it, codomain) as Func }

    override fun unfold(key: TypeLike): TypeLike {
        for (fn in instantiate()) {
            val eq = SymbolicEq.construct(key, fn.domain)

            if (eq.eq(Bool.True, Never) === Bool.True) {
                return fn.codomain.unfold(fn.domain)
            }
        }

        return Never
    }
}

interface Predicate : Func {
    companion object {
        fun construct(domain: TypeLike) = Func.construct(domain, Bool)
    }
}

interface Box : Expr {
    val key: Expr
    val value: Expr

    companion object {
        fun construct(key: Expr, value: Expr) = object : Box {
            override val name = "box(${key.name} -> ${value.name})"
            override val key = key
            override val value = value
        }
    }

    override fun infer(universe: Universe): TypeLike {
        val keyType = Infer.construct(key).evaluate(universe)
        val valType = Infer.construct(value).evaluate(universe)

        return TypeLike.construct("box(${keyType.name} -> ${valType.name})")
    }

    override fun evaluate(universe: Universe): TypeLike = Never
}

interface Unbox : Expr {
    companion object {
        fun construct(box: Box, key: Expr) = object : Unbox {
            override val name = "unbox(${box.name})(${key.name})"
            override val box = box
            override val key = key
        }
    }

    val box: Box
    val key: Expr

    override fun infer(universe: Universe): TypeLike {
        val keyType = Infer.construct(key).evaluate(universe)
        val valType = Infer.construct(box).evaluate(universe)

        return TypeLike.construct("unbox(${valType.name} <- ${keyType.name})")
    }

    override fun evaluate(universe: Universe): TypeLike {
        return Check.construct(key, box.key).evaluate(universe)
    }
}

interface Expr : TypeLike {
    fun evaluate(universe: Universe) : TypeLike

    fun universalIdentifier() : String = name
}

interface VarExpr : SubExpr {
    companion object {
        fun construct(name: String) = object : VarExpr {
            override val name: String = name

            override fun equals(other: Any?): Boolean = when (other) {
                is VarExpr -> this.name == other.name
                else -> false
            }
        }
    }

    override fun evaluate(universe: Universe): TypeLike {
        return universe.getBinding(name)
    }

    override fun infer(universe: Universe): TypeLike
        = evaluate(universe)

    override fun substitute(sub: Expr): Expr = sub
}

interface AnnotatedVarExpr : VarExpr, AnnotatedExpr<VarExpr> {
    companion object {
        fun construct(name: String, type: TypeLike) = object : AnnotatedVarExpr {
            override val name: String = "($name:${type.name})"
            override val identifier = name
            override val expr = this
            override val type: TypeLike = type
        }
    }

    val identifier: String

    override fun evaluate(universe: Universe): TypeLike
        = expr.evaluate(universe)

    override fun infer(universe: Universe): TypeLike {
        return type
    }

    override fun universalIdentifier(): String = identifier
}

interface AnnotatedExpr<E: Expr> : Expr {
    companion object {
        fun <E: Expr> construct(expr: E, type: TypeLike) = object : AnnotatedExpr<E> {
            override val name = "(${expr.name}:${type.name})"
            override val expr = expr
            override val type = type
        }
    }

    val expr: E
    val type: TypeLike

    override fun evaluate(universe: Universe): TypeLike {
        return expr.evaluate(universe)
    }

    override fun infer(universe: Universe): TypeLike = type
}

interface SubExpr : Expr {
    fun substitute(sub: Expr) : Expr
}

interface Applicable : Expr {
    fun apply(universe: Universe, arguments: List<Expr>) : TypeLike
}

interface Sub : Applicable {
    companion object {
        fun construct(new: Expr, old: SubExpr) = object : Sub {
            override val name = "sub(${old.name} for ${new.name})"
            override val new = new
            override val old = old
        }
    }

    val new: Expr
    val old: SubExpr

    override fun infer(universe: Universe): TypeLike {
        val oldType = Infer.construct(old).evaluate(universe)
        val newType = Infer.construct(new).evaluate(universe)

        return TypeLike.construct("sub(${newType.name} for ${oldType.name})")
    }

    override fun evaluate(universe: Universe): TypeLike = this

    override fun apply(universe: Universe, arguments: List<Expr>): TypeLike {
        if (arguments.count() != 1) return Never

        val oldType = Infer.construct(old).evaluate(universe)
        val argType = Infer.construct(arguments[0]).evaluate(universe)

        if (SymbolicEq.construct(oldType, argType).eq(oldType, Never) == Never) return Never

        return old.substitute(arguments[0])
    }
}

interface Abstraction : Applicable {
    companion object {
        fun construct(parameters: List<VarExpr>, body: Expr) = object : Abstraction {
            override val parameters = parameters
            override val body = body
        }
    }

    val parameters: List<VarExpr>
    val body: Expr

    override val name: String
        get() {
            val parameterNames = parameters.map(VarExpr::name).joinToString(", ")

            return "$parameterNames -> ${body.name}"
        }

    override fun apply(universe: Universe, arguments: List<Expr>): TypeLike {
        if (arguments.count() != parameters.count()) return Never

        // TODO - type check arguments against parameters
        val nBindings = parameters.zip(arguments).map { Pair(it.first.name, it.second) }
        val nUniverse = universe.withTerms(*nBindings.toTypedArray())

        return body.evaluate(nUniverse)
    }

    override fun evaluate(universe: Universe): TypeLike {
        val parameterNames = parameters.map(VarExpr::name).joinToString(", ")

        return TypeLike.construct("$parameterNames -> ${body.name}")
    }

    override fun infer(universe: Universe): TypeLike {
        val inputTypeNames = parameters
            .map(Infer.Companion::construct)
            .map(partial(Infer::evaluate, universe))
            .map(TypeLike::name).joinToString(", ")

        val returnType = Infer.construct(body).evaluate(universe)

        return TypeLike.construct("$inputTypeNames -> ${returnType.name}")
    }
}

fun List<TypeLike>.pretty() : String
    = map(TypeLike::name).joinToString(", ")

interface Apply : Expr {
    companion object {
        fun construct(app: Expr, arguments: List<Expr>) = object : Apply {
            override val name = "${app.name}(${arguments.pretty()})"
            override val app = app
            override val arguments = arguments
        }
    }

    val app: Expr
    val arguments: List<Expr>

    override fun infer(universe: Universe): TypeLike {
        val appType = Infer.construct(app).evaluate(universe)
        val argTypes = arguments
            .map(Infer.Companion::construct)
            .map(partial(Infer::evaluate, universe))
            .pretty()

        return TypeLike.construct("${appType.name}(${argTypes})")
    }

    override fun evaluate(universe: Universe): TypeLike {
        val ap: Applicable = when (app) {
            is Applicable -> app as Applicable
            else -> app.evaluate(universe) as? Applicable ?: Never
        }

        return ap.apply(universe, arguments)
    }
}

object AnyType : TypeLike {
    override val name = "Type"

    override fun infer(universe: Universe): TypeLike = this
}

interface SumType : TypeLike {
    companion object {
        fun construct(left: TypeLike, right: TypeLike) = object : SumType {
            override val name = "(${left.name} x ${right.name})"
            override val left = left
            override val right = right
        }
    }

    val left: TypeLike
    val right: TypeLike
}

interface ProductType : TypeLike {
    companion object {
        fun construct(left: TypeLike, right: TypeLike) = object : ProductType {
            override val name = "(${left.name} + ${right.name})"
            override val left = left
            override val right = right
        }
    }

    val left: TypeLike
    val right: TypeLike
}

interface Tuple : Expr {
    companion object {
        fun construct(left: Expr, right: Expr) = object : Tuple {
            override val name = "(${left.name}, ${right.name})"
            override val left = left
            override val right = right
        }
    }

    val left: Expr
    val right: Expr

    override fun infer(universe: Universe): TypeLike {
        val leftType = Infer.construct(left).evaluate(universe)
        val rightType = Infer.construct(right).evaluate(universe)

        return ProductType.construct(leftType, rightType)
    }

    override fun evaluate(universe: Universe): TypeLike = this
}

sealed interface TupleIndex : Expr {
    interface First : TupleIndex {
        companion object {
            fun construct(tuple: Tuple) = object : TupleIndex.First {
                override val name = "(${tuple.name} 0)"
                override val tuple = tuple
                override val index = 0
            }
        }
    }

    interface Second : TupleIndex {
        companion object {
            fun construct(tuple: Tuple) = object : TupleIndex.Second {
                override val name = "(${tuple.name} 1)"
                override val tuple = tuple
                override val index = 1
            }
        }
    }

    val tuple: Tuple
    val index: Int

    override fun infer(universe: Universe): TypeLike {
        val tupleType = Infer.construct(tuple).evaluate(universe)

        return TypeLike.construct("(${tupleType.name} ${index})")
    }

    override fun evaluate(universe: Universe): TypeLike = when (index) {
        0 -> tuple.left
        else -> tuple.right
    }
}

//interface K : Expr {
//    val lhs: K
//    val rhs: K
//
//    override fun evaluate(universe: Universe): TypeLike {
//        TODO("Not yet implemented")
//    }
//}

fun Map<VarExpr, TypeLike>.toStringMap() : Map<String, TypeLike>
    = map { Pair(it.key.name, it.value) }.toMap()

fun Map<String, TypeLike>.pretty() : String
    = map { "${it.key}: ${it.value.name}" }.joinToString("; ")

data class Universe(val level: Int = 0, private val types: Map<String, TypeLike> = emptyMap(), private val terms: Map<String, TypeLike> = emptyMap()) : TypeLike {
    companion object {
        val empty = Universe()
    }

    override val name: String
        get() = pretty()

    private fun pretty() : String {
        val indent = "\t".repeat(level)
        val typesHeader = "types {${types.pretty()}}"
        val termsHeader = "terms {${terms.pretty()}}"

        return """
            |$indent$typesHeader
            |$indent$termsHeader
        """.trimMargin()
    }

    fun getBinding(name: String) : TypeLike = terms[name] ?: Never
    fun getType(name: String) : TypeLike = types[name] ?: Never

    operator fun plus(other: Universe) : Universe
        = withTypesAndTerms(other.types, other.terms)

    fun withTypes(vararg types: Pair<String, TypeLike>)
        = Universe(level + 1,this.types + types, this.terms)

    fun withTerms(vararg terms: Pair<String, TypeLike>)
        = Universe(level + 1, this.types, this.terms + terms)

    fun withTypesAndTerms(types: Map<String, TypeLike>, terms: Map<String, TypeLike>)
        = Universe(level + 1, this.types + types, this.terms + terms)
}

private fun evalRec(expr: Expr, universe: Universe): TypeLike {
    return when (val v = expr.evaluate(universe)) {
        expr -> v // If we reach the bottom, we're done (TODO - this might be where Boxes come in!)
        is Expr -> evalRec(v, universe)
        else -> v
    }
}

interface Equals : Expr {
    companion object {
        fun construct(lhs: Expr, rhs: Expr) = object : Equals {
            override val name = "(${lhs.name} == ${rhs.name})"
            override val lhs = lhs
            override val rhs = rhs
        }
    }

    val lhs: Expr
    val rhs: Expr

    override fun evaluate(universe: Universe): TypeLike {
        val lhsValue = lhs //.evaluate(universe)
        val rhsValue = rhs //.evaluate(universe)

        println("EQ? ${lhsValue.name} == ${rhsValue.name}")

        return SymbolicEq.construct(lhsValue, rhsValue).eq(Bool.True, Bool.False)
    }
}

fun Array<out Expr>.pretty(level: Int, universe: Universe) : String {
    val indent = "\t".repeat(level + 1)

    return indent + joinToString("\n$indent") { "${it.name} : ${Infer.construct(it).evaluate(universe).name}" }
}

interface Program : Expr {
    companion object {
        fun construct(scope: Universe, vararg instructions: Expr) = object : Program {
            override val name: String
                get() = """
                    |${scope.name}
                    |${instructions.pretty(scope.level, scope)}
                """.trimMargin()

            override val universe = scope
            override val instructions = instructions.toList()
        }
    }

    val universe: Universe
    val instructions: List<Expr>

    override fun evaluate(universe: Universe): TypeLike {
        // Capture any free variables + any local let bindings
        val myUniverse = universe + this.universe
        val results = instructions.map { it.evaluate(myUniverse) }

        return results.lastOrNull() ?: Never
    }

    override fun infer(universe: Universe): TypeLike
        = instructions.lastOrNull()?.infer(universe + this.universe) ?: Never
}

interface Infer : Expr {
    companion object {
        fun construct(expr: TypeLike) = object : Infer {
            override val name = "(infer ${expr.name})"
            override val expr = expr
        }
    }

    val expr: TypeLike

    override fun infer(universe: Universe): TypeLike {
        val exprType = evaluate(universe)

        return TypeLike.construct("(infer ${exprType.name})")
    }

    override fun evaluate(universe: Universe): TypeLike
        = expr.infer(universe)
}

interface SumEq : Eq {
    companion object {
        fun construct(sumType: SumType, type: TypeLike) = object : SumEq {
            override val sumType = sumType
            override val type = type
        }
    }

    val sumType: SumType
    val type: TypeLike

    override fun eq(yes: TypeLike, no: TypeLike): TypeLike {
        val lEq = SymbolicEq.construct(sumType.left, type)
        val rEq = SymbolicEq.construct(sumType.right, type)

        if (lEq.eq(yes, no) === yes) return sumType.left
        if (rEq.eq(yes, no) === yes) return sumType.right

        return Never
    }
}

interface Check : Expr {
    companion object {
        fun construct(expr: Expr, type: TypeLike) = object : Check {
            override val name = "(check ${expr.name} ${type.name})"
            override val expr = expr
            override val type = type
        }
    }

    val expr: Expr
    val type: TypeLike

    override fun evaluate(universe: Universe): TypeLike {
        val exprType = expr.infer(universe)
        val typeType = type.infer(universe)

        if (typeType is AnyType) return Bool.True

        if (typeType is SumType) return SumEq.construct(typeType, exprType)
            .eq(Bool.True, Bool.False)

        return SymbolicEq.construct(exprType, typeType)
            .eq(Bool.True, Bool.False)
    }
}

interface Print : Expr {
    companion object {
        fun construct(expr: Expr) = object : Print {
            override val name = "print(${expr.name})"
            override val expr = expr
        }
    }

    val expr: Expr

    override fun evaluate(universe: Universe): TypeLike {
        val exprValue = evalRec(expr, universe)

        println(exprValue.name)

        return exprValue
    }

    override fun infer(universe: Universe): TypeLike {
        return Infer.construct(expr).evaluate(universe)
    }
}

interface Eval : Expr {
    companion object {
        fun construct(expr: Expr, universe: Universe) = object : Eval {
            override val name = "(eval ${expr.name})"
            override val expr = expr
            override val universe = universe
        }
    }

    val expr: Expr
    val universe: Universe

    override fun infer(universe: Universe): TypeLike {
        val exprType = Infer.construct(expr).evaluate(universe)

        return TypeLike.construct("(eval ${exprType.name})")
    }

    override fun evaluate(universe: Universe): TypeLike = expr.evaluate(universe)
}

class TypeMonomorphisation(private val typeConstructor: TypeConstructor, private val concreteParameters: List<ValuePositionType>, private val producesEphemeralInstances: Boolean = false) : Specialisation<Type> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    private fun specialiseTrait(context: ContextProtocol, partiallyResolvedTraitConstructor: PartiallyResolvedTraitConstructor, abstractParameters: List<TypeParameter>, concreteParameters: List<ValuePositionType>) : Trait {
        val concreteTraitParameters = partiallyResolvedTraitConstructor.typeParameterMap
            .map { abstractParameters.indexOf(it.value) }
            .map { concreteParameters[it] }

        return TraitConstructorMonomorphisation(partiallyResolvedTraitConstructor.traitConstructor, concreteTraitParameters)
            .specialise(context)
    }

    override fun specialise(context: ContextProtocol): Type {
        val nTypePath = concreteParameters.fold(OrbitMangler.unmangle(typeConstructor.name)) { acc, nxt ->
            acc + OrbitMangler.unmangle(nxt.name)
        }

        val nTypeName = nTypePath.toString(OrbitMangler)

        if (context.monomorphisedTypes.containsKey(nTypeName)) {
            return context.monomorphisedTypes[nTypeName]!!
        }

        val abstractParameters = typeConstructor.typeParameters
        val aPCount = abstractParameters.count()
        val cPCount = concreteParameters.count()

        if (cPCount != aPCount)
            throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Type Constructor ${typeConstructor.toString(printer)}. Expected $aPCount, found $cPCount", SourcePosition.unknown)

        var isComplete = true

        val concreteProperties = typeConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOfFirst { t -> t.name == it.type.name }
                    val abstractType = typeConstructor.typeParameters[aIdx]
                    val concreteType = when (val t = concreteParameters[aIdx]) {
                        is Type -> t
                        is TypeParameter -> t.synthesise()
                        else -> throw invocation.make<TypeSystem>("Type Constructors must be specialised on concrete Types, found ${t::class.java.simpleName} ${t.toString(printer)}", SourcePosition.unknown)
                    }

                    // Easiest way to do this is to construct an ephemeral subtype of concreteType + the constraint Traits
                    val ephemeralType = Type(concreteType.name, concreteType.typeParameters, concreteType.properties, abstractType.constraints, concreteType.equalitySemantics, isEphemeral = concreteType.isEphemeral, typeConstructor = (concreteType as? Type)?.typeConstructor)

                    val traitEnforcer = TraitEnforcer(true)

                    traitEnforcer.enforce(context, ephemeralType)

                    Property(it.name, concreteType)
                }

                is TypeConstructor -> {
                    val specialiser = TypeMonomorphisation(it.type, concreteParameters)
                    val nType = specialiser.specialise(context)

                    Property(it.name, nType)
                }

                is TraitConstructor -> {
                    isComplete = false
                    val specialiser = TraitConstructorMonomorphisation(it.type, concreteParameters)
                    val nTrait = specialiser.specialise(context)

                    Property(it.name, nTrait)
                }

                else -> it
            }
        }

        val metaTraits = typeConstructor.partiallyResolvedTraitConstructors
            .map { specialiseTrait(context, it, abstractParameters, concreteParameters) }

        val monomorphisedType = MetaType(typeConstructor, concreteParameters, concreteProperties, metaTraits, producesEphemeralInstances)
            .evaluate(context) as Type

        // We need to save a record of these specialised types to that we can code gen for them later on
        if (isComplete) {
            context.registerMonomorphisation(monomorphisedType)
        }

        return monomorphisedType
    }
}
