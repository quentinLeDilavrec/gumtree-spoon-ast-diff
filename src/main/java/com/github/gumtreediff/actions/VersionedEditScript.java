
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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.MyAction.AtomicAction;
import com.github.gumtreediff.actions.MyAction.ComposedAction;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.AbstractVersionedTree;
import com.github.gumtreediff.tree.Version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class representing edit scripts: sequence of edit actions.
 *
 * @see Action
 */
public class VersionedEditScript extends EditScript<AbstractVersionedTree> {

    Version leftVersion;
    Version rightVersion;

    public Version getLeftVersion() {
        return leftVersion;
    }

    public Version getRightVersion() {
        return rightVersion;
    }

    public VersionedEditScript(Version leftVersion, Version rightVersion) {
        this.leftVersion = leftVersion;
        this.rightVersion = rightVersion;
    }

}