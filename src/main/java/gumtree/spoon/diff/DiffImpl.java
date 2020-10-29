package gumtree.spoon.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
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
public class DiffImpl implements Diff {
	/**
	 * Actions over all tree nodes (CtElements)
	 */
	private final List<Operation> allOperations;
	/**
	 * Actions over the changes roots.
	 */
	private final List<Operation> rootOperations;
	/**
	 * the mapping of this diff
	 */
	private final MappingStore _mappingsComp;
	/**
	 * Context of the spoon diff.
	 */
	private final TreeContext context;
	private List<Action> actionsList;

	public List<Action> getActionsList() {
		return actionsList;
	}

	DiffImpl(AbstractVersionedTree middle, MultiVersionMappingStore multiMappingsComp, TreeContext context,
			ITree rootSpoonLeft, ITree rootSpoonRight, Version beforeVersion, Version afterVersion) {
		if (context == null) {
			throw new IllegalArgumentException();
		}
		final MappingStore mappingsComp = new SingleVersionMappingStore<ITree, ITree>(rootSpoonLeft, rootSpoonRight);
		this.context = context;

		final Matcher matcher = new CompositeMatchers.ClassicGumtree(rootSpoonLeft, rootSpoonRight, mappingsComp);
		matcher.match();

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
		final EditScriptGenerator actionGenerator = new MyScriptGenerator(middle, multiMappingsComp, moveMod);

		EditScript actions = actionGenerator.computeActions(matcher,beforeVersion,afterVersion);
		this.actionsList = actions.asList();

		ActionClassifier actionClassifier = new ActionClassifier(multiMappingsComp.asSet(), actionsList);
		// Bugfix: the Action classifier must be executed *BEFORE* the convertToSpoon
		// because it writes meta-data on the trees
		this.rootOperations = wrapSpoon(actionClassifier.getRootActions());
		this.allOperations = wrapSpoon(actionsList);

		this._mappingsComp = mappingsComp;

		for (int i = 0; i < this.getAllOperations().size(); i++) {
			Operation operation = this.getAllOperations().get(i);
			if (operation instanceof MoveOperation) {
				if (operation.getSrcNode() != null) {
					operation.getSrcNode().putMetadata("isMoved", true);
				}
				if (operation.getDstNode() != null) {
					operation.getDstNode().putMetadata("isMoved", true);
				}
			}
		}
	}

	private List<Operation> wrapSpoon(List<Action> actions) {
		List<Operation> collect = actions.stream().map(action -> {
			if (action instanceof Insert) {
				return new InsertOperation((Insert) action);
			} else if (action instanceof Delete) {
				return new DeleteOperation((Delete) action);
			} else if (action instanceof Update) {
				return new UpdateOperation((Update) action);
			} else if (action instanceof Move) {
				return new MoveOperation((Move) action);
			} else {
				throw new IllegalArgumentException("Please support the new type " + action.getClass());
			}
		}).collect(Collectors.toList());
		return collect;
	}

	@Override
	public List<Operation> getAllOperations() {
		return Collections.unmodifiableList(allOperations);
	}

	@Override
	public List<Operation> getRootOperations() {
		return Collections.unmodifiableList(rootOperations);
	}

	@Override
	public List<Operation> getOperationChildren(Operation operationParent, List<Operation> rootOperations) {
		return rootOperations.stream() //
				.filter(operation -> operation.getNode().getParent().equals(operationParent)) //
				.collect(Collectors.toList());
	}

	@Override
	public CtElement changedNode() {
		if (rootOperations.size() != 1) {
			throw new IllegalArgumentException("Should have only one root action.");
		}
		return commonAncestor();
	}

	@Override
	public CtElement commonAncestor() {
		final List<CtElement> copy = new ArrayList<>();
		for (Operation<?> operation : rootOperations) {
			CtElement el = operation.getSrcNode();
			if (operation instanceof InsertOperation) {
				// we take the corresponding node in the source tree
				el = (CtElement) _mappingsComp.getSrc(operation.getAction().getNode().getParent())
						.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			}
			copy.add(el);
		}
		while (copy.size() >= 2) {
			CtElement first = copy.remove(0);
			CtElement second = copy.remove(0);
			copy.add(commonAncestor(first, second));
		}
		return copy.get(0);
	}

	private CtElement commonAncestor(CtElement first, CtElement second) {
		while (first != null) {
			CtElement el = second;
			while (el != null) {
				if (first == el) {
					return first;
				}
				el = el.getParent();
			}
			first = first.getParent();
		}
		return null;
	}

	@Override
	public CtElement changedNode(Class<? extends Operation> operationWanted) {
		final Optional<Operation> firstNode = rootOperations.stream() //
				.filter(operation -> operationWanted.isAssignableFrom(operation.getClass())) //
				.findFirst();
		if (firstNode.isPresent()) {
			return firstNode.get().getNode();
		}
		throw new NoSuchElementException();
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind) {
		return rootOperations.stream() //
				.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
						&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind));
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind, String nodeLabel) {
		return containsOperations(getRootOperations(), kind, nodeKind, nodeLabel);
	}

	@Override
	public boolean containsOperations(List<Operation> operations, OperationKind kind, String nodeKind,
			String nodeLabel) {
		return operations.stream()
				.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
						&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind)
						&& operation.getAction().getNode().getLabel().equals(nodeLabel));
	}

	@Override
	public boolean containsOperations(OperationKind kind, String nodeKind, String nodeLabel, String newLabel) {
		if (kind != OperationKind.Update) {
			throw new IllegalArgumentException();
		}
		return getRootOperations().stream().anyMatch(operation -> operation instanceof UpdateOperation //
				&& ((UpdateOperation) operation).getAction().getNode().getLabel().equals(nodeLabel)
				&& ((UpdateOperation) operation).getAction().getValue().equals(newLabel)

		);
	}

	@Override
	public void debugInformation() {
		System.err.println(toDebugString());
	}

	private String toDebugString() {
		return toDebugString(rootOperations);
	}

	private String toDebugString(List<Operation> ops) {
		String result = "";
		for (Operation operation : ops) {
			ITree node = operation.getAction().getNode();
			final CtElement nodeElement = operation.getSrcNode();
			String nodeType = context.getTypeLabel(node.getType());
			if (nodeElement != null) {
				nodeType += "(" + nodeElement.getClass().getSimpleName() + ")";
			}
			result += "OperationKind." + operation.getAction().getClass().getSimpleName() + ", \"" + nodeType + "\", \""
					+ node.getLabel() + "\"";

			if (operation instanceof UpdateOperation) {
				// adding the new value for update
				result += ",  \"" + ((Update) operation.getAction()).getValue() + "\"";
			}

			result += " (size: " + node.getDescendants().size() + ")" + node.toTreeString();
		}
		return result;
	}

	@Override
	public String toString() {
		if (rootOperations.size() == 0) {
			return "no AST change";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		final CtElement ctElement = commonAncestor();

		for (Operation operation : rootOperations) {
			stringBuilder.append(operation.toString());

			// if all actions are applied on the same node print only the first action
			if (operation.getSrcNode().equals(ctElement) && operation instanceof UpdateOperation) {
				break;
			}
		}
		return stringBuilder.toString();
	}

	public TreeContext getContext() {
		return context;
	}

	public MappingStore getMappingsComp() {
		return _mappingsComp;
	}
}
