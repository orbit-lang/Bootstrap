package org.orbit.precess.backend.ast

import org.orbit.precess.frontend.components.nodes.ContextExprNode

interface ContextExprWalker<C: ContextExprNode<C>> : NodeWalker<C>
