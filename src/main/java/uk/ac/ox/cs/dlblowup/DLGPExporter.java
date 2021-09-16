package uk.ac.ox.cs.dlblowup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Query;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.io.ParseException;
import fr.lirmm.graphik.graal.api.io.Parser;
import fr.lirmm.graphik.graal.core.factory.DefaultAtomFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultAtomSetFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultRuleFactory;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;
import fr.lirmm.graphik.util.Prefix;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;
import fr.lirmm.graphik.util.stream.Stream;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;


public class DLGPExporter {

    public static void printDLGP(Collection<? extends TGD> tgds, OutputStream stream) throws IOException {
        DlgpWriter writer = new DlgpWriter(stream);

        for (TGD tgd : tgds)
            writer.write(createRule(tgd));

        writer.close();
    }
    
    public static Rule createRule(TGD tgd) {
        return DefaultRuleFactory.instance().create(createAtomSet(tgd.getBodyAtoms()), createAtomSet(tgd.getHeadAtoms()));
    }
    
    public static Atom[] createAtomSet(uk.ac.ox.cs.pdq.fol.Atom[] atoms) {
        List<Atom> graalAtoms = Arrays.stream(atoms).map(a -> createAtom(a)).collect(Collectors.toList());
        return graalAtoms.toArray(new Atom[0]);
    }

    public static Atom createAtom(uk.ac.ox.cs.pdq.fol.Atom atom) {
        java.util.stream.Stream<uk.ac.ox.cs.pdq.fol.Term> stream = Arrays.stream(atom.getTerms());
        List<Term> terms = stream.map(t -> createTerm(t)).collect(Collectors.toList());
        return DefaultAtomFactory.instance().create(createPredicate(atom.getPredicate()), terms);
    }
    
    public static Predicate createPredicate(uk.ac.ox.cs.pdq.fol.Predicate predicate) {
        return new Predicate(predicate.getName(), predicate.getArity());
    }

    public static Term createTerm(uk.ac.ox.cs.pdq.fol.Term term) {
        if (term.isVariable()) {
            return DefaultTermFactory.instance().createVariable(((Variable) term).getSymbol());
        } else {
            String message = "The type of term "+ term.getClass() +" is not supported: " + term;
            throw new NotImplementedException(message);
        }
    }
}
