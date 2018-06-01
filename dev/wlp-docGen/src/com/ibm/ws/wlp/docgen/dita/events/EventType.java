package com.ibm.ws.wlp.docgen.dita.events;

import java.util.Arrays;
import java.util.List;

/** 
 * This enum defines the order events get processed and thus where they appear in the dita 
 * with respect to each other. Note Title and Description MUST be the first two, in theory
 * reordering the others will move their location in the dita.
 */
public enum EventType {
  TITLE(TitleEvent.class),
  DESCRIPTION(DescriptionEvent.class),
  FEATURE_ENABLEMENT(FeatureEnablementEvent.class),
  JAVA_VERSION(JavaVersionEvent.class),
  SYMBOLIC_NAME(SymbolicNameEvent.class),
  SUPERCEEDED(SuperceededEvent.class),
  ENABLES(EnablesEvent.class),
  ENABLED_BY(EnabledByEvent.class),
  SPEC_API_PACKAGE(SpecAPIPackageEvent.class),
  IBM_API_PACKAGE(IBMAPIPackageEvent.class),
  THIRD_PARTY_API_PACKAGE(ThirdPartyAPIPackageEvent.class),
  SPI_PACKAGE(SPIPackageEvent.class),
  CONFIG_TOC(ConfigTOCEvent.class),
  CONFIG(ConfigEvent.class);
  
  private List<Class<? extends Event>> handlerTypes;
  
  private EventType(Class<? extends Event> ... types) {
    handlerTypes = Arrays.asList(types);
  }
  
  public static EventType from(Class<? extends Event> class1) {
    for (EventType t : values()) {
      Class<?> classToCheck = class1;
      while (classToCheck != null && !!!t.matches(classToCheck)) {
        classToCheck = classToCheck.getSuperclass();
      }
      if (classToCheck != null && Event.class.isAssignableFrom(classToCheck)) {
        return t;
      }
    }
    return null;
  }

  private boolean matches(Class<?> class1) {
    return handlerTypes.contains(class1);
  }
}
