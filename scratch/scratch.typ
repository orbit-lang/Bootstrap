# Module M
MkInt => ∆ + Int
MkZero => ∆ + Zero : (box ∆.Int) -> (box ∆.Int) -> box ∆.Int
OpenZero => ∆ + TZero : unbox ∆.Zero
         => check(∆.TZero, unbox ∆.Zero)

OpenM => MkInt(∆)
      => MkZero(∆)
      => OpenZero(∆)
      => dump(∆)

run OpenM(∆)