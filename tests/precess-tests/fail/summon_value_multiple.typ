Mk_T => ∆ + T
Mk_a => ∆ + a:∆.T
Mk_b => ∆ + b:∆.T
Summon_c => ∆ + summonValue ∆.T as c
Mk => Mk_T(∆) & Mk_a(∆) & Mk_b(∆)
Use_c => check(∆.c, ∆.T)

run Use_c(Summon_c(Mk(∆)))