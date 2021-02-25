package gumtree.spoon.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.VersionedEditScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.MultiVersionMappingStore;
import com.github.gumtreediff.matchers.SingleVersionMappingStore;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Version;
import com.github.gumtreediff.tree.VersionedTree;

import gnu.trove.map.TIntObjectMap;
import gumtree.spoon.apply.operations.MyScriptGenerator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.OperationKind;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.declaration.CtElement;

/**
 * @author Matias Martinez, matias.martinez@inria.fr
 */
public class MultiDiffImpl implements Diff {
	// /**
	//  * Actions over all tree nodes (CtElements)
	//  */
	// private final List<Operation> allOperations;
	// /**
	//  * Actions over the changes roots.
	//  */
	// private final List<Operation> rootOperations;
	// /**
	//  * the mapping of this diff
	//  */
	// private final MappingStore _mappingsComp;
	// /**
	//  * Context of the spoon diff.
	//  */
	// private final TreeContext context;
	private ITree lastSpooned;
	private List<Diff> diffs = new ArrayList<>();
	AbstractVersionedTree middle;
	private Version lastVersion;
	private Map<Version, Map<ITree, AbstractVersionedTree>> mappingPerVersion = new HashMap<>();

	public Map<ITree, AbstractVersionedTree> getMapping(Version version) {
		Map<ITree, AbstractVersionedTree> r = mappingPerVersion.get(version);
		if (r == null) {
			throw new RuntimeException("map with keys: " + mappingPerVersion.keySet() + " does not contain " + version);
		}
		return Collections.unmodifiableMap(r);
	}
	
	private final MyScriptGenerator actionGenerator;

	public AbstractVersionedTree getMiddle() {
		return middle;
	}

	public MultiDiffImpl(ITree initialSpooned, Version version) {
		this.lastSpooned = initialSpooned;
		this.lastVersion = version;
		VersionedTree.AVTfromITreeAlongSpoon avTfromITreeAlongSpoon = new VersionedTree.AVTfromITreeAlongSpoon(
				initialSpooned);
		middle = avTfromITreeAlongSpoon.cloned;

		this.mappingPerVersion.put(version, avTfromITreeAlongSpoon.getMappingFromOri());
		this.actionGenerator = new MyScriptGenerator(middle, version, this.mappingPerVersion, getGlobalGranularity());
	}

	public DiffImpl compute(ITree rootSpoonRight, Version versionRight) {
		ITree rootSpoonLeft = this.lastSpooned;
		Version rootVersionLeft = this.lastVersion;
		this.lastSpooned = rootSpoonRight;
		this.lastVersion = versionRight;
		this.mappingPerVersion.put(versionRight, new HashMap<>());
		DiffImpl r = new DiffImpl(actionGenerator, rootSpoonLeft,
				rootSpoonRight, rootVersionLeft, versionRight);
		diffs.add(r);
		return r;
	}

	private MyScriptGenerator.Granularity getGlobalGranularity() {
		String b = System.getProperty("gumtree.ganularity"); // b != null && b.equals("true")
		MyScriptGenerator.Granularity moveMod;
		if (b != null && b.equals("splited")) {
			moveMod = MyScriptGenerator.Granularity.SPLITED;
		} else if (b != null && b.equals("atomic")) {
			moveMod = MyScriptGenerator.Granularity.ATOMIC;
		} else if (b != null && b.equals("compose")) {
			moveMod = MyScriptGenerator.Granularity.COMPOSE;
		} else {
			moveMod = MyScriptGenerator.Granularity.SPLITED;
		}
		return moveMod;
	}

	@Override
	public List<Operation> getAllOperations() {
		List<Operation> r = new ArrayList<>();
		for (Diff diff : diffs) {
			r.addAll(diff.getAllOperations());
		}
		return Collections.unmodifiableList(r);
	}

	@Override
	public List<Operation> getRootOperations() {
		List<Operation> r = new ArrayList<>();
		for (Diff diff : diffs) {
			r.addAll(diff.getRootOperations());
		}
		return Collections.unmodifiableList(r);
	}

	@Override
	public List<Operation> getOperationChildren(Operation operationParent, List<Operation> rootOperations) {
		return rootOperations.stream() //
				.filter(operation -> operation.getNode().getParent().equals(operationParent)) //
				.collect(Collectors.toList());
	}

	@Override
	public CtElement changedNode() {
		throw new UnsupportedOperationException("Not implemented yet");
		// if (rootOperations.size() != 1) {
		// 	throw new IllegalArgumentException("Should have only one root action.");
		// }
		// return commonAncestor();
	}

