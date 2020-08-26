package gumtree.spoon.apply;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import gumtree.spoon.apply.operations.ApplicableOperation;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;

public class ApplicableDiffImpl implements ApplicableDiff {
    Map<Operation<?>, ApplicableOperation<?>> map;

    public ApplicableDiffImpl(Diff diff) {
        this.map = new HashMap<>();
        for (Operation<?> operation : diff.getAllOperations()) {
            ApplicableOperation<?> aO = new ApplicableOperation<>(operation.getAction());
            map.put(operation, aO);
        }
        
    }

    public Map<Operation<?>, ApplicableOperation<?>> getMap() {
        return Collections.unmodifiableMap(map);
    }

}
