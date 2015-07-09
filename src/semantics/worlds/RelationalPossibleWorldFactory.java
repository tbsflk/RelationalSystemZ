package semantics.worlds;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import edu.cs.ai.log4KR.relational.classicalLogic.semantics.RelationalPossibleWorldMapRepresentationFactory;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;

/**
 * Extension of {@link RelationalPossibleWorldMapRepresentationFactory},
 * changing the creation process of possible worlds in such a way that the order
 * of the interpretables is retained.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalPossibleWorldFactory extends RelationalPossibleWorldMapRepresentationFactory {

	public RelationalPossibleWorld[] createPossibleWorlds(Collection<RelationalAtom> atoms, Collection<Constant> domain) {
		// use linked list with order
		LinkedList<RelationalPossibleWorld> worlds = new LinkedList<RelationalPossibleWorld>();
		worlds.add(this.createEmptyWorld(atoms, domain));
		for (RelationalAtom atom : atoms) {
			LinkedList<RelationalPossibleWorld> newWorlds = new LinkedList<RelationalPossibleWorld>();
			for (RelationalPossibleWorld partialWorld : worlds) {
				for (int i = 0; i < atom.getNoInterpretations(); i++) {
					newWorlds.add(this.extendWorld(partialWorld, atom, i, domain));
				}
			}
			worlds = newWorlds;
		}
		return worlds.toArray(new RelationalPossibleWorld[worlds.size()]);
	}

	protected RelationalPossibleWorld createEmptyWorld(Collection<RelationalAtom> atoms, Collection<Constant> universe) {
		return new RelationalPossibleWorld(new HashMap<RelationalAtom, Integer>(), atoms, universe);
	}

	protected RelationalPossibleWorld extendWorld(RelationalPossibleWorld partialWorld, RelationalAtom atom, int value, Collection<Constant> universe) {
		RelationalPossibleWorld newWorld = new RelationalPossibleWorld(partialWorld, universe);
		newWorld.getInterpretation().put(atom, value);
		return newWorld;
	}

}
