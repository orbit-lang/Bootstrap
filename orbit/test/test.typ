∆T = ∆e => ∆e + T
∆t = ∆e => ∆e + t:∆e.T
∆f = ∆e => ∆e + f : (∆e.T) -> ∆e.T
∆fT = ∆e => ∆e + ∆T + ∆t + ∆f
P = ∆e => check(∆e.t, ∆e.T) in ∆e
Q = ∆e => ∆e
Prog = ∆e => P(∆e) & Q(∆e)

run Prog(∆fT)