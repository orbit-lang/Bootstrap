package org.orbit.types.next.components

import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.components.TypeVariable

data class Substitution(val typeVariable: TypeVariable, val type: TypeComponent)