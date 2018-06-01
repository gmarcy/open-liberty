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
package com.ibm.ws.wlp.docgen.dita;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.wlp.docgen.dita.Feature.Package;
import com.ibm.ws.wlp.docgen.dita.events.FeatureEvent;

public class DitaGenTask extends FeatureListBasedTask {

  private static final String XSD = "http://www.w3.org/2001/XMLSchema";
  private static final String EXT = "http://www.ibm.com/xmlns/dde/schema/annotation/ext";

  private File ditaDir;
  private File schemaFile;
  private File ditaTypes;
  private Locale locale;
  private String localeName;
  private Map<String, String> entities;
  private String edition;
  private boolean createFeatureDoc;
  private List<Tagging> editionTagData = new ArrayList<Tagging>();
  private List<Tagging> featureTagData = new ArrayList<Tagging>();

  private static class Holder {
    public Holder(Element e) {
      type = e;
    }

    private final Element type;
  }

  private static Stack<String> types = new Stack<String>();

  @Override
  public void doExecute(List<Feature> features, List<Feature> kernel) {

    Map<String, Holder> elements = loadComplexTypes();
    Map<String, Holder> serverTypeElements = getServerElementChildren(elements);

    Map<String, List<String>> enabledBy = new HashMap<String, List<String>>();

    for (Feature f : features) {
      for (String featureName : f.enables) {
        List<String> list = enabledBy.get(featureName);
        if (list == null) {
          list = new ArrayList<String>();
          enabledBy.put(featureName, list);
        }
        list.add(f.name);
      }
    }

    // Get the country code and prepend -.
    String lang = toLang(locale.getLanguage(), locale.getCountry());

    File dir = new File(ditaDir, "".equals(localeName) ? "en" : localeName);
    dir.mkdirs();

    try {
      DitaMapWriter featureMapWriter = new DitaMapWriter(dir, "feature", "Features", edition, lang, featureTagData);
      DitaMapWriter configMapWriter = new DitaMapWriter(dir, "config", "Config", edition, lang, new ArrayList<Tagging>());
      EntitiesDITAWriter entitiesWriter = null;
      if ("".equals(localeName) && createFeatureDoc) {
        entitiesWriter = new EntitiesDITAWriter(new File(dir, "share"),
            entities);
      }

      Collections.sort(features, new Comparator<Feature>() {
        @Override
        public int compare(Feature o1, Feature o2) {
          return o1.displayName.compareTo(o2.displayName);
        }
      });
      
      FeatureEvent.addFeatures(features);

      for (Feature f : features) {
        // TODO currently generates feature doc for protected features.
        // Need to discuss this.
        featureMapWriter.addEntry(f.name, f.displayName);

        if (createFeatureDoc) {
          if (entitiesWriter != null) {
            entitiesWriter.addFeatureEntities(f.name, f.displayName);
          }

          writeFeatureDita(elements, serverTypeElements, enabledBy, lang, f);
        }
      }

      for (Map.Entry<String, Holder> element : serverTypeElements.entrySet()) {
        String elementName = element.getKey();
        Holder type = element.getValue();
        writeElementDita(elementName, type, elements, lang); // TODO add
                                                             // features that
                                                             // enable it
        configMapWriter.addEntry(elementName, getLabel(elementName, type.type, elements));
      }

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

      if (createFeatureDoc) {
        if (entitiesWriter != null) {
          entitiesWriter.addFeatureEntities(kernelFeature.name,
              kernelFeature.displayName);
        }

        writeFeatureDita(elements, serverTypeElements, enabledBy, lang,
            kernelFeature);
      }

      if (entitiesWriter != null) {
        entitiesWriter.close();
      }

      featureMapWriter.close();
      configMapWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Unable to setup writers for " + lang, e);
    }

    // TODO write dita for the edition

  }

