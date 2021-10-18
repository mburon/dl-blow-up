package uk.ac.ox.cs.dlblowup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.dlblowup.App.Configuration;
import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class VariablePermutation {
    private static String variablePrefix = "X_";

    public static Set<GTGD> apply(Collection<GTGD> tgds, Collection<Predicate> permutedPredications) {
        Set<GTGD> result = new HashSet<>();
        // transpositions per predicates block of terms
        Map<Predicate, List<List<List<Integer>>>> predicateBlockTranspositions = new HashMap<>();
        int blowSize = Configuration.getBlowUpSize();

        // generate the transposition per predicate block
        for (Predicate pred : permutedPredications) {
            predicateBlockTranspositions.put(pred, new ArrayList<>());
            for (int blowBlock = 0; blowSize * blowBlock < pred.getArity(); blowBlock++) {
                predicateBlockTranspositions.get(pred).add(createTranspositions(blowSize, blowSize * blowBlock));
            }
        }

        // apply permutation based on the transpositions on the head atoms in the TGDs
        for (GTGD tgd : tgds) {
            Set<Atom> body = tgd.getBodySet();
            Set<Atom> head = new HashSet<>();
            for (Atom atom : tgd.getHeadSet()) {
                if (permutedPredications.contains(atom.getPredicate())) {
                    List<List<List<Integer>>> transpositionsPerBlock = predicateBlockTranspositions
                            .get(atom.getPredicate());
                    Atom permutedAtom = atom;
                    for (List<List<Integer>> blockTranspositions : transpositionsPerBlock) {
                        permutedAtom = applyPermutation(permutedAtom, blockTranspositions);
                    }
                    head.add(permutedAtom);
                } else {
                    head.add(atom);
                }
            }
            result.add(new GTGD(body, head));
        }

        // add the transposition rules
        for (Predicate pred : predicateBlockTranspositions.keySet()) {
            for (List<List<Integer>> blockTranspositions : predicateBlockTranspositions.get(pred)) {
                for (List<Integer> transposition : blockTranspositions) {
                    Term[] vars = new Term[pred.getArity()];
                    for (int pos = 0; pos < pred.getArity(); pos++) {
                        vars[pos] = Variable.create(variablePrefix + pos);
                    }
                    Set<Atom> body = Set.of(Atom.create(pred, vars));
                    Term tmp = vars[transposition.get(0)];
                    vars[transposition.get(0)] = vars[transposition.get(1)];
                    vars[transposition.get(1)] = tmp;
                    Set<Atom> head = Set.of(Atom.create(pred, vars));
                    result.add(new GTGD(body, head));
                }
            }
        }

        return result;
    }

    private static Atom applyPermutation(Atom atom, List<List<Integer>> transpositions) {
        Term[] terms = atom.getTerms();

        int transpositionNumber = (int) Math.ceil(Configuration.getTranspositionMaxNumber() * Math.random());

        if (transpositions.isEmpty())
            return atom;

        for (int i = 0; i < transpositionNumber; i++) {
            int transpositionIndex = (int) Math.floor(Math.random() * transpositions.size());
            List<Integer> transposition = transpositions.get(transpositionIndex);
            Term tmp = terms[transposition.get(0)];
            terms[transposition.get(0)] = terms[transposition.get(1)];
            terms[transposition.get(1)] = tmp;
        }

        return Atom.create(atom.getPredicate(), terms);
    }

    private static List<List<Integer>> createTranspositions(int size, int offset) {
        List<List<Integer>> results = new ArrayList<>();
        int transpositionsNumber = (int) Math
                .ceil(Configuration.getBlowUpSize() * Configuration.getTranspositionFactor() * Math.random()) - 1;

        for (int pos = 0; pos < transpositionsNumber; pos++) {
            List<Integer> transposition = new ArrayList<>();
            transposition.add(offset);
            transposition.add(offset + pos + 1);
            results.add(transposition);
        }

        return results;
    }
}
