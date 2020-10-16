/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.server;

import java.net.*;
import java.io.*;
import java.util.*;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.options.*;
import megamek.server.commands.*;

/**
 * @author Ben Mazur
 */
public class Server
implements Runnable {
    /**
     * Resolve all fire for the round
     */
    private void resolveWeaponAttacks() {
        
        int cen = Entity.NONE;
        
        for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
            // Object o = i.nextElement();
            // Entity entity = game.getEntity(((EntityAction)o).getEntityId());
        }
        
    }
}
