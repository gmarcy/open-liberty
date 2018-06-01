package com.ibm.ws.wlp.docgen.dita.events;


public class TitleEvent extends TextNodeEvent implements Event {

  public TitleEvent(String displayName) {
    super("title", displayName);
  }

}
