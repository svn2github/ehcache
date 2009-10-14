/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.repository.StandardApplicationsRepository;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.StandardXMLFileConfigurationCreator;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.DSOContextImpl;
import com.terracottatech.config.Client;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;

public class StandaloneL1Boot implements Callable<ClassFileTransformer> {

  private final String[]         tims;
  private final Map<String, URL> virtualTimJars;
  private final String           embeddedTcConfig;
  private final boolean          isURLConfig;
  private final ClassLoader      appLevelTimLoader;

  public StandaloneL1Boot(String[] tims, Map<String, URL> virtualTimJars, String embeddedTcConfig, boolean isURLConfig,
                          ClassLoader appLevelTimLoader) {
    this.tims = tims;
    this.virtualTimJars = virtualTimJars;
    this.embeddedTcConfig = embeddedTcConfig;
    this.isURLConfig = isURLConfig;
    this.appLevelTimLoader = appLevelTimLoader;

  }

  private File createConfigFile() {
    String config = resolveConfig();

    File tmp;
    try {
      tmp = File.createTempFile("tc-config", ".xml");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    tmp.deleteOnExit();

    String fName = tmp.getAbsolutePath();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(fName, false);
      fos.write(config.getBytes());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeQuietly(fos);
    }

    return tmp;
  }

  private static void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private File buildTcConfigFile(String[] tims) throws Exception {
    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfig tcConfig = doc.addNewTcConfig();
    Client clients = tcConfig.addNewClients();
    Modules modules = clients.addNewModules();

    for (String tim : tims) {
      Module module = modules.addNewModule();
      module.setName(tim);
    }

    File tmpFile = File.createTempFile("tc-config", ".xml");
    tmpFile.deleteOnExit();
    writeFile(tmpFile, doc.toString().getBytes());

    return tmpFile;
  }

  private static void writeFile(File file, byte[] contents) throws IOException {
    FileOutputStream out = null;

    try {
      out = new FileOutputStream(file);
      out.write(contents);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  private L1TVSConfigurationSetupManager createConfigSetupManager(String[] tims) throws Exception {
    File tcConfigFile = buildTcConfigFile(tims);

    Option configFileOption = new Option("f", "config", true, "configuration file (optional)");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);
    configFileOption.setRequired(false);

    Options options = new Options();
    options.addOption(configFileOption);

    String[] newArgs = new String[2];
    newArgs[newArgs.length - 2] = "-f";
    newArgs[newArgs.length - 1] = tcConfigFile.getAbsolutePath();
    CommandLine cmdLine = new PosixParser().parse(options, newArgs);

    StandardTVSConfigurationSetupManagerFactory factory;
    factory = new StandardTVSConfigurationSetupManagerFactory(cmdLine, false,
                                                              new FatalIllegalConfigurationChangeHandler());

    // XXX: use JDK logger here?
    TCLogger logger = new NullTCLogger();
    L1TVSConfigurationSetupManager setupManager = factory.createL1TVSConfigurationSetupManager(logger);

    // okay to try to delete temp file now
    tcConfigFile.delete();

    return setupManager;
  }

  private String resolveConfig() {
    TcConfig embedded;
    try {
      embedded = resolveEmbedded();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfig newTcConfig = doc.addNewTcConfig();

    Modules modules = newTcConfig.addNewClients().addNewModules();
    for (String tim : tims) {
      Module module = modules.addNewModule();
      module.setName(tim);
    }

    Client clients = embedded.getClients();
    if (clients == null) {
      clients = embedded.addNewClients();
    }
    clients.setModules(modules);

    // XXX: when/if we refactor or add new major sections, this set of things that we "copy" might
    // be wrong or incomplete!
    if (embedded.getServers() != null) {
      newTcConfig.setServers(embedded.getServers());
    }
    if (embedded.getSystem() != null) {
      newTcConfig.setSystem(embedded.getSystem());
    }
    newTcConfig.setClients(clients);
    if (embedded.getTcProperties() != null) {
      newTcConfig.setTcProperties(embedded.getTcProperties());
    }

    return doc.toString();
  }

  private TcConfig resolveEmbedded() throws ConfigurationSetupException, XmlException {
    final String configText;

    if (isURLConfig) {
      // XXX: use a non-null TClogger here?
      StandardXMLFileConfigurationCreator creator = new StandardXMLFileConfigurationCreator(
                                                                                            new NullTCLogger(),
                                                                                            embeddedTcConfig,
                                                                                            new File("."),
                                                                                            new TerracottaDomainConfigurationDocumentBeanFactory());
      StandardBeanRepository beanRepo = new StandardBeanRepository(Object.class);
      creator.createConfigurationIntoRepositories(beanRepo, beanRepo, beanRepo, beanRepo,
                                                  new StandardApplicationsRepository());
      configText = creator.rawConfigText();
    } else {
      configText = embeddedTcConfig;
    }

    return TcConfigDocument.Factory.parse(configText).getTcConfig();
  }

  public ClassFileTransformer call() throws Exception {
    File configFile = createConfigFile();

    DSOContext context = DSOContextImpl.createStandaloneContext(configFile.getAbsolutePath(), appLevelTimLoader,
                                                                virtualTimJars);

    // okay to try to delete temp file now
    configFile.delete();

    ManagerUtil.enableSingleton(context.getManager());
    return context;
  }
}
