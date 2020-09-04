/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.matchers;

import java.util.*;

import com.github.gumtreediff.tree.ITree;

public class SingleVersionMappingStore<T extends ITree, U extends ITree> extends MappingStore {

    private T src;
    private U dst;

    public SingleVersionMappingStore(T src, U dst, Set<Mapping> mappings) {
        super(mappings);
        this.src = src;
        this.dst = dst;
    }

    public SingleVersionMappingStore(T src, U dst) {
        super();
        this.src = src;
        this.dst = dst;
    }

    public T getSrc() {
        return src;
    }

    public U getDst() {
        return dst;
    }

    @Override
    public void link(ITree src, ITree dst) {
        super.link((T) src, (U) dst);
    }

    public T getSrc(ITree dst) {
        return (T) super.getSrc(dst);
    }

    public U getDst(ITree src) {
        return (U) super.getDst(src);
    }
}
