package org.orbit.types.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface Specialisation<T: TypeProtocol> {
    fun specialise(context: Context) : T
}

/**
 * EXAMPLE:
 *
 * trait T(x Int)
 * type A : T
 * type B(t T) // (T) -> B<T>
 *
 * b = B(A()) // => b = B(
 */
class TypePropertySpecialisation

class TypeMonomorphisation(private val typeConstructor: TypeConstructor, private val concreteParameters: List<ValuePositionType>) : Specialisation<Type> {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    override fun specialise(context: Context): Type {
        val abstractParameters = typeConstructor.typeParameters
        val aPCount = abstractParameters.count()
        val cPCount = concreteParameters.count()

        if (cPCount != aPCount) throw invocation.make<TypeSystem>("Incorrect number of type parameters passed to Type Constructor ${typeConstructor.toString(printer)}. Expected $aPCount, found $cPCount", SourcePosition.unknown)

        val concreteProperties = typeConstructor.properties.map {
            when (it.type) {
                is TypeParameter -> {
                    val aIdx = abstractParameters.indexOf(it.type)
                    val concreteType = concreteParameters[aIdx]

                    Property(it.name, concreteType)
                }

                else -> it
            }
        }

        val monomorphisedType = MetaType(typeConstructor, concreteParameters, concreteProperties)
            .evaluate(context) as Type

        // We need to save a record of these specialised types to that we can code gen for them later on
        context.registerMonomorphisation(monomorphisedType)

        return monomorphisedType
    }
}
