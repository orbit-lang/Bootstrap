package org.orbit.precess.backend.components

import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.frontend.components.nodes.ContextLetNode
import org.orbit.precess.frontend.components.nodes.StatementNode

interface StatementWalker<S: StatementNode<S>> : NodeWalker<S>
