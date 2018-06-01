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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GenerateAdocForConfig extends AdocGeneratorTask {

  private static Stack<String> types = new Stack<String>();
  private static Stack<String> names = new Stack<String>();

  private static final String EXT = "http://www.ibm.com/xmlns/dde/schema/annotation/ext";
  private static final String XSD = "http://www.w3.org/2001/XMLSchema";

  /** Pattern that checks for numeric HTML encoding. */
  private static final Pattern ENCODING_PATTERN = Pattern.compile("&\\#\\d{2,3};.*");

  private String format = "adoc";

  private static class Holder {
    public Holder(Element e) {
      type = e;
    }

    private final Element type;
  }

  public void doExecute() {

    Map<String, Holder> elements = loadComplexTypes();
    Map<String, Holder> serverElements = getServerElementChildren(elements);

    Map<String, String> elementNames = new HashMap<String, String>();

    for (Map.Entry<String, Holder> element : serverElements.entrySet()) {
      String elementName = element.getKey();
      Holder type = element.getValue();
      String label = getLabel(elementName, type.type, elements);
      elementNames.put(elementName, label);
      configManifest.setProperty(elementName, label);
      writeMarkdown(elementName, label, type.type, elements);
    }
  }

  private void writeMarkdown(String elementName, String label, Element type,
      Map<String, Holder> elements) {

    try {
      Map<String, String> formatAttributes = new HashMap<String, String>();
      formatAttributes.put("stylesheet", "../config.css");
      formatAttributes.put("nofooter", "");
      PrintStream out = new PrintStream(new File(dir, "rwlp_config_" + elementName + "." + format));
      Formatter f = Formatter.Factory.create(format, formatAttributes, out);
      f.header(1).text(label).raw(" (").text(elementName).raw(")").end();
      f.text(getDescription(type, elements)).next().next();

      List<Attribute> attributes = readAttributes(type, elements);
      List<Child> children = readChildren(type, elements);

      writeType(f, attributes, children, elements, elementName);
      out.close();
    } catch (FileNotFoundException e) {
      throw new BuildException(e.getMessage());
    }
  }

  private void writeType(Formatter f, List<Attribute> attributes,
      List<Child> children, Map<String, Holder> elements, String rootElementName) {
    writeAttributesTable(f, attributes);

    if (!!!children.isEmpty()) {
      for (Child child : children) {
        addToManifest(rootElementName, child.breadrum);
        f.referable(child.id).bold().raw(child.breadrum).end().next().next();
        f.text(child.desc).next().next().next();
        if (child.attributes != null && !!!child.attributes.isEmpty()) {
          List<Attribute> childAttributes = new ArrayList<Attribute>();
          List<Child> grandChildren = new ArrayList<Child>();
          for (Attribute a : child.attributes) {
            if (a.type == null) {
              childAttributes.add(a);
            } else {
              grandChildren.add(a.type);
            }
          }

          writeType(f, childAttributes, grandChildren, elements, rootElementName);
        }
      }
    }
  }

  private void writeAttributesTable(Formatter f, List<Attribute> attributes) {
    if (!!!attributes.isEmpty()) {
      String nameLabel = getNLS("Name");
      String typeLabel = getNLS("Type");
      String defaultLabel = getNLS("Default");
      String descriptionLabel = getNLS("Description");
      String maxLabel = getNLS("Max");
      String minLabel = getNLS("Min");

      f.table(4).tableHeader().tableCell().raw(nameLabel).tableCell().raw(typeLabel).tableCell().raw(defaultLabel).tableCell().raw(descriptionLabel);

      List<String> groups = new ArrayList<String>();

      for (Attribute a : attributes) {
        if (a.group != null && !a.group.equals("") && !groups.contains(a.group)) {
          f.tableRow().tableCell(4).bold().text(a.group).end().end().end();
          groups.add(a.group);
        }
        f.tableRow().tableCell().text(a.name).end();
        f.tableCell();
        if (a.enumValues != null && !!!a.enumValues.isEmpty()) {
          for (String key : a.enumValues.keySet()) {
            f.bullet().text(key).end();
          }
          f.end();
        } else if (a.typeName != null) {
          f.raw(getNLS(a.typeName, a.ref));
          if (a.min != null) {
            f.nl().raw(minLabel).raw(": ").text(a.min).nl();
          }
          if (a.max != null) {
            f.nl().raw(maxLabel).raw(": ").text(a.max).nl();
          }
        }
        f.tableCell().text(a.defaultValue);
        f.tableCell().text(a.desc);
        if (a.enumValues != null && !!!a.enumValues.isEmpty()) {
          for (Map.Entry<String, String> value : a.enumValues.entrySet()) {
            String k = value.getKey();
            String v = value.getValue();
            if (!k.equalsIgnoreCase(v) && !k.replaceAll(" ", "").equalsIgnoreCase(v.replaceAll(" ", ""))) {
              f.nl();
              f.bold().text(k).end();
              f.nl().text("  " + v);
            }
          }
        }
      }
      f.next();
    }
  }

  private void printDash(PrintStream out, int length) {
    for (int i = 0; i < length; i++) {
      out.print('-');
    }
  }

  private Map<String, Holder> loadComplexTypes() {
    Map<String, Holder> elements = new HashMap<String, Holder>();
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document d;
    try {
      d = dbf.newDocumentBuilder().parse(file);
    } catch (Exception e) {
      throw new BuildException("Error parsing schema: " + file, e);
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

    Collections.sort(children);

    return children;
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
   * @param type
   * @return
   */
  private static Child readChild(Element type, Map<String, Holder> elements) {
    Child child = new Child();
    child.name = type.getAttribute("name");
    child.breadrum = toBreadcrumb(names, child.name);
    child.id = toId(names, child.name);
    String typeName = type.getAttribute("type");

    Holder h = elements.get(typeName);

    if (h == null || types.contains(typeName)) {
      child.typeName = typeName;
    } else {
      types.push(typeName);
      names.push(child.name);
      child.attributes = readAttributes(h.type, elements);
      List<Child> children = readChildren(h.type, elements);

      for (Child c : children) {
        Attribute a = new Attribute();
        a.breadcrum = toBreadcrumb(names, c.name);
        c.id = toId(names, c.name);
        a.name = c.name;
        c.breadrum = a.breadcrum;
        a.desc = c.desc;
        a.type = c;
        child.attributes.add(a);
      }

      types.pop();
      names.pop();
    }

    child.desc = getDescription(type, elements);

    return child;
  }

  private static String toId(Stack<String> names2, String name) {
    StringBuilder builder = new StringBuilder();
    for (String n : names2) {
      builder.append(n);
      builder.append("/");
    }
    builder.append(name);
    return builder.toString();
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

  /**
   * @param elements
   * @param type
   * @return
   */
  private static List<Attribute> readAttributes(Element type,
      Map<String, Holder> elements) {
    List<Attribute> attributes = new ArrayList<Attribute>();

    Map<String, String> groups = new HashMap<String, String>();

    NodeList nl = type.getElementsByTagNameNS(EXT, "groupDecl");

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        Element e = (Element)n;
        String id = e.getAttribute("id");
        String label = e.getAttribute("label");
        groups.put(id, label);
      }
    }

    nl = type.getElementsByTagNameNS(XSD, "attribute");

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

          Map<String, String> enumData = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String one, String two) {
              try {
                int oneInt = Integer.parseInt(one);
                int twoInt = Integer.parseInt(two);
                return oneInt - twoInt;
              } catch (NumberFormatException nfe) {
                // Ignore
              }
              return one.compareTo(two);
            }
          });

          for (int j = 0; j < enumValues.getLength(); j++) {
            n = enumValues.item(j);
            if (n instanceof Element) {
              Element v = (Element) n;
              String name = v.getAttribute("value");
              String desc = getDescription(v, elements);
              enumData.put(name, desc);
            }
          }

          NodeList group = e.getElementsByTagNameNS(EXT, "group");
          if (group.getLength() == 1) {
            String id = ((Element)group.item(0)).getAttribute("id");
            attribute.group = groups.get(id);
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

    Collections.sort(attributes);

    return attributes;
  }

  private void addToManifest(String rootElementName, String breadcrum) {
    breadcrum = breadcrum.replaceAll(" > ", "/");
    breadcrum = rootElementName + "/" + breadcrum;
    configManifest.put(breadcrum, breadcrum);
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

    desc = desc.replaceAll("<","&lt;").replaceAll(">","&gt;");

    return desc;
  }

  private static String getLabel(String elementName, Element type,
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

  public static String toBreadcrumb(List<String> context, String elementName) {
    StringBuilder builder = new StringBuilder();

    for (String element : context) {
      builder.append(element);
      builder.append(" > ");
    }

    builder.append(elementName);

    return builder.toString();
  }

  public void setFormat(String fileExtension) {
    format = fileExtension;
  }
}
