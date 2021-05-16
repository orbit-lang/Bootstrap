package org.orbit.types.components

//class Application(val lambda: Lambda, val parameter: Type) : Type {
//    override val name: String = "${lambda.name}(${parameter.name})"
//    override val behaviours: List<Behaviour> = emptyList()
//    override val properties: List<Property> = listOf(
//        Property("lambda", lambda),
//        Property("parameter", parameter)
//    )
//}

//class ApplicativeEquality(application: Application) : Equality {
//    override val source: Type = application.lambda.inputType
//    override val target: Type = application.parameter
//
//    override fun satisfied(): Boolean {
//        return StructuralEquality(source, target)
//            .satisfied()
//    }
//}