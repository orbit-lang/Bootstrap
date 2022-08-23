MkT => ∆ + T
MkFn => ∆ + fn:(∆.T) -> ∆.T
Mk => MkT(∆) & MkFn(∆)
Check => check(∆.fn, (∆.T) -> ∆.T)

run Check(Mk(∆))