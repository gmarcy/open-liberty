package com.ibm.ws.wlp.docgen.dita;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.events.ConfigTOCEvent;
import com.ibm.ws.wlp.docgen.dita.events.DescriptionEvent;
import com.ibm.ws.wlp.docgen.dita.events.EnabledByEvent;
import com.ibm.ws.wlp.docgen.dita.events.EnablesEvent;
import com.ibm.ws.wlp.docgen.dita.events.Event;
import com.ibm.ws.wlp.docgen.dita.events.EventType;
import com.ibm.ws.wlp.docgen.dita.events.FeatureEnablementEvent;
import com.ibm.ws.wlp.docgen.dita.events.IBMAPIPackageEvent;
import com.ibm.ws.wlp.docgen.dita.events.JavaVersionEvent;
import com.ibm.ws.wlp.docgen.dita.events.SPIPackageEvent;
import com.ibm.ws.wlp.docgen.dita.events.SpecAPIPackageEvent;
import com.ibm.ws.wlp.docgen.dita.events.SuperceededEvent;
import com.ibm.ws.wlp.docgen.dita.events.SymbolicNameEvent;
import com.ibm.ws.wlp.docgen.dita.events.ThirdPartyAPIPackageEvent;
import com.ibm.ws.wlp.docgen.dita.events.TitleEvent;

// TODO <featureManager><feature> is shown as a child element need to consider this. It shouldn't look like an attribute or a child element.
// TODO Need to correctly generate the child element configuration dita.
public class DitaFeatureVisitor implements FeatureVisitor {
  private Locale locale;
  private DITAHelper dita;
  private String lang;
  private File ditaTypes;
  private File dir;
  private boolean feature = true;
  
  private List<Event> events = new ArrayList<Event>();
  private Stack<String> config = new Stack<String>();
  private List<Tagging> editionTagging;
  private List<Tagging> featureTagging;
  private String editionName;

  public DitaFeatureVisitor(File featureDitaDir, File ditaTypes, Locale locale,
      String lang, String edition, List<Tagging> tagging, List<Tagging> featureTagging) {
    this.locale = locale;
    this.lang = lang;
    this.ditaTypes = ditaTypes;
    this.dir = featureDitaDir;
    this.editionName = edition;
    this.editionTagging = tagging;
    this.featureTagging = featureTagging;
  }

  @Override
  public void visitName(String name) throws IOException {
    try {
      dita = new DITAHelper(dir, "feature", name, ditaTypes, locale, lang, editionName, editionTagging, featureTagging);
    } catch (XMLStreamException e) {
      throw new IOException(e);
    } catch (FactoryConfigurationError e) {
      throw new IOException(e);
    }
  }


  @Override
  public void visitSymbolicName(String symbolicName) throws IOException {
    pushEvent(new SymbolicNameEvent(symbolicName));
  }

  @Override
  public void visitDescription(String description) throws IOException {
    pushEvent(new DescriptionEvent(description));
    if (feature) {
      pushEvent(new FeatureEnablementEvent());
    } // TODO validate that this doesn't require us to call for the refbody for the kernel.
  }

  @Override
  public void visitSuperceededBy(String feature) throws IOException {
    pushEvent(new SuperceededEvent(feature));
  }

  @Override
  public void visitEnables(String feature) throws IOException {
    pushEvent(new EnablesEvent(feature));
  }

  @Override
  public void visitEnabledBy(String feature) throws IOException {
    pushEvent(new EnabledByEvent(feature));
  }

  @Override
  public void visitAPIPackage(String packageName, String type, String javadocLink)
      throws IOException {
    if (type.startsWith("spec")) {
        pushEvent(new SpecAPIPackageEvent(packageName, javadocLink));
    } else if ("ibm-api".equals(type)) {
        pushEvent(new IBMAPIPackageEvent(packageName, javadocLink));
    } else if ("third-party".equals(type)) {
        pushEvent(new ThirdPartyAPIPackageEvent(packageName, javadocLink));
    	
    }
  }

  @Override
  public void visitSPIPackage(String packageName, String javadocLink)
      throws IOException {
    pushEvent(new SPIPackageEvent(packageName, javadocLink));
  }

  @Override
  // TODO type isn't used
  public void visitConfigElement(String name, String desc, String type)
      throws IOException {
    config = new Stack<String>();
    config.add(name);
    pushEvent(new ConfigTOCEvent(name));
  }

  @Override
  public void visitConfigAttribute(Attribute attribute) throws IOException {
  }

  @Override
  public void visitChildElement(String name, String type, String description,
      boolean required) throws IOException {
//    pushEvent(new ConfigTOCEvent(config, name));
//    config.add(name);
  }

  @Override
  public void done() throws IOException {
    Event last = null;
    
    try {
      Collections.sort(events, new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
          EventType t1 = EventType.from(o1.getClass());
          EventType t2 = EventType.from(o2.getClass());
          if (t1 == null) {
            throw new IllegalArgumentException("o1 (of type: " + o1.getClass() + " is not valid: " + o1);
          }
          int result = t1.compareTo(t2);
          if (result == 0) {
            result = o1.compareTo(o2);
          }
          
          return result;
        }
      });
      for (Event e : events) {
        // TODO Compare based on the event order enum
        if (last == null || last.getClass() != e.getClass()) {
          if (last != null) {
              last.postpareEvents(dita);
          }
          e.prepareEvents(dita);
        }
        processEvent(e, dita);
        last = e;
      }
      if (last != null) {
        last.postpareEvents(dita);
      }
      
      dita.done();
    } catch (XMLStreamException e1) {
      throw new IOException(e1);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void processEvent(Event e, DITAHelper dita2) throws XMLStreamException, IOException {
    try {
      e.processEvent(dita);
    } catch (XMLStreamException e1) {
      throw e1;
    } catch (IOException e1) {
      throw e1;
    }
  }

  @Override
  public void visitTitle(String displayName) throws IOException {
    pushEvent(new TitleEvent(displayName));
  }

  @Override
  public void endChildElement() throws IOException {
//    config.pop();
  }

  @Override
  public void visitKernel() {
    feature = false;
  }

  private void pushEvent(Event event) {
    events.add(event);
  }

  @Override
  public void visitJavaLevels(String javaLevel) {
    pushEvent(new JavaVersionEvent(javaLevel));
  }
}
