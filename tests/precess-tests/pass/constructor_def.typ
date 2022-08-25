MkUnit => ∆ + Unit
TCons => MkUnit(∆) => ∆ + tCons:(∆.Unit) -> ∆.T
MkT => ∆ + T
    => TCons(∆)
    => dump(∆) as mkT

run MkT(∆)