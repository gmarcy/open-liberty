/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
 package com.ibm.ws.wlp.docgen.asciidoc;

import java.util.List;

public class Child implements Comparable {

    public String name;
    public String typeName;
    public String desc;
    public List<Attribute> attributes;
    public boolean required;
    public String breadrum;
    public String id;

    public int compareTo(Object other) {
      return name.compareTo(((Child)other).name);
    }
}