  private void writeElementDita(String elementName, Holder h,
      Map<String, Holder> elements, String lang) {

    try {
      Visitor visitor = createConfigVisitor(lang);
      visitor.visitName(elementName);
      visitor.visitTitle(getLabel(null, h.type, elements)); // TODO extract the label
      visitor.visitDescription(getDescription(h.type, elements));

      // Visit config

      Element e = h.type;
      String type = e.getAttribute("name");
      try {
        // Process attributes
        List<Attribute> attributes = readAttributes(e, elements);

        visitAttributes(visitor, attributes, false);

        List<Child> children = readChildren(e, elements);

        for (Child child : children) {
          visitChildElement(visitor, child);
        }
      } catch (Exception ex) {
        throw new BuildException("An error occurred writing element "
            + elementName + " of type " + type, ex);
      }

      visitor.done();
    } catch (IOException e) {
      throw new BuildException("Error while completing the dita generation", e);
    }

  }

  private static String getLabel(String elementName, Element type, Map<String, Holder> elements) {
    NodeList nl = type.getElementsByTagNameNS(XSD, "extension");
    if (nl.getLength() > 0) {
      Node n = nl.item(0);
      if (n instanceof Element) {
        String typeName = ((Element) n).getAttribute("base");
        Holder h = elements.get(typeName);
        if (h != null) {
          type = elements.get(typeName).type;
        }
      }
    }

    nl = type.getElementsByTagNameNS(XSD, "appinfo");

    if (nl.getLength() == 0) {
      String typeName = type.getAttribute("type");
      Holder h = elements.get(typeName);
      if (h != null) {
        type = elements.get(typeName).type;
      }
      nl = type.getElementsByTagNameNS(XSD, "appinfo");
    }

    String desc = "";

    if (elementName != null) {
      desc = elementName + " - ";
    }
    
    if (nl.getLength() == 0) {
      desc = "";
    } else {
      nl = ((Element) nl.item(0)).getElementsByTagNameNS(EXT, "label");
      desc = desc + nl.item(0).getTextContent().trim();
    }
    
    return desc;
  }

  private Visitor createConfigVisitor(String lang) {
    ConfigVisitor visitor;
    try {
      File ditaLang = new File(ditaDir, (localeName.length() == 0) ? "en"
          : localeName);
      File featureDitaDir = new File(ditaLang, "ae");
      featureDitaDir.mkdirs();
      visitor = new ConfigVisitor(featureDitaDir, ditaTypes, locale, lang);
    } catch (Exception e) {
      throw new BuildException("Unable to create writer for file: " + ditaDir,
          e);
    }
    return visitor;
  }

  private String toLang(String language, String country) {
    if ("".equals(language)) {
      language = "en";
    }

    if ("".equals(country)) {
      if ("cs".equals(language)) {
        country = "cz";
      } else if ("ja".equals(language)) {
        country = "jp";
      } else if ("ko".equals(language)) {
        country = "kr";
      } else if ("zh".equals(language)) {
        country = "cn";
      } else if ("en".equals(language)) {
        country = "us";
      } else {
        country = language;
      }
    }

    return language + '-' + country;
  }

