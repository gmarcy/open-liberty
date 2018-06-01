package com.ibm.ws.wlp.docgen.dita.events;

public class SpecAPIPackageEvent extends PackageEvent {

  public SpecAPIPackageEvent(String packageName, String javadocLink) {
    super("feat_specapi", packageName, javadocLink);
  }
}
