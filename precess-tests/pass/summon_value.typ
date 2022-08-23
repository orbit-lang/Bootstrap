Mk_T => ∆ + T
Mk_a => ∆ + a:∆.T
Summon_c => ∆ + summonValue ∆.T as c
Mk => Mk_T(∆) & Mk_a(∆)
Use_c => check(∆.c, ∆.T)

run Use_c(Summon_c(Mk(∆)))