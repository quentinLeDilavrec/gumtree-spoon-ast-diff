package gumtree.spoon.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import gnu.trove.map.TIntObjectMap;
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

	public DiffImpl(TreeContext context, ITree rootSpoonLeft, ITree rootSpoonRight) {
		if (context == null) {
			throw new IllegalArgumentException();
		}
		final MappingStore mappingsComp = new MappingStore();
		this.context = context;

		final Matcher matcher = new CompositeMatchers.ClassicGumtree(rootSpoonLeft, rootSpoonRight, mappingsComp);
		matcher.match();

		final ActionGenerator actionGenerator = new ActionGenerator(rootSpoonLeft, rootSpoonRight,
				matcher.getMappings());
		actionGenerator.generate();
		List<Action> actions;
		String b = System.getProperty("gumtree.match.gt.ag.nomove");
		if (b != null && b.equals("true")) {
			actions = removeMovesAndUpdates(actionGenerator);
		} else {
			actions = actionGenerator.getActions();
		}

		ActionClassifier actionClassifier = new ActionClassifier(matcher.getMappingsAsSet(), actions);
		// Bugfix: the Action classifier must be executed *BEFORE* the convertToSpoon
		// because it writes meta-data on the trees
		this.rootOperations = convertToSpoon(actionClassifier.getRootActions());
		this.allOperations = convertToSpoon(actions);

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

	private List<Action> removeMovesAndUpdates(ActionGenerator actionGenerator) {
		try {
			TIntObjectMap<ITree> origSrcTrees = extracted(actionGenerator, "origSrcTrees");
			TIntObjectMap<ITree> cpySrcTrees = extracted(actionGenerator, "cpySrcTrees");
			List<Action> actions = extracted(actionGenerator, "actions");
			MappingStore origMappings = extracted(actionGenerator, "origMappings");
			List<Action> actionsCpy = new ArrayList<>(actions.size());
			for (Action a : actions) {
				if (a instanceof Update) {
					Update u = (Update) a;
					ITree src = cpySrcTrees.get(a.getNode().getId());
					ITree dst = origMappings.getDst(src);
					actionsCpy.add(new Insert(dst, dst.getParent(), dst.positionInParent()));
					actionsCpy.add(new Delete(origSrcTrees.get(u.getNode().getId())));
				} else if (a instanceof Move) {
					Move m = (Move) a;
					ITree src = cpySrcTrees.get(a.getNode().getId());
					ITree dst = origMappings.getDst(src);
					actionsCpy.add(new Insert(dst, dst.getParent(), m.getPosition()));
					actionsCpy.add(new Delete(origSrcTrees.get(m.getNode().getId())));
				} else {
					actionsCpy.add(a);
				}
			}

			return actionsCpy;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T extracted(ActionGenerator actionGenerator, String name)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = actionGenerator.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(actionGenerator);
	}

	private List<Operation> convertToSpoon(List<Action> actions) {
		List<Operation> collect = actions.stream().map(action -> {
			action.getNode().setMetadata("type", context.getTypeLabel(action.getNode()));
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
