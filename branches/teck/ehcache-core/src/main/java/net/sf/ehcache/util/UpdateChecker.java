/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check for new Ehcache updates and alert users if an update is available
 *
 * @author Hung Huynh
 */
public class UpdateChecker extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class.getName());
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final int CONNECT_TIMEOUT = 3000;
    private static final String UNKNOWN = "UNKNOWN";
    private static final String UPDATE_CHECK_URL = "http://www.terracotta.org/kit/reflector?pageID=update.properties";
    private static final long START_TIME = System.currentTimeMillis();

    private final Map<String, String> params = new HashMap<String, String>();

    /**
     * Construct update checker object
     */
    public UpdateChecker() {
        prepareParams();
    }

    private void prepareParams() {
        ProductInfo productInfo = new ProductInfo();
        String productName = productInfo.getName().toLowerCase();
        String kitId = "ehcache.default";
        if (productName.contains("ehcache")) {
            kitId = "ehcache.default";
        } else if (productName.contains("bigmemory")) {
            kitId = "bigmemory.default";
        } else {
            throw new AssertionError("Unknown product name: " + productName);
        }
        putUrlSafe("kidID", kitId);
        putUrlSafe("id", Integer.toString(getClientId()));
        putUrlSafe("os-name", getProperty("os.name"));
        putUrlSafe("jvm-name", getProperty("java.vm.name"));
        putUrlSafe("jvm-version", getProperty("java.version"));
        putUrlSafe("platform", getProperty("os.arch"));
        putUrlSafe("tc-version", productInfo.getVersion());
        putUrlSafe("tc-product", productInfo.getName() + " " + productInfo.getVersion());
        putUrlSafe("source", productInfo.getName());
        putUrlSafe("uptime-secs", Long.toString(getUptimeInSeconds()));
        putUrlSafe("patch", productInfo.getPatchLevel());
    }

    /**
     * Add param to map, value is url-encoded
     * @param key
     * @param value
     */
    protected void putUrlSafe(String key, String value) {
        params.put(key, urlEncode(value));
    }

    /**
     * Run the update check
     */
    @Override
    public void run() {
        checkForUpdate();
    }

    /**
     * This method ensures that there will be no exception thrown.
     */
    public void checkForUpdate() {
        try {
            if (!Boolean.getBoolean("net.sf.ehcache.skipUpdateCheck")) {
                doCheck();
            }
        } catch (Throwable t) {
            LOG.debug("Update check failed: ", t);
        }
    }

    private void doCheck() throws IOException {
        updateParams();
        URL updateUrl = buildUpdateCheckUrl();
        if (Boolean.getBoolean("net.sf.ehcache.debug.updatecheck")) {
            LOG.info("Update check url: {}", updateUrl);
        }
        Properties updateProps = getUpdateProperties(updateUrl);
        String currentVersion = new ProductInfo().getVersion();
        String propVal = updateProps.getProperty("general.notice");
        if (notBlank(propVal)) {
            LOG.info(propVal);
        }
        propVal = updateProps.getProperty(currentVersion + ".notices");
        if (notBlank(propVal)) {
            LOG.info(propVal);
        }
        propVal = updateProps.getProperty(currentVersion + ".updates");
        if (notBlank(propVal)) {
            StringBuilder sb = new StringBuilder();
            String[] newVersions = propVal.split(",");
            for (int i = 0; i < newVersions.length; i++) {
                String newVersion = newVersions[i].trim();
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(newVersion);
                propVal = updateProps.getProperty(newVersion + ".release-notes");
                if (notBlank(propVal)) {
                    sb.append(" [");
                    sb.append(propVal);
                    sb.append("]");
                }
            }
            if (sb.length() > 0) {
                LOG.info("New update(s) found: " + sb.toString() + ". Please check http://ehcache.org for the latest version.");
            }
        }
    }

    private Properties getUpdateProperties(URL updateUrl) throws IOException {
        URLConnection connection = updateUrl.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        InputStream in = connection.getInputStream();
        try {
            Properties props = new Properties();
            props.load(connection.getInputStream());
            return props;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private URL buildUpdateCheckUrl() throws MalformedURLException, UnsupportedEncodingException {
        String url = System.getProperty("ehcache.update-check.url", UPDATE_CHECK_URL);
        String connector = url.indexOf('?') > 0 ? "&" : "?";
        return new URL(url + connector + buildParamsString());
    }

    /**
     * hook point to update any params before update check
     */
    protected void updateParams() {
        putUrlSafe("uptime-secs", Long.toString(getUptimeInSeconds()));
    }

    private String buildParamsString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    private long getUptimeInSeconds() {
        long uptime = System.currentTimeMillis() - START_TIME;
        return uptime > 0 ? (uptime / MILLIS_PER_SECOND) : 0;
    }

    private int getClientId() {
        try {
            return InetAddress.getLocalHost().hashCode();
        } catch (Throwable t) {
            return 0;
        }
    }

    private String urlEncode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    private String getProperty(String prop) {
        return System.getProperty(prop, UNKNOWN);
    }

    private boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }
}
