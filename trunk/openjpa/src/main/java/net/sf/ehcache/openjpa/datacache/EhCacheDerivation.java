package net.sf.ehcache.openjpa.datacache;

import java.util.Map;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Introduces a DataCache specialized for EhCache.
 * This derivation is activated by setting <code>openjpa.DataCacheManager</code>
 * configuration property to <code>"ehcache"</code>.
 * <BR>
 * This derivation also forces that <code>openjpa.DataCache</code> and 
 * <code>openjpa.RemoteCommitProvider</code> property is <em>not</em> 
 * specified or specified as <code>"ehcache"</code> and <code>"none"</code>
 * respectively. 
 * 
 * 
 * @author Pinaki Poddar 
 * @author Craig Andrews
 *
 */
public class EhCacheDerivation extends AbstractProductDerivation {
    public static final String EHCACHE = "ehcache";
    public static final String NO_RCP    = "none";
    
    private static final Localizer _loc = 
        Localizer.forPackage(EhCacheDerivation.class);
    
    @Override
    public void validate() throws Exception {
        Class.forName("net.sf.ehcache.CacheManager");
    }

    
    public int getType() {
        return TYPE_FEATURE;
    }
    
    public boolean beforeConfigurationLoad(Configuration conf) {
        if (conf instanceof OpenJPAConfiguration) {
            OpenJPAConfigurationImpl oconf = (OpenJPAConfigurationImpl)conf;
            oconf.dataCacheManagerPlugin.setAlias(EHCACHE, 
                    EhCacheDataCacheManager.class.getName());
            oconf.dataCachePlugin.setAlias(EHCACHE, 
                    EhCacheDataCache.class.getName());
            oconf.queryCachePlugin.setAlias(EHCACHE, 
                    EhCacheQueryCache.class.getName());
            oconf.remoteProviderPlugin.setAlias("none", 
                    NoOpRemoteCommitProvider.class.getName());
        }
        return false;
    }
    
    public boolean beforeConfigurationConstruct(ConfigurationProvider cp) {
        Map props = cp.getProperties();
        Object dcm = Configurations.getProperty("DataCacheManager", props);
        if (dcm != null && isCompliant(dcm, EhCacheDataCacheManager.class)) {
            Object dc = Configurations.getProperty("DataCache", props);
            if (dc == null) {
                cp.addProperty("openjpa.DataCache", EHCACHE);
            } else if (!isCompliant(dc, EhCacheDataCache.class)) {
                warn("incompatible-configuration", "DataCache", dc, EHCACHE);
                cp.addProperty("openjpa.DataCache", EHCACHE);
            }
            Object rcp = Configurations.getProperty("RemoteCommitProvider", 
                    props);
            if (rcp == null) {
                cp.addProperty("openjpa.RemoteCommitProvider", 
                        NoOpRemoteCommitProvider.class.getName());
            } else if (!isCompliant(rcp, NoOpRemoteCommitProvider.class)) {
                warn("incompatible-configuration", "RemoteCommitProvider", 
                        rcp, NO_RCP);
                cp.addProperty("openjpa.RemoteCommitProvider", NO_RCP);
            }
        }
        return false;
    }
    
    boolean isCompliant(Object dcm, Class cls) {
        return dcm.equals(EHCACHE) || 
               dcm.equals(cls.getName()) || 
               cls.isAssignableFrom(dcm.getClass());
    }

    
    void warn(String key, Object...args) {
        Localizer.Message message = _loc.get(key, args);
        System.err.println("*** WARN: " + message);
    }

}
