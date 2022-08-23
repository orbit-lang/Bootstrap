MkT => ∆ + T
Mkt => ∆ + t:∆.T
S => ∆ + summonValue ∆.T as r
Dbg => dump(∆)
MkAll => MkT(∆) & Mkt(∆)
Prog => Dbg(S(MkAll(∆)))

run Prog(∆)