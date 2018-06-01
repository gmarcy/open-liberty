/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.asciidoc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public abstract class AdocGeneratorTask extends Task {

  protected File file;
  protected File dir;
  private Properties rb = new Properties();
  protected String nlsPrefix;
  protected String lang;
  protected Properties configManifest = new Properties();

  public void setSchema(File f) {
    file = f;
  }
  public void setDir(File d) {
    dir = d;
  }
  public void setNLS(String file) {
    nlsPrefix = file;
  }
  public void setLang(String lang) {
    this.lang = lang;
  }

  public void execute() {
    validate();
    doExecute();
    try {
      configManifest.store(new FileWriter(new File(dir, "manifest.properties")), null);
    } catch (IOException e) {
      throw new BuildException("Failed to write manifest", e);
    }
  }

  protected abstract void doExecute();

  private void validate() {
    if (file == null) {
      throw new BuildException("file is a required attribute");
    }

    if (dir == null) {
      throw new BuildException("dir is a required attribute");
    }

    if (nlsPrefix == null) {
      throw new BuildException("nls is a required attribute. It should point to a properties file prefix.");
    }

    if (lang == null) {
      throw new BuildException("lang is a required attribute.");
    }

    if (!!!file.exists()) {
      throw new BuildException("The file " + file
          + " does not exist");
    }

    String langDirName = lang.replaceAll("_", "/");

    // The language code uses _ for language variants, like Brazilian Portuguese
    // or the difference between traditional and simplified chinese, but the KC
    // needs them to be in child directories, so we just replace any instances.
    dir = new File(dir.getAbsolutePath().replaceAll(lang, langDirName));

    if (!!!dir.exists() && !!!dir.mkdirs()) {
      throw new BuildException("The output directory " + dir
          + " does not exist");
    }

    if (!!!dir.isDirectory()) {
      throw new BuildException("The output directory " + dir
          + " is not a directory");
    }

    String fileName = nlsPrefix + ((lang.length() == 0) ? "" : '_' + lang) + ".nlsprops";
    try {
      String basedir = getProject().getProperty("basedir");
      rb.load(new FileReader(new File(basedir, fileName)));
    } catch (IOException e) {
      throw new BuildException(e.getMessage(), e);
    }
  }
  protected String getNLS(String key, Object... inserts) {
    String result = rb.getProperty(key, key);
    if (inserts != null && inserts.length > 0) {
      result = MessageFormat.format(result, inserts);
    }
    return result;
  }

}
