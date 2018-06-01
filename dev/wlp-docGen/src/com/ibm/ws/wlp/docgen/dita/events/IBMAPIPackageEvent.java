package com.ibm.ws.wlp.docgen.dita.events;

public class IBMAPIPackageEvent extends PackageEvent {

  public IBMAPIPackageEvent(String packageName, String javadocLink) {
    super("feat_ibmapi", packageName, javadocLink);
  }
}
