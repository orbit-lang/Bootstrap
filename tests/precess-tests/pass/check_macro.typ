MkCheck => ∆ + T : (box ∆.A) -> box ∆.A
        => check(∆.T, (∆.A) -> ∆.A)
        => dump(∆)

UseCheck => ∆ + A
         => MkCheck(∆)

run UseCheck(∆)