Dbg => dump(∆) as dbg
Undecl => ∆ + T
       => ∆ + t:∆.T
       => Dbg(∆)
       => ∆ - t:∆.T
       => ∆ - T
       => Dbg(∆)

run Undecl(∆)