package uk.ac.ox.cs.dlblowup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.dlblowup.App.Configuration;
import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

public class ArityBlowUp {

    private static final String PREDICATE_PREFIX = "BLOW_";
    private final Map<Predicate, Predicate> blownPredicateFromOriginal = new HashMap<>();

    public ArityBlowUp() {
    }

    public Map<Predicate, Predicate> getBlownPredicateFromOriginal() {
        return blownPredicateFromOriginal;
    }

    public GTGD blowUpTGD(GTGD tgd) {
        Set<Atom> blownBody = getBlownAtoms(tgd.getBodySet());
        Set<Atom> blownHead = getBlownAtoms(tgd.getHeadSet());

        GTGD blownTgd = new GTGD(blownBody, blownHead);
        return blownTgd;
    }

    public Set<Atom> getBlownAtoms(Collection<Atom> atoms) {
        int blowUpSize = Configuration.getBlowUpSize();
        Set<Atom> blownAtoms = new HashSet<>();
        for (Atom atom : atoms) {
            Predicate blownPredicate = createBlownUpPredicate(atom.getPredicate(), blowUpSize);
            blownPredicateFromOriginal.put(atom.getPredicate(), blownPredicate);
            Term[] blownterms = new Term[blowUpSize * atom.getPredicate().getArity()];
            for (int pos = 0; pos < atom.getPredicate().getArity(); pos++) {
                if (atom.getTerm(pos).isVariable()) {
                    Variable variable = (Variable) atom.getTerm(pos);
                    int count = 0;
                    for (int blownPos = blowUpSize * pos; blownPos < blowUpSize * (pos + 1); blownPos++) {
                        blownterms[blownPos] = new Variable(variable.getSymbol() + "_" + count);
                        count++;
                    }
                } else {
                    Constant constant = (Constant) atom.getTerm(pos);
                    for (int blownPos = blowUpSize * pos; blownPos < blowUpSize * (pos + 1); blownPos++)
                        blownterms[blownPos] = constant;

                    //                    System.out.println("The constants are not yet supported, but one is encountered in:\n " + atom);
                    //System.exit(1);
                }
            }
            blownAtoms.add(Atom.create(blownPredicate, blownterms));
        }
        return blownAtoms;
    }

    private Predicate createBlownUpPredicate(Predicate predicate, int blownUpSize) {
        return Predicate.create(PREDICATE_PREFIX + predicate.getName(), blownUpSize * predicate.getArity());
    }

}
