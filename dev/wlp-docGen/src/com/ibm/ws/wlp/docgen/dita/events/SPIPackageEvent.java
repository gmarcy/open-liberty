package com.ibm.ws.wlp.docgen.dita.events;

public class SPIPackageEvent extends PackageEvent {

  public SPIPackageEvent(String packageName, String javadocLink) {
    super("feat_spi", packageName, javadocLink);
  }
}
