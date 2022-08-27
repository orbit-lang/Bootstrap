MkUnit => ∆ + Unit
MkBool => ∆ + Bool
MkOpt => ∆ + OptNone
      => ∆ + OptSome
      => ∆ + Opt : (∆.OptNone | ∆.OptSome)
      => ∆ + OptNoneCons : (∆.Unit) -> ∆.OptNone
      => ∆ + OptSomeCons : (∆.OptT) -> (∆.OptSome * ∆.OptT)

PrepareOpt => ∆ + Int
           => ∆ + OptT : ∆.Int

ProjectBoolAsOption => MkBool(∆)
                    => ∆ + OptT : ∆.Bool
                    => MkOpt(∆)
                    => ∆ + optionValue : (∆.Bool) -> ∆.Opt

Dbg => dump(∆) as opt

run PrepareOpt(∆) & MkUnit(∆) & MkOpt(∆) & Dbg(∆)
run ProjectBoolAsOption(∆) & Dbg(∆)