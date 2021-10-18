package uk.ac.ox.cs.dlblowup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.cs.gsat.DLGPIO;
import uk.ac.ox.cs.gsat.GTGD;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.Term;

/**
 * Arity blow-up for any guarded tgd set
 *
 */
public class App {

    private static final String GENERATED_PRED_NAME = "GEN_";
    private static int generatedPredIndex;

    public static void main(String[] args) throws Exception {
        String inputPath;
        String outputPath = null;
        if (args.length > 0) {
            inputPath = args[0];
            if (args.length > 1)
                outputPath = args[1];
        } else {
            System.out.println("the first argument is the input DLGP file");
            System.out.println("(optional) the second argument is output path");
            return;
        }

        // configuration loading
        Configuration.initialize();

        // we define the output as file or as the standard output
        OutputStream outputStream;
        if (outputPath == null)
            outputStream = System.out;
        else
            outputStream = new FileOutputStream(outputPath);
        // we parse the intput
        DLGPIO io = new DLGPIO(inputPath, false);

        Collection<Dependency> dependencies = io.getRules();
        Collection<GTGD> tgds = new HashSet<>();
        Collection<Predicate> predicates = new HashSet<>();
        Collection<Predicate> guardPredicates = new HashSet<>();

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

        Collection<GTGD> blownup = new HashSet<>();
        ArityBlowUp blowUp = new ArityBlowUp();
        for (GTGD tgd : tgds) {
            blownup.add(blowUp.blowUpTGD(tgd));
        }

        // create the side atoms
        Collection<GTGD> tgdWithSideAtoms = new HashSet<>();
        Collection<Predicate> predicatesWithSideAtoms = guardPredicates.stream()
                .map(p -> blowUp.getBlownPredicateFromOriginal().get(p)).collect(Collectors.toSet());

        List<Predicate> availableSidePredicates = new ArrayList<>();
        if (Configuration.getReuseOriginalPredicates())
            availableSidePredicates.addAll(blowUp.getBlownPredicateFromOriginal().values());
        
        Map<Predicate, List<Predicate>> sidePredicatesMap = new HashMap<>();
        Map<Predicate, List<List<Integer>>> sideTermMappingMap = new HashMap<>();

        for (GTGD tgd : blownup) {
            Atom guard = tgd.getGuard();
            Set<Atom> bodyWithSideAtoms = insertSideAtoms(guard, new HashSet<>(tgd.getBodySet()),
                    predicatesWithSideAtoms, sidePredicatesMap, sideTermMappingMap, availableSidePredicates);
            Set<Atom> headWithSideAtoms = insertSideAtoms(tgd.getHeadSet(), predicatesWithSideAtoms, sidePredicatesMap,
                    sideTermMappingMap, availableSidePredicates);
            tgdWithSideAtoms.add(new GTGD(bodyWithSideAtoms, headWithSideAtoms));
        }

        Collection<GTGD> permutedTGDs = VariablePermutation.apply(tgdWithSideAtoms, predicatesWithSideAtoms);
        
        DLGPExporter.printDLGP(permutedTGDs, outputStream);
    }

    private static Set<Atom> insertSideAtoms(Set<Atom> atoms, Collection<Predicate> predicatesWithSideAtoms,
            Map<Predicate, List<Predicate>> sidePredicatesMap, Map<Predicate, List<List<Integer>>> sideTermMappingMap,
            List<Predicate> availableSidePredicates) {
        Set<Atom> atomsWithSide = new HashSet<>(atoms);
        for (Atom atom : atoms) {
            insertSideAtoms(atom, atomsWithSide, predicatesWithSideAtoms, sidePredicatesMap, sideTermMappingMap,
                    availableSidePredicates);
        }
        return atomsWithSide;
    }

    private static Set<Atom> insertSideAtoms(Atom atomWithSide, Set<Atom> atoms,
            Collection<Predicate> predicatesWithSideAtoms, Map<Predicate, List<Predicate>> sidePredicatesMap,
            Map<Predicate, List<List<Integer>>> sideTermMappingMap, List<Predicate> availableSidePredicates) {
        Predicate predicate = atomWithSide.getPredicate();
        if (predicatesWithSideAtoms.contains(predicate)) {
            List<Predicate> sidePredicates;
            List<List<Integer>> sideTermMapping = new ArrayList<>();
            if (sidePredicatesMap.containsKey(predicate)) {
                sidePredicates = sidePredicatesMap.get(predicate);
                sideTermMapping = sideTermMappingMap.get(predicate);
            } else {
                sidePredicates = createSidePredicates(predicate, availableSidePredicates);
                sideTermMapping = createSideTermMapping(predicate, sidePredicates);
                sidePredicatesMap.put(predicate, sidePredicates);
                sideTermMappingMap.put(predicate, sideTermMapping);
            }

            // insertion of the side atoms
            for (int i = 0; i < sidePredicates.size(); i++) {
                atoms.add(Atom.create(sidePredicates.get(i),
                        getMappedTerms(atomWithSide.getTerms(), sideTermMapping.get(i))));
            }
        }
        return atoms;
    }

