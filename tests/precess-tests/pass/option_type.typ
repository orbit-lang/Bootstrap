MkUnit => ∆ + Unit
MkBool => ∆ + Bool
MkOpt => ∆ + OptNone
      => ∆ + OptSome
      => ∆ + Opt : ∑(∆.OptNone, ∆.OptSome)
      => ∆ + OptNoneCons : (box ∆.Unit) -> ∆.OptNone
      => ∆ + OptSomeCons : (box ∆.OptT) -> ∏(∆.OptSome, box ∆.OptT)

PrepareOpt => ∆ + Int
           => ∆ + OptT : ∆.Int

ProjectBoolAsOption => MkUnit(∆)
                    => MkBool(∆)
                    => ∆ + OptT : ∆.Bool
                    => MkOpt(∆)
                    => ∆ + optionValue : (∆.Bool) -> ∆.Opt

Dbg => dump(∆) as opt

RawOpt => MkOpt(∆)
       => dump(∆)

run RawOpt(∆)
run PrepareOpt(∆) & MkUnit(∆) & MkOpt(∆) & Dbg(∆)
run ProjectBoolAsOption(∆) & Dbg(∆)