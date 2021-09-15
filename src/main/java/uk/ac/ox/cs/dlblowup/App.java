package uk.ac.ox.cs.dlblowup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.gsat.DLGPIO;
import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {
        String path;
        if (args.length > 0) {
            path = args[0];
        } else {
            System.out.println("the first input in the DLGP file");
            return;
        }

        DLGPIO io = new DLGPIO(path, false);

        Collection<Dependency> dependencies = io.getRules();
        Collection<GTGD> tgds = new HashSet<>();
        Collection<Predicate> predicates = new HashSet<>();
        Collection<Predicate> guardPredicates = new HashSet<>();
        Collection<Predicate> blownPredicates = new HashSet<>();
        Collection<Dependency> blownup = new HashSet<>();

        for (Dependency dependency : dependencies) {
            if (dependency instanceof TGD) {
                TGD tgd = (TGD) dependency;
                if (tgd.isGuarded()) {
                    for (Atom atom : tgd.getAtoms())
                        predicates.add(atom.getPredicate());
                    GTGD gtgd = new GTGD(Set.of(tgd.getBodyAtoms()), Set.of(tgd.getHeadAtoms()));

                    tgds.add(gtgd);
                    guardPredicates.add(gtgd.getGuard().getPredicate());
                } else {
                    System.out.println("Encounter a input not guarded TGD: \n" + tgd);
                    System.exit(1);
                }
            } else {
                System.out.println("The input contains a dependency, which is not TGD :\n" + dependency);
                System.exit(1);
            }
        }

        // Apply the blow-up
        Map<Predicate, Predicate> blownPredicateFromOriginal = new HashMap<>();

        for (GTGD tgd : tgds) {
            Set<Atom> blownBody = getBlownAtoms(tgd.getBodySet());
            Set<Atom> blownHead = getBlownAtoms(tgd.getHeadSet());
            
            System.out.println(tgd);
            TGD blownTgd = new GTGD(blownBody, blownHead);
            System.out.println(blownTgd);
        }

    }

    public static Set<Atom> getBlownAtoms(Collection<Atom> atoms) {
        int blowUpSize = Configuration.getBlowUpSize();
        Set<Atom> blownAtoms = new HashSet<>();
        for (Atom atom : atoms) {
            Predicate blownPredicate = new BlownUpPredicate(atom.getPredicate(), blowUpSize);
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
                    System.out.println("The constants are not yet supported, but one is encountered in:\n " + atom);
                    System.exit(1);
                }
            }
            blownAtoms.add(Atom.create(blownPredicate, blownterms));
        }
        return blownAtoms;
    }

    public static class Configuration {

        public static int getBlowUpSize() {
            return 2;
        }
    }

    public static class BlownUpPredicate extends Predicate {

        private static final String PREDICATE_PREFIX = "BLOW_";
        private final Predicate initPredicate;

        public BlownUpPredicate(Predicate predicate, int blownUpSize) {
            super(PREDICATE_PREFIX + predicate.getName(), blownUpSize * predicate.getArity());

            this.initPredicate = predicate;
        }

        public Predicate getInitPredicate() {
            return initPredicate;
        }

    }
}
