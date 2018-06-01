/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.asciidoc;

import java.util.ArrayList;
import java.util.List;

public class Feature {
  public static class Package {
    public Package(String packageName, String type) {
      this.packageName = packageName;
      this.packageType = type;
    }
    public String packageName;
    public String packageType;
    public String javadocLink;
  }
  public String name;
  public String symbolicName;
  public String displayName;
  public String description;
  public boolean superceeded;
  public List<String> superceededBy = new ArrayList<String>();
  public List<String> enables = new ArrayList<String>();
  public List<String> enabledBy = new ArrayList<String>();
  public List<String> configElement = new ArrayList<String>();
  public List<Package> apiPackages = new ArrayList<Package>();
  public List<Package> spiPackages = new ArrayList<Package>();
  public List<String> apiJar = new ArrayList<String>();
  public List<String> spiJar = new ArrayList<String>();
  public List<String> javaLevels = new ArrayList<String>();
}
