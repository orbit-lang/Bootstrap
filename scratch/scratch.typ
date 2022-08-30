MkCombinable => ∆ + T0 : box ∆.Self
             => ∆ + Combinable_combine : (∆.T0) -> (∆.T0) -> ∆.T0

OpenCombinable => MkCombinable(∆)
               => ∆ + summonValue unbox ∆.Combinable_combine as combineFn
               => ∆ + T0Concrete : unbox ∆.T0
               => check(∆.combineFn, (∆.T0Concrete) -> (∆.T0Concrete) -> ∆.T0Concrete)
               => dump(∆)

Prog => ∆ + Int
     => ∆ + Self : ∆.Int
     => ∆ + combine : (∆.Int) -> (∆.Int) -> ∆.Int
     => OpenCombinable(∆)

run Prog(∆)