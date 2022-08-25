MkUnit => ∆ + Unit

TCons => MkUnit(∆)
      => tCons:(∆.Unit) -> ∆.T

MkT => ∆ + T
    => TCons(∆)
    => dump(∆) as mkT

InvokeTCons => MkT(∆)
            => ∆ + t:∆.T

Dbg => dump(∆) as dbg

run MkT(∆) & InvokeTCons(∆, y) & Dbg(∆)