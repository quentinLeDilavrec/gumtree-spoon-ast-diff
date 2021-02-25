package gumtree.spoon.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.MyAction;
import com.github.gumtreediff.actions.VersionedEditScript;
import com.github.gumtreediff.actions.VersionedEditScriptGenerator;
import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
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
	private List<Operation> allOperations;
	/**
	 * Actions over the changes roots.
	 */
	private List<Operation> rootOperations;
	/**
	 * the mapping of this diff
	 */
	private MappingStore _mappingsComp;
	/**
	 * Context of the spoon diff.
	 */
	private List<Action> atomicActionsList;
	private List<Action> composedActionsList;
	private VersionedEditScript editScript;

	public List<MyAction<?>> getActions() {
		return editScript.asList();
	}

	public <U extends Action & AtomicAction<AbstractVersionedTree>> List<U> getAtomic() {
		return editScript.getAtomic();
	}

	public <U extends Action & ComposedAction<AbstractVersionedTree>> Set<U> getComposed() {
		return editScript.getComposed();
	}
	
	DiffImpl(VersionedEditScriptGenerator actionGenerator, ITree rootSpoonLeft, ITree rootSpoonRight,
			Version beforeVersion, Version afterVersion) {
		final MappingStore mappingsComp = new SingleVersionMappingStore<ITree, ITree>(rootSpoonLeft, rootSpoonRight);

		final Matcher matcher = new CompositeMatchers.ClassicGumtree(rootSpoonLeft, rootSpoonRight, mappingsComp);
		matcher.match();
		this.editScript = actionGenerator.computeActions(matcher, beforeVersion,
				afterVersion);
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

	// @Override
	// public boolean containsOperation(OperationKind kind, String nodeKind) {
	// 	return rootOperations.stream() //
	// 			.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
	// 					&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind));
	// }

	// @Override
	// public boolean containsOperation(OperationKind kind, String nodeKind, String nodeLabel) {
	// 	return containsOperations(getRootOperations(), kind, nodeKind, nodeLabel);
	// }

	// @Override
	// public boolean containsOperations(List<Operation> operations, OperationKind kind, String nodeKind,
	// 		String nodeLabel) {
	// 	return operations.stream()
	// 			.anyMatch(operation -> operation.getAction().getClass().getSimpleName().equals(kind.name()) //
	// 					&& context.getTypeLabel(operation.getAction().getNode()).equals(nodeKind)
	// 					&& operation.getAction().getNode().getLabel().equals(nodeLabel));
	// }

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

	// @Override
	// public void debugInformation() {
	// 	System.err.println(toDebugString());
	// }

	// private String toDebugString() {
	// 	return toDebugString(rootOperations);
	// }

	// private String toDebugString(List<Operation> ops) {
	// 	String result = "";
	// 	for (Operation operation : ops) {
	// 		ITree node = operation.getAction().getNode();
	// 		final CtElement nodeElement = operation.getSrcNode();
	// 		String nodeType = context.getTypeLabel(node.getType());
	// 		if (nodeElement != null) {
	// 			nodeType += "(" + nodeElement.getClass().getSimpleName() + ")";
	// 		}
	// 		result += "OperationKind." + operation.getAction().getClass().getSimpleName() + ", \"" + nodeType + "\", \""
	// 				+ node.getLabel() + "\"";

	// 		if (operation instanceof UpdateOperation) {
	// 			// adding the new value for update
	// 			result += ",  \"" + ((Update) operation.getAction()).getValue() + "\"";
	// 		}

	// 		result += " (size: " + node.getDescendants().size() + ")" + node.toTreeString();
	// 	}
	// 	return result;
	// }

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

	// public TreeContext getContext() {
	// 	return context;
	// }

	public MappingStore getMappingsComp() {
		return _mappingsComp;
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsOperation(OperationKind kind, String nodeKind, String nodeLabel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsOperations(List<Operation> operations, OperationKind kind, String nodeKind,
			String nodeLabel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void debugInformation() {
		// TODO Auto-generated method stub
	}
}
