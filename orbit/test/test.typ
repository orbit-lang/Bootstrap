∆T = ∆e => ∆e + T
∆t = ∆e => ∆e + t:∆e.T
∆f = ∆e => ∆e + f : (∆e.T) -> ∆e.T
∆fT = ∆e => ∆T(∆e) + ∆t(∆e) + ∆f(∆e)
P = ∆e => check(∆e.t, ∆e.T) in ∆e
Q = ∆e => ∆e
Prog = ∆e => P(∆e) & Q(∆e)

run Prog(∆fT(∆∆))