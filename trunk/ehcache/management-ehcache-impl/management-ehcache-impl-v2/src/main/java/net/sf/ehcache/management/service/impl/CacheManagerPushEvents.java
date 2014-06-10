package net.sf.ehcache.management.service.impl;

import java.util.Observable;
import java.util.Observer;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitFeatureTypeInternal;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.feature.ManagementInternalFeature;
import org.terracotta.toolkit.internal.feature.ToolkitManagementEvent;

public class CacheManagerPushEvents implements Observer {

  @Override
  public void update(Observable o, Object arg) {
    
//    Toolkit toolkit = TerracottaToolkitBuilder.toolkit;
    //TODO : wtf ?
        Toolkit toolkit = null;
        try {
          toolkit = ToolkitFactory.createToolkit("toolkit:terracotta://localhost:9510");
        } catch (Exception e) {
          e.printStackTrace();
        }
    ToolkitInternal toolkitInternal = (ToolkitInternal)toolkit;
    if (toolkitInternal != null) {
      ManagementInternalFeature feature = toolkitInternal.getFeature(ToolkitFeatureTypeInternal.MANAGEMENT);
      feature.sendEvent((ToolkitManagementEvent) arg);
    }
  }

}
