/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.injection.dfi;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.FATSuite;
import com.ibm.ws.jpa.JPAFATServletClient;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIProYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPriYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIProYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPubYesInhDDOvrdTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JPA10Injection_DFI_YesInheritance_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/injection/";
    private final static String applicationName = "JPA10Injection_DFIYesInheritance_Web"; // Name of EAR
    private final static String contextRoot = "JPA10Injection_DFIYesInheritance_Web";
    private final static String contextRoot2 = "JPA10Injection_DFIYesInheritance_DDOvrd_Web";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerDFI")
    @TestServlets({
                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdTestServlet.class, path = contextRoot + "/" + "DFIPkgYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhAnoOvrdTestServlet.class, path = contextRoot + "/" + "DFIPriYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIProYesInhAnoOvrdTestServlet.class, path = contextRoot + "/" + "DFIProYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhAnoOvrdTestServlet.class, path = contextRoot + "/" + "DFIPubYesInhAnoOvrdTestServlet"),

                    @TestServlet(servlet = DFIPkgYesInhDDOvrdTestServlet.class, path = contextRoot2 + "/" + "DFIPkgYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdTestServlet.class, path = contextRoot2 + "/" + "DFIPriYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdTestServlet.class, path = contextRoot2 + "/" + "DFIProYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdTestServlet.class, path = contextRoot2 + "/" + "DFIPubYesInhDDOvrdTestServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(JPA10Injection_DFI_YesInheritance_Web.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", FATSuite.repeatPhase);

        server1.startServer();

        setupDatabaseApplication(server1, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        final String webModuleName = "injectionDFIYesInheritance";
        final String webModule2Name = "injectionDFIYesInheritanceDDOvrd";
        final String webFileRootPath = RESOURCE_ROOT + "web/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd");
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, webModule2Name + ".war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd");
        ShrinkHelper.addDirectory(webApp2, webFileRootPath + webModule2Name + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(webApp2);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (FATSuite.repeatPhase != null && FATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(JPA10Injection_DFI_YesInheritance_Web.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/" + applicationName + ".ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }
}
