# Combine
MkCombinableCtx => ∆ + Combine : (box ∆.CombinableCtxT) -> (box ∆.CombinableCtxT) -> box ∆.CombinableCtxT
OpenCombinableCtx => MkCombinableCtx(∆)
                  => ∆ + combine : unbox ∆.Combine

MkBoxCtx => ∆ + Box : (box ∆.BoxValueType) -> ∆.Box
OpenBoxCtx => MkBoxCtx(∆)

Prog => ∆ + Int
     => ∆ + CombinableCtxT : ∆.Int
     => OpenCombinableCtx(∆)
     => ∆ + summonValue unbox ∆.combine as combine2
     => MkBoxCtx(∆)

run Prog(∆)