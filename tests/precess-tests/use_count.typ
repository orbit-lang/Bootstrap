AssertMaxUse => ∆ + summonHistory ∆.a
         => ∆ + summonType ∆.useCount as getUseCount
         => ∆ + invoke ∆.getUseCount(∆.h) as useCount
         => ∆ + invoke ∆.compare(1, ∆.UseCount) as result
         => check(∆.result, ∆.Equal)
         => ∆ - h
         => ∆ - getUseCount
         => ∆ - useCount
         => ∆ - result

Prog => ∆ + A
     => ∆ + a : ∆.A
     => ∆ + summonValue ∆.A as a2
     => AssertMaxUse(∆)
     => ∆ + summonValue ∆.A as a3
     => AssertMaxUse(∆)

run Prog(∆)