# DL blow-up 

Consider a source schema S with unary and binary relations and a
set of TGDs Sigma over S , we define an equivalence relation on
positions of relations of the schema, identifying any two positions
R[i] and S[j] whenever there is a TGD τ in Σ and a variable x in τ
that occurs at both R[i] and S[j].  In the following, we also denote
the classes of equivalence using a position notation.

Below, we present some techniques to complicate the saturation process
of TGDs using a binary or unary schema. These techniques always apply
an arity blow-up to the TGDs to the positions of a position class,
then this blow-up is exploited to complicate the reasoning based on
these TGDs.


## Arity Blow-up

The blown-up schema for blow-up function B has the same
relations as in S, but the arity of a relation $R$ is Σ_{i a position
of R} B(R[i])

Given a TGD tau in Sigma and blow-up function B, let B(tau) be the TGD
over the blown-up schema obtained by applying $B$ to each atom in tau:

Example: Suppose Sigma contains

tau: R(x,y) S(x) –> exists z R(y, z)

then S[1] R[1] and R[2] are all equivalent.  A valid schema blow-up B
might map the class with S[1], R[1], R[2] to 3.


The TGD B(tau) over the blown-up schema is:

R(x_1, x_2, x_3, y_1, y_2, y_3) S(x_1, x_2, x_3) –> exists z_1 z_2 z_3
R(y_1, y_2, y_3, z_1 , z_2, z_3)

Remarks:
- the arity blowup does not change the number of
TGDs we can derived, since the unifications between TGD heads and body
after blowup are isomorphic to the ones we can obtain between TGD
heads and bodies before blowup.
- Moreover, blowing up the derived TGDs from Σ returns the
set of derived TGDs from blown-up TGDs of Σ.

So in order to make the saturation task more challenging, we need to
introduce some complementary TGD modifications. First, we propose to
introduce some side atoms for each position, which will complicate the
evolve steps. Second, we propose to add permutation on the existential
variables introduced by the blow-up of a single position.

## Side atoms

To extend the blow-up, we could introduce some side atoms.  To do so,
we assume that a set of side predicates is available and is disjoint
from the predicates in S. We define N a function associating each
position R[i] with a side predicate and P a function associating each
position R[i] with a list of indexes in {1, …, B(R[i])} of length
equals to the arity of N(R[i]). P is used to select the variables used
in the side atom of a position among the one available at this
position after the blow-up.

For example, on the previous example, we can define:
- N by R[1] –> Q, R[2] –> T, S[1] –> T,
- P by R[1] –> [2,2,3], R[2] –> [1], S[1] –> [3],
the resulting TGD will be:

R(x_1, x_2, x_3, y_1, y_2, y_3), Q(x_2, x_2, x_3), T(y_1), S(x_1, x_2,
x_3), T(x_3) –> exists z_1 z_2 z_3 R(y_1, y_2, y_3, z_1 , z_2, z_3),
Q(y_2, y_2, y_3), T(z_1)


Remarks:
1. Let Σ be a set of TGDs, Σ_s the set of TGDs in Σ to which side
atoms have been added and Σ' and Σ_s' the respective set of derived
TGDs using one of our saturation algorithm. Σ' is included into
Σ_s' to which the side atoms are removed from each TGD. In other
words, it states that *adding side atoms does not reduce the
complexity of the saturation*.
2. Instead of adding an side atom for each position, we may add an
atom for each position *class*, meaning that the domain of R and P
would be the position classes. It would reduce the number of side
atoms added to each TGD, but it would increase the number of pair
of TGDs that share a side predicate.
3. While the diversity of possible shape for the side atoms increases
with the blow-up arity, the number of derived TGDs from TGDs with
side atoms seems independent of the blow-up arity. It is not the
case of techniques like variables permutation and unification.

I propose the following parameters for the creation of these side
atoms:

