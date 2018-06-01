/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.asciidoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.wlp.docgen.asciidoc.Feature.Package;

public class GenerateAdocForFeatures extends AdocGeneratorTask {

  private String format = "adoc";

  @Override
  protected void doExecute() {
    List<Feature> features = loadFeatures("feature");
    List<Feature> kernel = loadFeatures("kernelFeature");

    // Merge all the kernel features into one kernel feature
    Feature kernelFeature = new Feature();
    kernelFeature.name = "kernel";
    for (Feature f : kernel) {
      kernelFeature.apiJar.addAll(f.apiJar);
      kernelFeature.spiJar.addAll(f.spiJar);
      kernelFeature.apiPackages.addAll(f.apiPackages);
      kernelFeature.spiPackages.addAll(f.spiPackages);
      kernelFeature.configElement.addAll(f.configElement);
      if ("com.ibm.websphere.appserver.kernelCore-1.0".equals(f.symbolicName)) {
        kernelFeature.displayName = f.displayName;
        kernelFeature.description = f.description;
      }
    }

    features.add(kernelFeature);

    Map<String, Feature> featuresByName = new HashMap<String, Feature>();

    for (Feature f : features) {
      featuresByName.put(f.name, f);
    }

    for (Feature f : features) {
      for (String featureName : f.enables) {
        Feature other = featuresByName.get(featureName);
        if (other != null) {
          other.enabledBy.add(f.name);
        }
      }
    }

    for (Feature f : features) {
      configManifest.setProperty(f.name, f.displayName);
      writeFeature(f, featuresByName);
    }
  }

  private void writeFeature(Feature f, Map<String, Feature> featuresByName) {
    try {
      PrintStream out = new PrintStream(new File(dir, "rwlp_feature_" + f.name + "." + this.format));

      Map<String, String> formatAttributes = new HashMap<String, String>();
      formatAttributes.put("stylesheet", "../feature.css");
      formatAttributes.put("nofooter", "");
      Formatter format = Formatter.Factory.create(this.format, formatAttributes, out);

      format.header(1).raw(f.displayName).end();
      format.raw(f.description).next().next();
      if (!!!"kernel".equals(f.name)) {
        format.header(2).raw(getNLS("feature.enable.title")).next().
               raw(getNLS("feature.enable.desc", f.displayName)).next().next().
               code("xml").raw("<feature>").raw(f.name).raw("</feature>").end();
      }
      if (!!!f.javaLevels.isEmpty()) {
        format.next().header(2).raw(getNLS("required.java")).end().next();
        for (String java : f.javaLevels) {
          format.bullet().raw(java).end();
        }
        format.end();
      }
      if (!"kernel".equals(f.name)) {
        format.next().header(2).raw(getNLS("feature.dependency.title")).next().
               raw(getNLS("feature.dependency.desc", f.displayName)).next().
               next().code("").raw(f.symbolicName).raw("; type=\"osgi.subsystem.feature\"").end();
      }
      if (f.superceeded) {
        printFeatures(format, "superceeded.title", f.superceededBy, featuresByName);
      }
      printFeatures(format, "feature.enables.title", f.enables, featuresByName);
      printFeatures(format, "feature.enabledBy.title", f.enabledBy, featuresByName); // TODO enabled
                                                                // by, this
                                                                // isn't enabled
                                                                // by
      printPackage(format, "package.spec.api.title", "spec", f.apiPackages);
      printPackage(format, "package.stable.api.title", "stable", f.apiPackages);
      printPackage(format, "package.ibm.api.title", "ibm-api", f.apiPackages);
      printPackage(format, "package.third.party.api.title", "third-party",
          f.apiPackages);
      printPackage(format, "package.spi.title", null, f.spiPackages);
      if (!!!f.configElement.isEmpty()) {
        Collections.sort(f.configElement);
        format.next().header(2).raw(getNLS("config.title")).end();
        for (String element : f.configElement) {
          format.bullet().link(element, "config").end();
        }
        format.end();
      }
    } catch (IOException e) {
      throw new BuildException(e.getMessage(), e);
    }
  }

  private void printPackage(Formatter f, String titleKey, String type,
      List<Package> packages) {
    List<Package> relevantPackages = new ArrayList<Feature.Package>();
    if (type != null) {
      for (Package aPackage : packages) {
        if (type.equals(aPackage.packageType)) {
          relevantPackages.add(aPackage);
        }
      }
    } else {
      relevantPackages = packages;
    }
    if (!!!relevantPackages.isEmpty()) {
      Collections.sort(relevantPackages, new Comparator<Package>() {

        @Override
        public int compare(Package o1, Package o2) {
          return o1.packageName.compareTo(o2.packageName);
        }
      });
      f.next().header(2).raw(getNLS(titleKey)).end();
      for (Package aPackage : relevantPackages) {
        f.bullet();
        f.link(aPackage.packageName, "javadoc");
        f.end();
      }
      f.end();
    }
  }