  private void writeFeatureDita(Map<String, Holder> elements,
      Map<String, Holder> serverTypeElements,
      Map<String, List<String>> enabledBy, String lang, Feature f) {

    try {
      FeatureVisitor visitor = createFeatureVisitor(lang);
      if ("kernel".equals(f.name)) {
        visitor.visitKernel();
      }
      visitor.visitName(f.name);
      visitor.visitTitle(f.displayName);
      visitor.visitDescription(f.description);
      if (f.symbolicName != null
          && !!!"com.ibm.websphere.appserver.javaeeClient-7.0"
              .equals(f.symbolicName)) {
        visitor.visitSymbolicName(f.symbolicName);
      }

      // Visit config

      List<String> configElements = f.configElement;

      Collections.sort(configElements);

      Iterator<String> it = configElements.iterator();
      // stip out elements we can't find as children of the serverType.
      while (it.hasNext()) {
        String name = it.next();
        Holder h = serverTypeElements.get(name);
        if (h == null) {
          it.remove();
        }
      }

      for (String name : configElements) {
        Holder h = serverTypeElements.get(name);
        if (h != null) {
          Element e = h.type;
          String type = e.getAttribute("name");
          String desc = getDescription(e, elements); // Get
          // description
          try {
            visitor.visitConfigElement(name, desc, type);

            // Process attributes
            List<Attribute> attributes = readAttributes(e, elements);

            visitAttributes(visitor, attributes, false);

            List<Child> children = readChildren(e, elements);

            for (Child child : children) {
              visitChildElement(visitor, child);
            }
          } catch (Exception ex) {
            throw new BuildException("An error occurred writing element "
                + name + " of type " + type, ex);
          }
        } else {
          getProject().log(
              "Unable to find element " + name
                  + " as a child of the server element", Project.MSG_INFO);
        }
      }

      if (f.superceeded) {
        for (String feature : f.superceededBy) {
          visitor.visitSuperceededBy(feature);
        }
      }

      if (!!!f.enables.isEmpty()) {
        for (String feature : f.enables) {
          visitor.visitEnables(feature);
        }
      }

      if (!!!f.javaLevels.isEmpty()) {
        for (String javaLevels : f.javaLevels) {
          visitor.visitJavaLevels(javaLevels);
        }
      }

      List<String> enabledByFeatures = enabledBy.get(f.name);

      if (enabledByFeatures != null && !!!enabledByFeatures.isEmpty()) {
        for (String feature : enabledByFeatures) {
          visitor.visitEnabledBy(feature);
        }
      }

      if (!!!f.apiPackages.isEmpty()) {
        for (Package p : f.apiPackages) {
          visitor.visitAPIPackage(p.packageName, p.packageType, p.javadocLink);
        }
      }

      if (!!!f.spiPackages.isEmpty()) {
        for (Package p : f.spiPackages) {
          visitor.visitSPIPackage(p.packageName, p.javadocLink);
        }
      }

      visitor.done();
    } catch (IOException e) {
      throw new BuildException("Error while completing the dita generation", e);
    }
  }

  private void visitAttributes(Visitor visitor, List<Attribute> attributes,
      boolean child) throws IOException {
    if (!!!attributes.isEmpty()) {
      for (Attribute attrib : attributes) {
        if (!!!child || (child && attrib.type == null)) {
          visitor.visitConfigAttribute(attrib);
        }
      }
    }
  }

  private void visitChildElement(Visitor visitor, Child child)
      throws IOException {
    visitor.visitChildElement(child.name, child.typeName, child.desc,
        child.required);
    if (child.attributes != null && !!!child.attributes.isEmpty()) {
      visitAttributes(visitor, child.attributes, true);
      for (Attribute attrib : child.attributes) {
        if (attrib.type != null) {
          visitChildElement(visitor, attrib.type);
        }
      }
    }
    visitor.endChildElement();
  }

  private FeatureVisitor createFeatureVisitor(String lang) {
    FeatureVisitor visitor;
    try {
      File ditaLang = new File(ditaDir, (localeName.length() == 0) ? "en"
          : localeName);
      File featureDitaDir = new File(ditaLang, "ae");
      featureDitaDir.mkdirs();
      visitor = new DitaFeatureVisitor(featureDitaDir, ditaTypes, locale, lang, edition, editionTagData, featureTagData);
    } catch (Exception e) {
      throw new BuildException("Unable to create writer for file: " + ditaDir,
          e);
    }
    return visitor;
  }

  private Map<String, Holder> getServerElementChildren(
      Map<String, Holder> elements) {
    Holder serverType = elements.get("serverType");
    NodeList nl = serverType.type.getElementsByTagNameNS(XSD, "element");

    Map<String, Holder> serverTypeElements = new TreeMap<String, Holder>();

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);

