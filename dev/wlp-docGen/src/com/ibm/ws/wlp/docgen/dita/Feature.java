package com.ibm.ws.wlp.docgen.dita;

import java.util.ArrayList;
import java.util.List;

public class Feature {
  public static class Package {
    public Package(String packageName, String type, String link) {
      this.packageName = packageName;
      this.packageType = type;
      this.javadocLink = link;
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
  public List<String> configElement = new ArrayList<String>();
  public List<Package> apiPackages = new ArrayList<Package>();
  public List<Package> spiPackages = new ArrayList<Package>();
  public List<String> apiJar = new ArrayList<String>();
  public List<String> spiJar = new ArrayList<String>();
  public List<String> javaLevels = new ArrayList<String>();
}
