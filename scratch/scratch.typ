MkOpt => ∆ + OptNone
      => ∆ + OptSome
      => ∆ + Opt : (OptNone | OptSome)
      => ∆ + OptNoneCons : (∆.Unit) -> ∆.OptNone
      => ∆ + OptSomeCons : (∆.OptT) -> (∆.OptSome * ∆.OptT)