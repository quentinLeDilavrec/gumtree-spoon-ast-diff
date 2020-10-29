package com.github.gumtreediff.tree;

import org.apache.commons.lang3.Range;

import spoon.support.Experimental;

/**
 * Not a version range, because I also want it to handle multiple branches
 * For now only accept one a min and max limits, where min is excluded
 */
@Experimental
public class VersionSlice implements Version {

    public final Version minExcluded;
    public final Version maxIncluded;

    @Override
    public COMP_RES partiallyCompareTo(Version other) {
        if (other instanceof VersionSlice) {
            VersionSlice o = ((VersionSlice) other);
            COMP_RES a = minExcluded.partiallyCompareTo(o.maxIncluded);
            switch (a) {
                case SUPERIOR:
                    return COMP_RES.SUPERIOR;
                default:
                    break;
            }
            COMP_RES b = maxIncluded.partiallyCompareTo(o.minExcluded);
            switch (b) {
                case INFERIOR:
                    return COMP_RES.INFERIOR;
                // case PARALLEL:
                //     if (!minExcluded.partiallyCompareTo(o.minExcluded).equals(COMP_RES.INFERIOR)) {
                //         return COMP_RES.INFERIOR;
                //     }
                default:
                    return COMP_RES.UNKNOWN;
            }
        } else {
            COMP_RES a = minExcluded.partiallyCompareTo(other);
            switch (a) {
                case SUPERIOR:
                    return COMP_RES.SUPERIOR;
                default:
                    break;
            }
            COMP_RES b = maxIncluded.partiallyCompareTo(other);
            switch (b) {
                case INFERIOR:
                    return COMP_RES.INFERIOR;
                default:
                    break;
            }
            return COMP_RES.UNKNOWN;
        }
    }

    public boolean contain(Version v) {
        if (v instanceof VersionSlice) {
            // min <= v.min && v.max <= max
            VersionSlice v1 = (VersionSlice) v;
            COMP_RES a = minExcluded.partiallyCompareTo(v1.minExcluded);
            COMP_RES b = maxIncluded.partiallyCompareTo(v1.maxIncluded);
            if ((a.equals(COMP_RES.EQUAL) || a.equals(COMP_RES.INFERIOR))
                    && (b.equals(COMP_RES.EQUAL) || b.equals(COMP_RES.SUPERIOR))) {
                return true;
            } else {
                return false;
            }
        } else {
            // min < v <= max
            if (maxIncluded.partiallyCompareTo(v).equals(COMP_RES.EQUAL)) {
                // max = v
                return true;
            }
            // min < v < max
            if (v.partiallyCompareTo(minExcluded).equals(COMP_RES.SUPERIOR)
                    && v.partiallyCompareTo(maxIncluded).equals(COMP_RES.INFERIOR)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static VersionSlice between(Version minExcluded, Version maxIncluded) {
        if (minExcluded instanceof VersionSlice) {
            throw new RuntimeException("minExcluded cannot be a VersionSlice");
        }
        if (maxIncluded instanceof VersionSlice) {
            throw new RuntimeException("maxIncluded cannot be a VersionSlice");
        }
        switch (minExcluded.partiallyCompareTo(maxIncluded)) {
            case EQUAL:
                throw new RuntimeException("a=b thus singleton, not allowed");
            case INFERIOR:
                break;
            case SUPERIOR:
                throw new RuntimeException("min must be inferior to max");
            case PARALLEL:
                throw new RuntimeException("cannot have // limits");
            case UNKNOWN:
                throw new RuntimeException("unknown order");
        }
        return new VersionSlice(minExcluded, maxIncluded);
    }

    private VersionSlice(Version minExcluded, Version maxIncluded) {
        this.minExcluded = minExcluded;
        this.maxIncluded = maxIncluded;
    }
}