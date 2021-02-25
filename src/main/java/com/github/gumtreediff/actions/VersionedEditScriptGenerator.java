package com.github.gumtreediff.actions;

import java.util.HashSet;
import java.util.Set;

import javax.management.RuntimeErrorException;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Version;

/**
 * Interface for script generators that compute edit scripts from mappings.
 *
 * @see MappingStore
 * @see VersionedEditScript
 */
public abstract class VersionedEditScriptGenerator {
    // final AbstractVersionedTree middle;
    final Set<Version> visitedVersions;
    protected VersionedEditScript actions;
    // private VersionedEditScriptGenerator() {
    // }

    public VersionedEditScriptGenerator(AbstractVersionedTree middle, Version initialVersion) {
        // this.middle = middle;
        this.visitedVersions = new HashSet<>();
        visitedVersions.add(initialVersion);
    }

    public final VersionedEditScript computeActions(Matcher matcher, Version beforeVersion, Version afterVersion) {
        this.actions = new VersionedEditScript(beforeVersion, afterVersion);
        if (visitedVersions.contains(beforeVersion) && visitedVersions.contains(afterVersion)) {
            computeActionsSuplementary(matcher, beforeVersion, afterVersion);
        } else if (visitedVersions.contains(beforeVersion)) {
            computeActionsForward(matcher, beforeVersion, afterVersion);
        } else if (visitedVersions.contains(afterVersion)) {
            computeActionsBackward(matcher, beforeVersion, afterVersion);
        } else {
            throw new UnsupportedOperationException("for now do not compute actions in parallele branches");
        }
        visitedVersions.add(beforeVersion);
        visitedVersions.add(afterVersion);
        actions.compress();
        return actions;
    }

    protected final Action addInsert(ITree x, AbstractVersionedTree w){
        MyAction.MyInsert a = new MyAction.MyInsert(x, w);
        actions.add(a);
        return a;
    }

    protected final Action addDelete(AbstractVersionedTree w){
        MyAction.MyDelete a = new MyAction.MyDelete(w);
        actions.add(a);
        return a;
    }


    protected final void add(MyAction<?> a){
        actions.add(a);
    }

    protected final Action addUpdate(AbstractVersionedTree w, AbstractVersionedTree newTree){
        MyAction.MyUpdate a = new MyAction.MyUpdate(w, newTree);
        actions.add(a);
        return a;
    }

    protected final MyAction.MyMove addMove(AbstractVersionedTree w, AbstractVersionedTree newTree){
        // protected final Action addMove(AbstractVersionedTree w, AbstractVersionedTree newTree){
        // Action a = new MyAction.MyMove(w,newTree);


        MyAction.MyInsert iact = new MyAction.MyInsert(w, newTree);
        MyAction.MyDelete dact = new MyAction.MyDelete(w);
        MyAction.MyMove a = new MyAction.MyMove(dact, iact);
        // actions.addComposed(a);

        actions.add(iact);
        actions.add(a);
        return a;
    }

    public abstract VersionedEditScript computeActionsForward(Matcher matcher, Version beforeVersion,
            Version afterVersion);

    public abstract VersionedEditScript computeActionsBackward(Matcher matcher, Version beforeVersion,
            Version afterVersion);

    public abstract VersionedEditScript computeActionsSuplementary(Matcher matcher, Version beforeVersion,
            Version afterVersion);
}