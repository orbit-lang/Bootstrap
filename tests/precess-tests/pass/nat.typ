MkNat => ∆ + Z
      => ∆ + S : (∆.Nat) -> ∆.Nat
      => ∆ + Nat : ∑(∆.Z, ∆.S)
      => check(∆.Z, ∆.Nat)
      => check(∆.S, ∆.Nat)
      => dump(∆)

run MkNat(∆)