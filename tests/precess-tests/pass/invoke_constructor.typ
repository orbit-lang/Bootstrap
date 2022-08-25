MkUnit => ∆ + Unit

TCons => MkUnit(∆)
      => ∆ + tCons:(∆.Unit) -> ∆.T

MkT => ∆ + T
    => TCons(∆)

InvokeTCons => MkT(∆)
            => ∆ + t:∆.T

Dbg => dump(∆) as dbg

run InvokeTCons(∆) & Dbg(∆)