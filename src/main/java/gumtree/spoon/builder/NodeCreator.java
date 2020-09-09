package gumtree.spoon.builder;

import com.github.gumtreediff.tree.ITree;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.support.reflect.CtExtendedModifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * responsible to add additional nodes only overrides scan* to add new nodes
 */
public class NodeCreator extends CtInheritanceScanner {
	public static final String MODIFIERS = "Modifiers_";
	private final TreeScanner builder;

	NodeCreator(TreeScanner builder) {
		this.builder = builder;
	}

	@Override
	public void scanCtModifiable(CtModifiable m) {

		if (m.getModifiers().isEmpty())
			return;

		// // We add the type of modifiable element
		// String type = MODIFIERS + getClassName(m.getClass().getSimpleName());
		// ITree modifiers = builder.createNode(type, "Modifiers");

		// // We create a virtual node
		// modifiers.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtVirtualElement(type, m, m.getExtendedModifiers()));

		// // ensuring an order (instead of hashset)
		// // otherwise some flaky tests in CI
		Set<CtExtendedModifier> modifiers1 = new TreeSet<>(new Comparator<CtExtendedModifier>() {
			@Override
			public int compare(CtExtendedModifier o1, CtExtendedModifier o2) {
				SourcePosition p1 = o1.getPosition();
				SourcePosition p2 = o2.getPosition();
				if (p1.isValidPosition() && p2.isValidPosition()) {
					return p1.getSourceStart() - p2.getSourceStart();
				}
				if (p1.isValidPosition()) {
					return 1;
				}
				return o1.getKind().name().compareTo(o2.getKind().name());
			}
		});
		modifiers1.addAll(m.getExtendedModifiers());

		for (CtExtendedModifier mod : modifiers1) {
			ITree modifier = builder.createNode("MODIFIER", mod.getKind().toString());
			// modifiers.addChild(modifier);
			// We wrap the modifier (which is not a ctelement)
			modifier.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtWrapper(mod, m));
			builder.addSiblingNode(modifier);
		}
		// builder.addSiblingNode(modifiers);

	}

	private String getClassName(String simpleName) {
		if (simpleName == null)
			return "";
		return simpleName.replace("Ct", "").replace("Impl", "");
	}

	@Override
	public <T> void scanCtVariable(CtVariable<T> e) {
		CtTypeReference<T> type = e.getType();
		if (type != null) {
			ITree variableType = builder.createNode("VARIABLE_TYPE",
					builder.getTypeName(type.getClass().getSimpleName()));
			variableType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, type);
			type.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, variableType);
			genericTransfo(type, variableType);
			builder.addSiblingNode(variableType);
		}
	}

	@Override
	public <T> void visitCtMethod(CtMethod<T> e) {
		// add the return type of the method
		CtTypeReference<T> type = e.getType();
		if (type != null) {
			ITree returnType = builder.createNode("RETURN_TYPE", builder.getTypeName(type.getClass().getSimpleName()));
			returnType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, type);
			type.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, returnType);
			genericTransfo(type, returnType);
			builder.addSiblingNode(returnType);
		}

		for (CtTypeReference<?> thrown : e.getThrownTypes()) {
			ITree thrownType = builder.createNode("THROWS", builder.getTypeName(thrown.getClass().getSimpleName()));
			thrownType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, thrown);
			type.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, thrownType);
			genericTransfo(thrown, thrownType);
			builder.addSiblingNode(thrownType);
		}

		super.visitCtMethod(e);
	}

	private <T> void genericTransfo(CtTypeReference<T> parametrizedType, ITree parametrizedTypeTree) {
		ITree label = builder.createNode("LABEL", parametrizedType.getQualifiedName());
		parametrizedTypeTree.addChild(label);
		for (CtTypeReference<?> typeParam : parametrizedType.getActualTypeArguments()) {
			ITree tree = builder.createNode(builder.getNodeType(typeParam),
					builder.getTypeName(typeParam.getClass().getSimpleName()));
			tree.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, typeParam);
			typeParam.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, tree);
			genericTransfo(typeParam, tree);
			parametrizedTypeTree.addChild(tree);
		}
	}

	@Override
	public void scanCtReference(CtReference reference) {
		if (reference instanceof CtTypeReference && reference.getRoleInParent() == CtRole.SUPER_TYPE) {
			ITree superType = builder.createNode("SUPER_CLASS",
					builder.getTypeName(((CtTypeReference<?>) reference).getClass().getSimpleName()));
			CtWrapper<CtReference> k = new CtWrapper<CtReference>(reference, reference.getParent());
			superType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, k);
			reference.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, superType);
			genericTransfo((CtTypeReference<?>) reference, superType);
			builder.addSiblingNode(superType);
		} else if (reference instanceof CtTypeReference && reference.getRoleInParent() == CtRole.INTERFACE) {
			ITree superType = builder.createNode("INTERFACE",
					builder.getTypeName(((CtTypeReference<?>) reference).getClass().getSimpleName()));
			CtWrapper<CtReference> k = new CtWrapper<CtReference>(reference, reference.getParent());
			superType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, k);
			reference.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, superType);
			genericTransfo((CtTypeReference<?>) reference, superType);
			builder.addSiblingNode(superType);
		} else {
			super.scanCtReference(reference);
		}
	}

	// @Override
	// public <T> void scanCtTypedElement(CtTypedElement<T> typedElement) {
	// 	if (typedElement instanceof CtAnnotation) {
	// 		CtAnnotation<?> annot = (CtAnnotation<?>) typedElement;
			
	// 	} else {
	// 		super.scanCtTypedElement(typedElement);
	// 	}
	// }
}
