package org.orbit.frontend.rules

import org.orbit.core.nodes.WhereClauseExpressionNode

interface WhereClauseExpressionRule<N: WhereClauseExpressionNode> : ParseRule<N>