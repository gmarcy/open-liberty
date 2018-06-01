package com.ibm.ws.wlp.docgen.selector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.selectors.BaseExtendSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TocBasedSelector extends BaseExtendSelector {

  private List<String> hrefsToGet = new ArrayList<String>();
  private boolean verified;

  @Override
  public boolean isSelected(File arg0, String arg1, File arg2)
      throws BuildException {
    if (!verified) {
      verifySettings();
    }
	
	//If we're on windows this needs to happen.
	String normalized = arg1.replace("\\", "/");

	boolean found = false;
	//There is probably a more efficient way to do this
	for (String item : hrefsToGet) {
		if (normalized.contains(item)) {
			//System.out.println("Adding: " + arg1);
			found=true;
			break;
		}
	}
	
	//System.out.println("Arg to search: " + normalized + " found? " + found);
	
    return found;
  }

  @Override
  public void verifySettings() {
    if (verified) return;
    super.verifySettings();

    File tocFile = null;
    Parameter[] params = getParameters();

    for (Parameter p : params) {
      if ("toc.file".equals(p.getName())) {
        String fileName = p.getValue();
        String basedir = getProject().getProperty("basedir");
        tocFile = new File(basedir, fileName);
      }
    }
    
    if (tocFile == null) {
      throw new BuildException("toc.file parameter must be specified");
    }
    
    if (!tocFile.exists()) {
      throw new BuildException("toc.file does not exist: " + tocFile);
    }
    
    try {
      Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(tocFile);
      NodeList nl = d.getElementsByTagName("topic");
      for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        Node hrefNode = n.getAttributes().getNamedItem("href");
        if (hrefNode != null) {
          String href = hrefNode.getTextContent();
          hrefsToGet.add(href);
        }
      }
    } catch (SAXException e) {
      throw new BuildException("unable to parse " + tocFile, e);
    } catch (IOException e) {
      throw new BuildException("unable to read " + tocFile, e);
    } catch (ParserConfigurationException e) {
      throw new BuildException("unable to parse " + tocFile, e);
    }

    verified = true;
  }
}