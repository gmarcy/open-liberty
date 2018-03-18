/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public abstract class CommonTests {
    public abstract LibertyServer getServer();

    @Test
    public void testBasicSpringBootApplication() throws Exception {
        LibertyServer server = getServer();
        File f = new File(server.getServerRoot() + "/dropins/spr/");
        assertTrue("file does not exist", f.exists());
        server.startServer(true, false);
        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*"));

        // NOTE we set the port to the expected port according to the test application.properties
        server.setHttpDefaultPort(8081);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

}
