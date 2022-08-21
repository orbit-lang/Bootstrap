package org.orbit.precess.backend.ast

import org.orbit.precess.frontend.components.nodes.DeclNode

interface DeclWalker<Self: DeclNode<Self>> : NodeWalker<Self>