  private void printFeatures(Formatter format, String key, List<String> features, Map<String, Feature> featuresByName) {
    if (!!!features.isEmpty()) {
      Collections.sort(features);
      format.next().header(2).raw(getNLS(key)).end();
      for (String feature : features) {
        int index = feature.indexOf('[');
        if (index >= 0) {
          feature = feature.substring(index + 1, feature.indexOf( ']', index));
        }
        feature = feature.trim();
        Feature f = featuresByName.get(feature);
        if (f != null) {
          format.bullet().link(feature, "feature").end();
        } else {
          getProject().log("Unable to find feature " + feature, Project.MSG_DEBUG);
        }
      }
      format.end();
    }
  }

  private List<Feature> loadFeatures(String element) {
    List<Feature> features = new ArrayList<Feature>();
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document d;
    try {
      d = dbf.newDocumentBuilder().parse(file);
    } catch (Exception e) {
      throw new BuildException("Error parsing featureList: " + file, e);
    }

    Element doc = d.getDocumentElement();

    NodeList nl = doc.getElementsByTagName(element);

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Feature f = new Feature();
        Element e = (Element) n;
        f.name = e.getAttribute("name");
        f.symbolicName = getElementText(e, "symbolicName");
        f.displayName = getElementText(e, "displayName");

        if (f.displayName == null) {
          f.displayName = f.name;
        }

        f.description = getElementText(e, "description");
        String superceeded = getElementText(e, "superseded");
        if (superceeded != null) {
          f.superceeded = Boolean.parseBoolean(superceeded);
        }

        loadStrings(e, "supersededBy", f.superceededBy);
        loadStrings(e, "configElement", f.configElement);
        loadStrings(e, "enables", f.enables);
        appendEnables(e, "include", f.enables); // Add tolerated versions
        loadStrings(e, "apiJar", f.apiJar);
        loadStrings(e, "spiJar", f.spiJar);
        loadStrings(e, "javaVersion", f.javaLevels);
        loadPackages(e, "apiPackage", f.apiPackages);
        loadPackages(e, "spiPackage", f.spiPackages);

        Iterator<String> it = f.apiJar.iterator();
        while (it.hasNext()) {
          String jar = it.next();
          if (!!!jar.startsWith("dev/api/ibm/")) {
            it.remove();
          }
        }

        it = f.spiJar.iterator();
        while (it.hasNext()) {
          String jar = it.next();
          if (!!!jar.startsWith("dev/spi/ibm/")) {
            it.remove();
          }
        }

        features.add(f);
      }
    }

    return features;
  }

  private String getElementText(Element e, String elementName) {
    NodeList nl = e.getElementsByTagName(elementName);

    if (nl.getLength() == 1) {
      return nl.item(0).getTextContent();
    }

    return null;
  }

  private void loadPackages(Element e, String elementName,
      List<Package> packages) {
    NodeList nl = e.getElementsByTagName(elementName);

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Element p = (Element) n;
        String packageName = n.getTextContent();
        String type = p.getAttribute("type");
        if (!!!"internal".equals(type)) {
          packages.add(new Package(packageName, type));
        }
      }
    }
  }

  private void loadStrings(Element e, String elementName, List<String> strings) {
    NodeList nl = e.getElementsByTagName(elementName);

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      strings.add(n.getTextContent());
    }
  }

  private void appendEnables(Element e, String elementName, List<String> enables) {

    NodeList nl = e.getElementsByTagName(elementName);

    for (int i = 0; i < nl.getLength(); i++) {

      Node n = nl.item(i);
      NamedNodeMap nodeNamedMap = n.getAttributes();
      Node shortName = nodeNamedMap.getNamedItem("shortName");
      Node tolerates = nodeNamedMap.getNamedItem("tolerates");

      // Shortname version should already be in the list
      // Create the tolerated versions, if there are any
      if ((shortName != null) && (tolerates != null)) {
        int pos = shortName.getTextContent().lastIndexOf("-");
        if ((pos) < 0) {
          break; // No hyphen, this feature can't be versioned
        }
        String featureName = shortName.getTextContent().substring(0, pos);

        String[] versions = tolerates.getTextContent().split(",");
        for (int i2 = 0; i2 < versions.length; i2++) {
          enables.add(featureName + "-" + versions[i2]);
        }
      }
    }
  }

  public void setFormat(String fileExtension) {
    format = fileExtension;
  }
}
