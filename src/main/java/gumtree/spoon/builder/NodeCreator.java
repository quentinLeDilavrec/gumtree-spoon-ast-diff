package gumtree.spoon.builder;

import com.github.gumtreediff.tree.ITree;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
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
			if (mod.isImplicit()) {
				continue;
			}
			ITree modifier = builder.createNode("MODIFIER", mod.getKind().toString());
			// modifiers.addChild(modifier);
			// We wrap the modifier (which is not a ctelement)
			modifier.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtWrapper.CtModifierWrapper(mod, m));
			builder.addSiblingNode(modifier);
		}
		// builder.addSiblingNode(modifiers);

	}

	private String getClassName(String simpleName) {
		if (simpleName == null)
			return "";
		return simpleName.replace("Ct", "").replace("Impl", "");
	}

	private <T> void genericTransfo(CtTypeReference<T> parametrizedType, ITree parametrizedTypeTree) {
		ITree label = builder.createNode("LABEL", extracted(parametrizedType));//.replace("$", ".")); 
		// TODO simple or qual ? should build qual myself for implicit ones
		// computeExpliciteQualName(parametrizedTypeTree,parametrizedType);
		parametrizedTypeTree.addChild(label);
		for (CtTypeReference<?> typeParam : parametrizedType.getActualTypeArguments()) {
			if (!typeParam.isImplicit()) {
				ITree tree = builder.createNode(builder.getNodeType(typeParam),
						builder.getTypeName(typeParam.getClass().getSimpleName()));
				tree.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, typeParam);
				typeParam.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, tree);
				genericTransfo(typeParam, tree);
				parametrizedTypeTree.addChild(tree);
			}
		}
	}

	private <T> String extracted(CtTypeReference<T> reference) {
		if (reference instanceof CtArrayTypeReference) {
			return ((CtArrayTypeReference) reference).getComponentType().getSimpleName();
		} else {
			return reference.getSimpleName();
		}
	}

}
