package com.ibm.ws.wlp.docgen.dita;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.wlp.docgen.dita.Feature.Package;

public abstract class FeatureListBasedTask extends Task {
  private File featureList;
  private File javadocs;

  public final void execute() {
    featureListValidate();
    validate();
    
    List<Feature> features = loadFeatures("feature");
    List<Feature> kernel = loadFeatures("kernelFeature");
    
    doExecute(features, kernel);
  }
  
  private void featureListValidate() {
    if (featureList == null) {
      throw new BuildException("The featureList attribute must be specified");
    }
    
    if (javadocs == null) {
      throw new BuildException("You need to set the javadocDir attribute");
    }
    
  }

  public abstract void validate();
  public abstract void doExecute(List<Feature> list, List<Feature> kernel);

  public void setFeatureList(File featureList) {
    this.featureList = featureList;
  }

  public void setJavadocDir(File dir) {
    javadocs = dir;
  }
  
  protected File getJavadocDir() {
    return javadocs;
  }

  private String getElementText(Element e, String elementName) {
    NodeList nl = e.getElementsByTagName(elementName);
    
    if (nl.getLength() == 1) {
      return nl.item(0).getTextContent();
    }
    
    return null;
  }

  protected List<Feature> loadFeatures(String element) {
    Map<String, String> javadocLinks = loadJavadocLinks();
    List<Feature> features =  new ArrayList<Feature>();
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document d;
    try {
      d = dbf.newDocumentBuilder().parse(featureList);
    } catch (Exception e) {
      throw new BuildException("Error parsing featureList: " + featureList, e);
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
        loadPackages(e, "apiPackage", f.apiPackages, javadocLinks);
        loadPackages(e, "spiPackage", f.spiPackages, javadocLinks);
  
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

  private Map<String, String> loadJavadocLinks() {
  
    Map<String, String> javadocLinks = new HashMap<String, String>();
  
    File[] dirs = javadocs.listFiles(new FileFilter() {
      @Override
      public boolean accept(File arg0) {
        return arg0.isDirectory();
      }
    });
  
    if (dirs != null) {
      for (File dir : dirs) {
        File f = new File(dir, "package-list");
        try {
          BufferedReader reader = new BufferedReader(new FileReader(f));
          String line;
  
          while ((line = reader.readLine()) != null) {
            String packageName = line;
            line = line.replaceAll("\\.", "/");
            
            String link = javadocs.getName() + "/" + dir.getName() + "/" + line + "/package-summary.html";
  
            javadocLinks.put(packageName, link);
          }
        } catch (IOException e) {
          throw new BuildException("Unable to read: " + f, e);
        }
      }
    }
  
    return javadocLinks;
  }

  private void loadPackages(Element e, String elementName, List<Package> packages, Map<String, String> links) {
    NodeList nl = e.getElementsByTagName(elementName);
    
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Element p = (Element) n;
        String packageName = n.getTextContent();
        String type = p.getAttribute("type");
        if (!!!"internal".equals(type)) {
          packages.add(new Package(packageName, type, links.get(packageName)));
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
		  if ((shortName != null) && (tolerates != null))  {
			  int pos = shortName.getTextContent().lastIndexOf("-");
			  if ((pos) < 0) {
				  break; // No hyphen, this feature can't be versioned
			  }
			  String featureName = shortName.getTextContent().substring(0, pos); 

			  String[] versions = tolerates.getTextContent().split(",");
			  for (int i2 = 0; i2 < versions.length;i2++) {
				  enables.add(featureName + "-" + versions[i2]);
			  }
		  }
	  }
  }
}
