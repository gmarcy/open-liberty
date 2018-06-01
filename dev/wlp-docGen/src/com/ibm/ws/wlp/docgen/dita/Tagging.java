package com.ibm.ws.wlp.docgen.dita;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Tagging {
  private String editionName;
  private List<String> features;
  private Map<String, String> tags = new HashMap<String, String>();

  public void setName(String edition) {
    editionName = edition;
  }
  
  public void setAudience(String audience) {
    tags.put("audience", audience);
  }
  
  public void setPlatform(String platform) {
    tags.put("platform", platform);
  }
  
  public void setProduct(String product) {
    tags.put("product", product);
  }

  public String getName() {
    return editionName;
  }

  public Map<String, String> getTags() {
    return tags;
  }
  
  public boolean matchFeature(File dir, String edition, String feature) {
    if (edition.equals(editionName)) {
      return true;
    }
    ensureFeatureMapRead(dir);
    
    if (features == null) {
      return true;
    }
    
    return features.contains(feature);
  }

  private void ensureFeatureMapRead(File dir) {
    File ditaFile = DitaMapWriter.getDitaMap(dir.getParentFile(), "feature", editionName);
    if (features == null && ditaFile.exists()) {
      features = new ArrayList<String>();
      XMLStreamReader reader;
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document d = factory.newDocumentBuilder().parse(ditaFile);
        NodeList nl = d.getElementsByTagName("topicref");
        
        for (int i = 0; i < nl.getLength(); i++) {
          Element e = (Element) nl.item(i);
          String href = e.getAttribute("href");
          href = href.substring(16, href.lastIndexOf('.'));
          features.add(href);
        }
        
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (FactoryConfigurationError e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (SAXException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
  }
}