1. a parameter for associating the side atoms with the position class
instead of the position (default false)
2. the probability of adding a side atom to a position
3. the probability of reusing a side predicate when a new side atom is
introduced (default 0)
4. the minimum factor between B(R[i]) and N(R[i])
5. the maximum factor between B(R[i]) and N(R[i])


## Variables permutation

We introduce variables permutations in the blown-up TGDs, in order to
that the number of derived TGDs increases (possibily exponentially)
with the arity of the blowing-up i.e. the minimal value taken by B.

For a class of position R[i], we will select T(R[i]) a subset of the
transpositions of the indexes of the variables introduced during the
blowing-up, meaning the transpositions of {1, …, B(R[i])}. For each
TGD τ and each occurrence p of a position of the class R[i] in τ, we
build S(τ,p) a permutation of {1, …, B(R[i])} by composing some
transpositions from T(R[i]). We replace τ with the TGD obtained by
applying S(τ,p) to the indexes of the variables introduced by the
blowing-up at the position p only. We continue by consecutively
permutate the variables to every occurence of position of R[i] in τ
and obtain a TGD denoted τ_{S(R[i])}. And for each tranposition (k l)
in T(R[i]) and for each position P[j] in the class R[i], we add a
permutation TGD of the form:

P(α, y_1, …, y_k, …, y_l, …, y_{B(R[i])}, β) -> P(α, y_1,…, y_l, …,
y_k, …, y_{B(R[i])}, β)

Example: Consider the blown-up TGDs on the equivalent positions B[2]
and C[1], we will apply the variables permutation on this position
class:

1. τ = A(x) -> B(x, y_1, y_2, y_3), C(y_1, y_2, y_3, x)
2. τ' = B(u, v_1, v_2, v_3), C(v_1, v_2, v_3, u) -> D(u)

We select the transposition set T(R[i]) of {1, 2, 3} equals to {(1 2),
(1 3)}. For every p position in τ, we choose that S(τ, p) = id, for
the single occurence p_b of B[2] in τ' we choose S(τ', p_b) = (1 3) o
(1 2) = (2 3 1) and for the single occurence p_c of C[1] in τ' we
choose S(τ', p_c) = (1 2). So we obtain:

1. τ = A(x) -> B(x, y_1, y_2, y_3), C(y_1, y_2, y_3, x)
2. τ'_{S(B[2])} B(u, v_2, v_3, v_1), C(v_2, v_1, v_3, u) -> D(u)
3. B(u, v_1, v_2, v_3) -> B(u, v_2, v_1, v_3)
4. B(u, v_1, v_2, v_3) -> B(u, v_3, v_2, v_1)
5. C(v_1, v_2, v_3, u) -> C(v_2, v_1, v_3, u)
6. C(v_1, v_2, v_3, u) -> C(v_3, v_2, v_1, u)

We observe that we can not evolve (in GSat) τ and τ'_{S(B[2])}, while
we can evolve τ and τ'. But we can evolve first τ with 4., then 3. and
with 5. to obtain:

A(x) -> B(x, y_1, y_2, y_3), C(y_1, y_2, y_3, x), B(x, y_3, y_2,
y_1), B(x, y_2, y_3, y_1), C(y_2, y_1, y_3, x)

This last TGD can be evolved with τ'_{S(B[2])}.

Remarks:
1. Let τ be a TGD and R[i] a class of positions, if for every
occurrence p of a position of the R[i] in τ, S(τ, p) always is the
same permutation, then τ τ_{S(R[i]) }are the isomorphical. In
particular, if there is only one ocurrence of a position of R[i] in
τ, τ and τ_{S(R[i])} are isomorphical.
2. As state for the side atoms addition, applying variables
permutation do not reduce the complexity of the saturation.

The parameters for the variables permutation:
- the number of transposition in T(R[i]) (remark: n-1 transpositions
can generated all the permutation set by composition e.g. (1 2), (1
3), …, (1 n))
- the number of compositions of transpositions from T(R[i]) used to
build each S(τ, p)
