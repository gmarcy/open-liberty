package com.ibm.ws.wlp.docgen.toc;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.ibm.ws.wlp.docgen.dita.Feature;
import com.ibm.ws.wlp.docgen.dita.FeatureListBasedTask;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class FeatureConfigToc extends FeatureListBasedTask {

  private File dir;
  private File configManifest;
  private String nlsPrefix;
  private String lang;
  private Properties rb = new Properties();

  private static class LinkData implements Comparable<LinkData> {
    private String label;
    private String link;
    private String fragment;
    private Map<String, LinkData> children = new HashMap<String, LinkData>();

    public LinkData(String theLabel, String theLink) {
      label = theLabel;
      link = theLink;
    }

    public LinkData(String theLabel, String theLink, String theFragment) {
      label = theLabel;
      link = theLink;
      fragment = theFragment;
    }

    public String getLabel() {
      return label;
    }

    public String getLink(String type) {
      StringBuilder builder = new StringBuilder();
      builder.append("ae/rwlp_");
      builder.append(type);
      builder.append('_');
      builder.append(link);
      builder.append(".html");
      if (fragment != null) {
        builder.append('#');
        builder.append(fragment);
      }
      return builder.toString();
    }

    public LinkData addChild(String element) {
      LinkData data = children.get(element);
      if (data == null) {
        String fragment = (this.fragment == null) ? "" : (this.fragment + '/');
        data = new LinkData(element, link, fragment + element);
        children.put(element, data);
      }
      return data;
    }

    public void addChildren(Map<String, LinkData> children) {
      this.children.putAll(children);
    }

    public Collection<LinkData> getChildren() {
      List<LinkData> result = new ArrayList<LinkData>(children.values());
      Collections.sort(result);
      return result;
    }

    public boolean hasChildren() {
      return !children.isEmpty();
    }

    @Override
    public int compareTo(LinkData o) {
      return label.compareTo(o.label);
    }
  }

  @Override
  public void validate() {
    if (dir == null) {
      throw new BuildException("dir must be set");
    }

    if (configManifest == null) {
      throw new BuildException("configManifest must be set");
    }

    String langDirName = lang.replaceAll("_", "/");

    String tocDirName = dir.getAbsolutePath();
    tocDirName = tocDirName.replaceAll(lang, langDirName);
    dir = new File(tocDirName);

    dir.mkdirs();

    // The language code uses _ for language variants, like Brazilian Portuguese
    // or the difference between traditional and simplified chinese, but the KC
    // needs them to be in child directories, so we just replace any instances.
    configManifest = new File(configManifest.getAbsolutePath().replaceAll(lang, langDirName));

    if (!!!configManifest.exists()) {
      File parentFile = configManifest.getParentFile();
      String parentDirName = parentFile.getName();
      if (parentDirName.contains("_")) {
        configManifest = new File(parentFile.getParentFile(), parentDirName.replaceAll("_", "/") + "/" + configManifest.getName());
      }

      if (!!!configManifest.exists()) {
        throw new BuildException("configManifest " + configManifest + " does not exist");
      }
    }

    String fileName = nlsPrefix + ((lang.length() == 0) ? "" : '_' + lang) + ".nlsprops";
    try {
      String basedir = getProject().getProperty("basedir");
      rb.load(new FileReader(new File(basedir, fileName)));
    } catch (IOException e) {
      throw new BuildException(e.getMessage(), e);
    }
  }

  @Override
  public void doExecute(List<Feature> list, List<Feature> kernel) {

    Set<String> config = new HashSet<String>();
    Set<LinkData> featureLinks = new TreeSet<LinkData>();
    Set<LinkData> configLinks = new TreeSet<LinkData>();

    for (Feature f : list) {
      featureLinks.add(new LinkData(f.displayName, f.name));
      config.addAll(f.configElement);
    }

    for (Feature k : kernel) {
      config.addAll(k.configElement);
    }

    for (Feature auto : loadFeatures("autoFeature")) {
      config.addAll(auto.configElement);
    }

    Properties props = new Properties();
    try {
      props.load(new FileReader(configManifest));
    } catch (IOException e) {
      throw new BuildException("Failed to read config manifest", e);
    }

    Map<String, List<String>> childElements = new HashMap<String, List<String>>();
    for (Object key : props.keySet()) {
      String xpath = (String)key;
      int index = xpath.indexOf('/');
      if (index != -1) {
        String root = xpath.substring(0, index);
        String path = xpath.substring(index + 1);
        List<String> childPaths = childElements.get(root);
        if (childPaths == null) {
          childPaths = new ArrayList<String>();
          childElements.put(root, childPaths);
        }
        childPaths.add(path);
      }
    }

    for (String element : config) {
      String label = props.getProperty(element);
      if (label == null) {
        log("Unable to find label for element {" + element + "}.", Project.MSG_ERR);
      } else {
        LinkData data = new LinkData(label, element);
        configLinks.add(data);
        List<String> children = childElements.get(element);
        if (children != null) {
          Collections.sort(children);
          for (String child : children) {
            String[] childComponents = child.split("/");
            String root = childComponents[0];
            LinkData ld = data.addChild(root);
            for (int i = 1; i < childComponents.length; i++) {
              ld = ld.addChild(childComponents[i]);
            }
          }
        }
      }
    }

    writeToc(new File(dir, "toc-feature.xml"), "feature", featureLinks);
    writeToc(new File(dir, "toc-config.xml"), "config", configLinks);
  }

  public void writeToc(File toc, String linkKind, Collection<LinkData> data) {
    if (data.isEmpty()) return;
    try {
      XMLWriter writer = new XMLWriter(toc);
      writer.writeProcessingInstruction("APT Element gi=\"document2\"");
      writer.writeProcessingInstruction("APT Element gi=\"document1\"");
      writer.writeProcessingInstruction("APT Element gi=\"document\"");
      writer.writeProcessingInstruction("APT Element gi=\"toc\" attrs=\"label link_to\"");
      writer.writeProcessingInstruction("APT Element gi=\"topic\" attrs=\"label href\"");
      writer.writeProcessingInstruction("APT Element gi=\"link\" attrs=\"toc\"");
      writer.writeProcessingInstruction("NLS TYPE=\"org.eclipse.help.toc\"");

      // <toc label="Liberty Javadocs" link_to="../<infocenterId>/<xref" />
      writer.pushElement("toc");
      writer.addAttribute("label", getNLS("toc.toplevel.label"));
      writeLinks(data, linkKind, writer);
      writer.pop("toc"); // toc
      writer.close();
    } catch (IOException e) {
      throw new BuildException(e);
    } catch (XMLStreamException e) {
      throw new BuildException(e);
    } catch (FactoryConfigurationError e) {
      throw new BuildException(e);
    }
  }

  private void writeLinks(Collection<LinkData> links, String kind, XMLWriter writer)
      throws XMLStreamException, IOException {
    for (LinkData link : links) {
      writer.pushElement("topic");
      // TODO write label
      writer.addAttribute("label", link.getLabel());
      writer.addAttribute("href", link.getLink(kind));
      if (link.hasChildren()) {
        writeLinks(link.getChildren(), kind, writer);
      }
      writer.pop("topic"); // topic
    }
  }

  private String getNLS(String key, Object... inserts) {
    String result = rb.getProperty(key, key);
    if (inserts != null && inserts.length > 0) {
      result = MessageFormat.format(result, inserts);
    }
    return result;
  }

  public void setDir(File tocDir) {
    dir = tocDir;
  }

  public void setConfigManifest(File f) {
    configManifest = f;
  }

  public void setNLS(String file) {
    nlsPrefix = file;
  }

  public void setLang(String theLang) {
    lang = theLang;
  }
}