      if (n instanceof Element) {
        Element e = (Element) n;
        String name = e.getAttribute("name");
        String type = e.getAttribute("type");
        Holder h = elements.get(type);
        serverTypeElements.put(name, h);
      }
    }
    return serverTypeElements;
  }

  private Map<String, Holder> loadComplexTypes() {
    Map<String, Holder> elements = new HashMap<String, Holder>();
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document d;
    try {
      d = dbf.newDocumentBuilder().parse(schemaFile);
    } catch (Exception e) {
      throw new BuildException("Error parsing schema: " + schemaFile, e);
    }

    Element doc = d.getDocumentElement();
    NodeList nl = doc.getElementsByTagNameNS(XSD, "complexType");

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);

      if (n instanceof Element) {
        Element e = (Element) n;
        String nodeName = e.getAttribute("name");
        elements.put(nodeName, new Holder(e));
      }
    }
    return elements;
  }

  @Override
  public void validate() {
    if (ditaDir == null) {
      throw new BuildException(
          "You need to set the dita attribute for where to write the dita to");
    }

    if (schemaFile == null) {
      throw new BuildException(
          "You need to set the schema attribute for where to read the schema from");
    }

    if (ditaTypes == null) {
      throw new BuildException(
          "You need to set the typesResource attribute for where to read the types NLS data from");
    }

    if (locale == null) {
      throw new BuildException("You need to set the locale attribute");
    }

    if (entities == null) {
      throw new BuildException("You need to set the entities attribute");
    }
  }

  private static List<Child> readChildren(Element type,
      Map<String, Holder> elements) {
    List<Child> children = new ArrayList<Child>();

    NodeList nl = type.getElementsByTagNameNS(XSD, "element");

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Child c = readChild((Element) n, elements);
        if (c != null) {
          children.add(c);
        }
      }
    }

    nl = type.getElementsByTagNameNS(XSD, "extension");

    if (nl.getLength() > 0) {
      String baseType = ((Element) nl.item(0)).getAttribute("base");
      Element e = elements.get(baseType).type;
      children.addAll(readChildren(e, elements));
    }

    return children;
  }

  /**
   * @param elements
   * @param type
   * @return
   */
  private static List<Attribute> readAttributes(Element type,
      Map<String, Holder> elements) {
    List<Attribute> attributes = new ArrayList<Attribute>();

    NodeList nl = type.getElementsByTagNameNS(XSD, "attribute");

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Element e = (Element) n;
        Attribute attribute = new Attribute();
        attribute.name = e.getAttribute("name");
        if ("internal.properties".equals(attribute.name)) {
          continue;
        }
        attribute.optional = "optional".equals(e.getAttribute("use"));
        attribute.defaultValue = e.getAttribute("default");
        attribute.typeName = getType(e);

        Holder h = elements.get(attribute.typeName);

        if (h == null) {
          if ("".equals(attribute.typeName)) {
            attribute.typeName = null;
          }

          attribute.ref = getReference(e);

          attribute.min = getValue(e, "minInclusive");
          if (attribute.min == null) {
            attribute.min = getValue(e, "minLength");
          }

          attribute.max = getValue(e, "maxInclusive");
          if (attribute.max == null) {
            attribute.max = getValue(e, "maxLength");
          }

          NodeList enumValues = e.getElementsByTagNameNS(XSD, "enumeration");

          Map<String, String> enumData = new HashMap<String, String>();

          for (int j = 0; j < enumValues.getLength(); j++) {
            n = enumValues.item(j);
            if (n instanceof Element) {
              Element v = (Element) n;
              String name = v.getAttribute("value");
              String desc = getDescription(v, elements);
              enumData.put(name, desc);
            }
          }

          attribute.enumValues = enumData;

          attribute.variable = getVariable(e);

        } else {
          attribute.typeName = null;

          attribute.type = readChild(h.type, elements);
        }

        attribute.desc = getDescription(e, elements);

        attributes.add(attribute);
      }
    }

    nl = type.getElementsByTagNameNS(XSD, "extension");

    if (nl.getLength() > 0) {
      String baseType = ((Element) nl.item(0)).getAttribute("base");
      Element e = elements.get(baseType).type;
      attributes.addAll(readAttributes(e, elements));
    }

    return attributes;
  }

  private static String getType(Element e) {
    String typeName = e.getAttribute("type");
    if (typeName == null || typeName.length() == 0) {
      NodeList nl = e.getElementsByTagNameNS(XSD, "restriction");
      if (nl.getLength() == 1) {
        typeName = ((Element) nl.item(0)).getAttribute("base");
      }
    }

    if (typeName != null && typeName.startsWith("xsd:")) {
      typeName = typeName.substring(4);
    }

    return typeName;
  }

  private static String getValue(Element e, String element) {
    NodeList nl = e.getElementsByTagNameNS(XSD, element);

    if (nl.getLength() == 1) {
      return ((Element) nl.item(0)).getAttribute("value");
    }

    return null;
  }

  private static String getVariable(Element e) {
    NodeList nl = e.getElementsByTagNameNS(
        "http://www.ibm.com/xmlns/dde/schema/annotation/ext", "variable");
    String result = null;

    if (nl.getLength() > 0) {
      result = nl.item(0).getTextContent().trim();
    }

    return result;
  }

  /**
   * @param e
   * @return
   */
  private static String getReference(Element e) {
    NodeList nl = e.getElementsByTagNameNS(
        "http://www.ibm.com/xmlns/dde/schema/annotation/ext", "reference");
    String result = null;

    if (nl.getLength() > 0) {
      result = nl.item(0).getTextContent().trim();
    }

    return result;
  }

  /**
   * @param type
   * @return
   */
  private static Child readChild(Element type, Map<String, Holder> elements) {
    Child child = new Child();
    child.name = type.getAttribute("name");
    String typeName = type.getAttribute("type");

    Holder h = elements.get(typeName);

    if (h == null || types.contains(typeName)) {
      child.typeName = typeName;
    } else {
      types.push(typeName);
      child.attributes = readAttributes(h.type, elements);
      List<Child> children = readChildren(h.type, elements);

      for (Child c : children) {
        Attribute a = new Attribute();
        a.name = c.name;
        a.desc = c.desc;
        a.type = c;
        child.attributes.add(a);
      }

      types.pop();
    }

    child.desc = getDescription(type, elements);

    return child;
  }

  /**
   * @param type
   * @param elements
   * @return
   */
  private static String getDescription(Element type,
      Map<String, Holder> elements) {
    NodeList nl = type.getElementsByTagNameNS(XSD, "extension");
    if (nl.getLength() > 0) {
      Node n = nl.item(0);
      if (n instanceof Element) {
        String typeName = ((Element) n).getAttribute("base");
        Holder h = elements.get(typeName);
        if (h != null) {
          type = elements.get(typeName).type;
        }
      }
    }

    nl = type.getElementsByTagNameNS(XSD, "documentation");

    if (nl.getLength() == 0) {
      String typeName = type.getAttribute("type");
      Holder h = elements.get(typeName);
      if (h != null) {
        type = elements.get(typeName).type;
      }
      nl = type.getElementsByTagNameNS(XSD, "documentation");
    }

    String desc;

    if (nl.getLength() == 0) {
      desc = "";
    } else {
      desc = nl.item(0).getTextContent().trim();
    }

    return desc;
  }

  public void setDitaDir(File dita) {
    this.ditaDir = dita;
  }

  public void setSchema(File schemaFile) {
    this.schemaFile = schemaFile;
  }

  public void setTypesResource(File typesResource) {
    ditaTypes = typesResource;
  }

  public void setLocale(String locale) {
    localeName = locale;
    String[] localeElements = locale.split("_");
    if (localeElements.length == 1) {
      this.locale = new Locale(localeElements[0]);
    } else if (localeElements.length == 2) {
      this.locale = new Locale(localeElements[0], localeElements[1]);
    } else if (localeElements.length == 3) {
      this.locale = new Locale(localeElements[0], localeElements[1],
          localeElements[2]);
    } else if (localeElements.length == 0) {
      throw new BuildException("Can't process the locale string " + locale);
    }
  }

  public void setEntities(File file) throws FileNotFoundException, IOException {
    Properties props = new Properties();
    props.load(new FileReader(file));

    entities = new HashMap<String, String>();

    @SuppressWarnings("unchecked")
    Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();

    while (propNames.hasMoreElements()) {
      String name = propNames.nextElement();
      entities.put(name, props.getProperty(name));
    }
  }

  public void setCreateFeatureDoc(boolean genFeatureDoc) {
    createFeatureDoc = genFeatureDoc;
  }

  public void setEdition(String editionName) {
    edition = editionName;
  }
  
  public Tagging createEditionTags() {
    Tagging tag = new Tagging();
    editionTagData.add(tag);
    return tag;
  }
  
  public Tagging createFeatureTags() {
    Tagging tag = new Tagging();
    featureTagData.add(tag);
    return tag;
  }
}
