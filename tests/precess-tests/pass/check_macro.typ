MkCheck => ∆ + T : (box ∆.A) -> box ∆.A
        => check(∆.T, (box ∆.A) -> box ∆.A)
        => dump(∆)

UseCheck => ∆ + A
         => MkCheck(∆)

run MkCheck(∆)