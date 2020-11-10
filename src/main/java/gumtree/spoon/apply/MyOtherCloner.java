package gumtree.spoon.apply;

import java.nio.file.Path;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import gumtree.spoon.CloneVisitorNewFactory;
import spoon.MavenLauncher;
import spoon.MavenLauncher.SOURCE_TYPE;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;
import spoon.support.visitor.equals.CloneHelper;

public class MyOtherCloner extends CloneHelper {
	private Factory facto;

	public MyOtherCloner(Factory facto) {
		this.facto = facto;
	}

	@Override
	public <T extends CtElement> T clone(T element) {
		final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, facto);
		cloneVisitor.scan(element);
		T clone = cloneVisitor.getClone();
		return clone;
	}

	public SourcePosition clone(SourcePosition position, CtElement parent) {
		if (parent == null) {
			throw new RuntimeException();
		} else if (parent instanceof CtPackage) {
			final CloneVisitorNewFactory cloneVisitor = new CloneVisitorNewFactory(this, facto) {
				@Override
				public void visitCtCompilationUnit(CtCompilationUnit compilationUnit) {
					CtCompilationUnit aCtCompilationUnit = this.factory.Core().createCompilationUnit();
					this.builder.copy(compilationUnit, aCtCompilationUnit);
					aCtCompilationUnit.setPosition(clonePosition(compilationUnit.getPosition()));

					// parent: ProjRoot/ --------------
					// current: ------ /srcDir/filePath
					ImmutableTriple<Path, Path, SOURCE_TYPE> parentTriple = (ImmutableTriple<Path, Path, MavenLauncher.SOURCE_TYPE>) parent
							.getPosition().getCompilationUnit().getMetadata("SourceTypeNRootDirectory");
					ImmutableTriple<Path, Path, SOURCE_TYPE> srcTriple = (ImmutableTriple<Path, Path, MavenLauncher.SOURCE_TYPE>) compilationUnit
							.getMetadata("SourceTypeNRootDirectory");
					if (parentTriple != null && srcTriple != null) {
						aCtCompilationUnit
								.setFile(parentTriple.left.resolve(srcTriple.left.resolve(srcTriple.middle)
										.relativize(compilationUnit.getFile().toPath())).toFile());
						aCtCompilationUnit.putMetadata("SourceTypeNRootDirectory",
								new ImmutableTriple<>(parentTriple.left, srcTriple.middle, srcTriple.right));
					}

					aCtCompilationUnit.setComments(this.cloneHelper.clone(compilationUnit.getComments()));
					aCtCompilationUnit.setAnnotations(this.cloneHelper.clone(compilationUnit.getAnnotations()));
					aCtCompilationUnit
							.setPackageDeclaration(this.cloneHelper.clone(compilationUnit.getPackageDeclaration()));
					aCtCompilationUnit.setImports(this.cloneHelper.clone(compilationUnit.getImports()));
					aCtCompilationUnit.setDeclaredModuleReference(
							this.cloneHelper.clone(compilationUnit.getDeclaredModuleReference()));
					aCtCompilationUnit.setDeclaredTypeReferences(
							this.cloneHelper.clone(compilationUnit.getDeclaredTypeReferences()));
					this.cloneHelper.tailor(compilationUnit, aCtCompilationUnit);
					this.other = aCtCompilationUnit;
				}
			};
			return cloneVisitor.clonePosition(position);
		} else {
			return new CloneVisitorNewFactory(this, facto).clonePositionAux(position,
					parent.getPosition().getCompilationUnit());
		}
	}
}