	@Override
	public CtElement commonAncestor() {
		throw new UnsupportedOperationException("Not implemented yet");
		// final List<CtElement> copy = new ArrayList<>();
		// for (Operation<?> operation : rootOperations) {
		// 	CtElement el = operation.getSrcNode();
		// 	if (operation instanceof InsertOperation) {
		// 		// we take the corresponding node in the source tree
		// 		el = (CtElement) _mappingsComp.getSrc(operation.getAction().getNode().getParent())
		// 				.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		// 	}
		// 	copy.add(el);
		// }
		// while (copy.size() >= 2) {
		// 	CtElement first = copy.remove(0);
		// 	CtElement second = copy.remove(0);
		// 	copy.add(commonAncestor(first, second));
		// }
		// return copy.get(0);
	}

	// private CtElement commonAncestor(CtElement first, CtElement second) {
	// 	while (first != null) {
	// 		CtElement el = second;
	// 		while (el != null) {
	// 			if (first == el) {
	// 				return first;
	// 			}
	// 			el = el.getParent();
	// 		}
	// 		first = first.getParent();
	// 	}
	// 	return null;
	// }

	@Override
	public CtElement changedNode(Class<? extends Operation> operationWanted) {
		throw new UnsupportedOperationException("Not implemented yet");
		// final Optional<Operation> firstNode = rootOperations.stream() //
		// 		.filter(operation -> operationWanted.isAssignableFrom(operation.getClass())) //
		// 		.findFirst();
		// if (firstNode.isPresent()) {
		// 	return firstNode.get().getNode();
		// }
		// throw new NoSuchElementException();
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind) {
		throw new UnsupportedOperationException("Not implemented yet");
		// return rootOperations.stream() //
		// 		.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
		// 				&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind));
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind, String nodeLabel) {
		return containsOperations(getRootOperations(), kind, nodeKind, nodeLabel);
	}

	@Override
	public boolean containsOperations(List<Operation> operations, OperationKind kind, String nodeKind,
			String nodeLabel) {
		throw new UnsupportedOperationException("Not implemented yet");
		// return operations.stream()
		// 		.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
		// 				&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind)
		// 				&& operation.getAction().getNode().getLabel().equals(nodeLabel));
	}

	@Override
	public boolean containsOperations(OperationKind kind, String nodeKind, String nodeLabel, String newLabel) {
		throw new UnsupportedOperationException("Not implemented yet");
		// if (kind != OperationKind.Update) {
		// 	throw new IllegalArgumentException();
		// }
		// return getRootOperations().stream().anyMatch(operation -> operation instanceof UpdateOperation //
		// 		&& ((UpdateOperation) operation).getAction().getNode().getLabel().equals(nodeLabel)
		// 		&& ((UpdateOperation) operation).getAction().getValue().equals(newLabel)

		// );
	}

	@Override
	public void debugInformation() {
		System.err.println(toDebugString());
	}

	private String toDebugString() {
		throw new UnsupportedOperationException("Not implemented yet");

		// return toDebugString(rootOperations);
	}

	private String toDebugString(List<Operation> ops) {
		throw new UnsupportedOperationException("Not implemented yet");
		// String result = "";
		// for (Operation operation : ops) {
		// 	ITree node = operation.getAction().getNode();
		// 	final CtElement nodeElement = operation.getSrcNode();
		// 	String nodeType = context.getTypeLabel(node.getType());
		// 	if (nodeElement != null) {
		// 		nodeType += "(" + nodeElement.getClass().getSimpleName() + ")";
		// 	}
		// 	result += "OperationKind." + operation.getAction().getClass().getSimpleName() + ", \"" + nodeType + "\", \""
		// 			+ node.getLabel() + "\"";

		// 	if (operation instanceof UpdateOperation) {
		// 		// adding the new value for update
		// 		result += ",  \"" + ((Update) operation.getAction()).getValue() + "\"";
		// 	}

		// 	result += " (size: " + node.getDescendants().size() + ")" + node.toTreeString();
		// }
		// return result;
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException("Not implemented yet");
		// if (rootOperations.size() == 0) {
		// 	return "no AST change";
		// }
		// final StringBuilder stringBuilder = new StringBuilder();
		// final CtElement ctElement = commonAncestor();

		// for (Operation operation : rootOperations) {
		// 	stringBuilder.append(operation.toString());

		// 	// if all actions are applied on the same node print only the first action
		// 	if (operation.getSrcNode().equals(ctElement) && operation instanceof UpdateOperation) {
		// 		break;
		// 	}
		// }
		// return stringBuilder.toString();
	}

	public TreeContext getContext() {
		throw new UnsupportedOperationException("Not implemented yet");
		// return context;
	}

	public MappingStore getMappingsComp() {
		throw new UnsupportedOperationException("Not implemented yet");
		// return _mappingsComp;
	}
}
