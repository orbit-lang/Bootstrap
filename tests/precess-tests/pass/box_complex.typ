MkBox => ∆ + A
      => ∆ + a : ∆.A
      => ∆ + F : box (∆.T) -> ∆.T
      => dump(∆) as dbg1
      => ∆ + T
      => ∆ + U : unbox ∆.F
      => dump(∆) as dbg2
      => ∆ + b : box ∆.a
      => dump(∆)

run MkBox(∆)