MkBox => ∆ + T
      => ∆ + b : box ∆.T
      => ∆ + u : unbox ∆.b
      => check(∆.u, ∆.T)

run MkBox(∆)