package com.ibm.ws.wlp.docgen.dita.events;

public class ThirdPartyAPIPackageEvent extends PackageEvent {

  public ThirdPartyAPIPackageEvent(String packageName, String javadocLink) {
    super("feat_thirdpartyapi", packageName, javadocLink);
  }

}
