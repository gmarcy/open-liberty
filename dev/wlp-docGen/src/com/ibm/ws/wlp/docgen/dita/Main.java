package com.ibm.ws.wlp.docgen.dita;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ibm.ws.wlp.docgen.dita.events.ConfigAttributeEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigElementEndEvent;
import com.ibm.ws.wlp.docgen.dita.events.ConfigElementEvent;
import com.ibm.ws.wlp.docgen.dita.events.Event;
import com.ibm.ws.wlp.docgen.dita.events.EventType;

public class Main {
  public static void main(String[] args) {
    List<Event> events = new ArrayList<Event>();
    
    events.add(new ConfigElementEvent("quickStartSecurity", ""));
    events.add(new ConfigAttributeEvent(Arrays.asList("quickStartSecurity"), createAttribute("aaa")));
    events.add(new ConfigElementEndEvent("quickStartSecurity"));
    events.add(new ConfigElementEvent("abc", ""));
    events.add(new ConfigAttributeEvent(Arrays.asList("quickStartSecurity"), createAttribute("bbb")));
    events.add(new ConfigElementEndEvent("abc"));
    events.add(new ConfigElementEvent("zzz", ""));
    events.add(new ConfigAttributeEvent(Arrays.asList("quickStartSecurity"), createAttribute("ccc")));
    events.add(new ConfigElementEndEvent("zzz"));
    
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
    
    System.out.println(events);
  }

  private static Attribute createAttribute(String string) {
    Attribute a = new Attribute();
    a.name = string;
    return a;
  }
}
