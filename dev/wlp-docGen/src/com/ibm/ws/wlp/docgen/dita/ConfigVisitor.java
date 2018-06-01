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

import com.ibm.ws.wlp.docgen.dita.events.ConfigAttributeEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigChildAttributeEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigChildElementEndEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigChildElementEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigElementEndEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigElementEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigTOCEvent;
import com.ibm.ws.wlp.docgen.dita.events.DescriptionEvent;
import com.ibm.ws.wlp.docgen.dita.events.Event;
import com.ibm.ws.wlp.docgen.dita.events.EventType;
import com.ibm.ws.wlp.docgen.dita.events.TitleEvent;

public class ConfigVisitor implements Visitor {
  private List<Event> events = new ArrayList<Event>();
  private DITAHelper dita;
  private File dir;
  private File ditaTypes;
  private Locale locale;
  private String lang;
  private Stack<String> config = new Stack<String>();
  
  public ConfigVisitor(File featureDitaDir, File ditaTypes2, Locale locale2,
      String lang2) {
    dir = featureDitaDir;
    ditaTypes = ditaTypes2;
    locale = locale2;
    lang = lang2;
  }

  @Override
  public void visitName(String name) throws IOException {
    try {
      List<Tagging> tagging = Collections.emptyList();
      dita = new DITAHelper(dir, "config", name, ditaTypes, locale, lang, "", tagging, tagging);
    } catch (XMLStreamException e) {
      throw new IOException(e);
    } catch (FactoryConfigurationError e) {
      throw new IOException(e);
    }
  }

  @Override
  public void visitDescription(String description) throws IOException {
    pushEvent(new DescriptionEvent(description, true));
  }

  @Override
  public void visitConfigElement(String name, String desc, String type)
      throws IOException {
    config = new Stack<String>();
    config.add(name);
    pushEvent(new ConfigElementEvent(name, desc));
    pushEvent(new ConfigElementEndEvent(name));
    pushEvent(new ConfigTOCEvent(name));
  }

  @Override
  public void visitConfigAttribute(Attribute attribute) throws IOException {
    pushEvent(new ConfigAttributeEvent(config, attribute));
  }

  @Override
  public void visitChildElement(String name, String type, String description,
      boolean required) throws IOException {
    
    pushEvent(new ConfigTOCEvent(config, name));
    if (type == null) {
      pushEvent(new ConfigChildElementEvent(config, name, type, description, required));
      config.add(name);
      pushEvent(new ConfigChildElementEndEvent(config, name, type));
    } else {
      pushEvent(new ConfigChildAttributeEvent(config, name, type, description, required));
      config.add(name);
    }
  }

  @Override
  public void done() throws IOException {
    Event last = null;
    
    try {
      ConfigTOCEvent.setWriteFeatureToc(false);
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
        if (e.matches(last)) {
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
    } finally {
      ConfigTOCEvent.setWriteFeatureToc(true);
    }
  }

  @Override
  public void visitTitle(String displayName) throws IOException {
    pushEvent(new TitleEvent(displayName + " (" + dita.getId() + ")"));
  }

  @Override
  public void endChildElement() throws IOException {
    config.pop();
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

  private void pushEvent(Event event) {
    events.add(event);
  }
}