    private static Term[] getMappedTerms(Term[] terms, List<Integer> list) {
        Term[] mappedTerms = new Term[list.size()];

        int current = 0;
        for (Integer pos : list) {
            mappedTerms[current] = terms[pos];
            current++;
        }
        return mappedTerms;
    }

    private static List<List<Integer>> createSideTermMapping(Predicate predicate, List<Predicate> sidePredicates) {
        List<List<Integer>> sideTermMapping = new ArrayList<>();

        for (Predicate sidePredicate : sidePredicates) {
            List<Integer> mapping = new ArrayList<>();
            for (int pos = 0; pos < sidePredicate.getArity(); pos++) {
                mapping.add((int) Math.floor(Math.random() * predicate.getArity()));
            }
            sideTermMapping.add(mapping);
        }

        return sideTermMapping;
    }

    private static List<Predicate> createSidePredicates(Predicate predicate, List<Predicate> availableSidePredicates) {
        List<Predicate> sidePredicates = new ArrayList<>();
        int numberOfSideAtoms = (int) Math.round(Math.random() * predicate.getArity());

        for (int i = 0; i < numberOfSideAtoms; i++) {
            Predicate sidePredicate;
            // flip a count to generate a new predicate or reusing a existing one
            if (availableSidePredicates.isEmpty() || Math.random() > Configuration.getProbabilityOfReusingSidePredicate()) {
                sidePredicate = generatePredicate(predicate.getArity());
            } else {
                Collections.shuffle(availableSidePredicates);
                int pos = 0;
                Predicate candidate = availableSidePredicates.get(pos);

                while ((candidate.equals(predicate) || candidate.getArity() > predicate.getArity())
                        && pos < availableSidePredicates.size() - 1) {
                    candidate = availableSidePredicates.get(++pos);
                }
                if (pos < availableSidePredicates.size())
                    sidePredicate = candidate;
                else
                    sidePredicate = generatePredicate(predicate.getArity());
            }
            sidePredicates.add(sidePredicate);
            availableSidePredicates.add(sidePredicate);
        }

        return sidePredicates;
    }

    private static Predicate generatePredicate(int maxArity) {
        int arity = (int) Math.floor(Math.random() * maxArity) + 1;
        return Predicate.create(GENERATED_PRED_NAME + (generatedPredIndex++), arity);
    }

    public static class Configuration {
        private static final String file = "config.properties";
        private static Properties prop = new Properties();

        private static synchronized void initialize() {

            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(Configuration.file);
                Configuration.prop.load(inStream);
            } catch (final IOException e) {
                System.err.println("Could not open configuration file.");
                System.err.println(e.toString());
                System.err.println("Falling back to defaults.");
            } finally {
                if (inStream != null)
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        public static int getBlowUpSize() {
            if (Configuration.prop.containsKey("blow_up.size")) {
                return Integer.parseInt(Configuration.prop.getProperty("blow_up.size"));
            } else {
                return 5;
            }
        }

        public static double getProbabilityOfReusingSidePredicate() {
            if (Configuration.prop.containsKey("blow_up.probability_of_reusing_side_predicate")) {
                return Double
                        .parseDouble(Configuration.prop.getProperty("blow_up.probability_of_reusing_side_predicate"));
            } else {
                return 0.5;
            }
        }

        public static boolean getReuseOriginalPredicates() {
            if (Configuration.prop.containsKey("blow_up.reuse_original_predicates")) {
                return Boolean
                        .parseBoolean(Configuration.prop.getProperty("blow_up.reuse_original_predicates"));
            } else {
                return true;
            }
        }

        public static double getTranspositionFactor() {
            if (Configuration.prop.containsKey("blow_up.transposition_factor")) {
                return Double
                        .parseDouble(Configuration.prop.getProperty("blow_up.transposition_factor"));
            } else {
                return 0.5;
            }
        }

        public static double getTranspositionMaxNumber() {
            if (Configuration.prop.containsKey("blow_up.transposition_max")) {
                return Double
                        .parseDouble(Configuration.prop.getProperty("blow_up.transposition_max"));
            } else {
                return 1;
            }
        }

    }

}
