package com.ibm.ws.wlp.docgen.toc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.tools.ant.BuildException;

import com.ibm.ws.wlp.docgen.dita.Feature;
import com.ibm.ws.wlp.docgen.dita.FeatureListBasedTask;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class JavadocToc extends FeatureListBasedTask {

  private File toc;
  private String infocenterId;
  private String xref = "nav_ref.xml#qref_adm_javadoc_bottom";
  private File install;
 
  // Strip off the micro version
  private static final Pattern FOLDER_PATTERN = Pattern.compile("/([^/]*)\\.[0-9]+\\.jar");

  private static final class JavadocLink implements Comparable<JavadocLink> {
    private String name;
    private String label;
    public JavadocLink(String name, String label) {
      this.name = name;
      this.label = label;
    }
    
    @Override
    public int compareTo(JavadocLink o) {
      return label.compareTo(o.label);
    }
  }
  
  private boolean processJavadocLinks(List<Feature> features, Set<JavadocLink> apis, Set<JavadocLink> spis) {
    boolean javadocFound = false;
    for (Feature f : features) {
      javadocFound |= processJavadocLinks(f.apiJar, apis);
      javadocFound |= processJavadocLinks(f.spiJar, spis);
    }
    return javadocFound;
  }
  
  @Override
  public void doExecute(List<Feature> list, List<Feature> kernel) {
    boolean writeDoc = false;
    Set<JavadocLink> apiJavadoc = new TreeSet<JavadocLink>();
    Set<JavadocLink> spiJavadoc = new TreeSet<JavadocLink>();

    writeDoc |= processJavadocLinks(list, apiJavadoc, spiJavadoc);
    writeDoc |= processJavadocLinks(kernel, apiJavadoc, spiJavadoc);
    writeDoc |= processJavadocLinks(loadFeatures("autoFeature"), apiJavadoc, spiJavadoc);

    if (writeDoc) {
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
        writer.addAttribute("label", "Liberty Javadocs");
        writer.addAttribute("link_to", "../" + infocenterId + "/" + xref );
        if (!!!apiJavadoc.isEmpty()) {
          writer.pushElement("topic");
          writer.addAttribute("label", "Liberty API");
          writer.addAttribute("href", "about:blank");

          writeJavadocLinks(apiJavadoc, writer);
          writer.pop("topic"); // topic
        }
        if (!!!spiJavadoc.isEmpty()) {
          writer.pushElement("topic");
          writer.addAttribute("label", "Liberty SPI");
          writer.addAttribute("href", "about:blank");

          writeJavadocLinks(spiJavadoc, writer);
          writer.pop("topic"); // topic
        }
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
  }

  private void writeJavadocLinks(Set<JavadocLink> apiJavadoc, XMLWriter writer)
      throws XMLStreamException, IOException {
    for (JavadocLink javadocLink : apiJavadoc) {
      writer.pushElement("topic");
      writer.addAttribute("label", javadocLink.label);
      writer.addAttribute("href", javadocLink.name + "/index.html");
      writer.pop("topic"); // topic
    }
  }

  private boolean processJavadocLinks(List<String> jars, Set<JavadocLink> apiJavadoc) {
    boolean writeDoc = false;
    for (String jar : jars) {
      Matcher matcher = FOLDER_PATTERN.matcher(jar);
      if (!matcher.find()) {
        throw new BuildException("Could not work out the javadoc folder name for " + jar);
      }
      String folder = matcher.group(1) + "-javadoc";

      File javadocDir = new File(getJavadocDir(), folder);
      if (javadocDir.exists()) {
       apiJavadoc.add(new JavadocLink(javadocDir.getName(), getLabel(new File(jar))));
        writeDoc = true;
      }
    }
    return writeDoc;
  }

  private String getLabel(File jarFile) {
    
    String jarName = jarFile.getName();
    File matchedJar = null;
    
    matchedJar = findJar(jarName, new File(install, "dev/api"));
    if (matchedJar == null) {
      matchedJar = findJar(jarName, new File(install, "dev/spi"));
    }

    if (matchedJar == null) {
      throw new BuildException("Unable to find " + jarName + " (looked in " + install +")");
    }
    
    try {
      JarFile jar = new JarFile(matchedJar);
      Manifest man = jar.getManifest();
      return man.getMainAttributes().getValue("Bundle-Name");
    } catch (IOException e1) {
      throw new BuildException("Unable to open jar file " + matchedJar, e1);
    }
  }

  private File findJar(final String jarName, File apis) {
    File[] dirs = apis.listFiles();
    
    if (dirs != null) {
      for (File f : dirs) {
        if (f.isDirectory()) {
          File possibleJar = new File(f, jarName);
          if (possibleJar.exists()) {
            return possibleJar;
          }
        }
      }
    }
    
    return null;
  }

  public void validate() {
    if (toc == null) {
      throw new BuildException("The toc attribute must be specified");
    }
    
    if (infocenterId == null) {
      throw new BuildException("The infocenter attribute must be specified");
    }
    
    if (install == null) {
      throw new BuildException("The installDir for a liberty install was not provided");
    }
  }

  public void setInstallDir(File install) {
    this.install = install;
  }
  
  public void setToc(File toc) {
    this.toc = toc;
  }

  public void setInfocenter(String id) {
    infocenterId = id;
  }

  public void setInfocenterId(String infocenterId) {
    this.infocenterId = infocenterId;
  }

  public void setXref(String xref) {
    this.xref = xref;
  }
}
