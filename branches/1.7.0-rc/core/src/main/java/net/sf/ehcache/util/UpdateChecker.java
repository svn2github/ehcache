/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Check for new Ehcache updates and alert users if an update is available
 * 
 * @author Hung Huynh
 */
public class UpdateChecker extends TimerTask {
    private static final Logger LOG = Logger.getLogger(UpdateChecker.class.getName());
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final String NOT_AVAILABLE = "UNKNOWN";
    private static final String UPDATE_CHECK_URL = "http://www.terracotta.org/kit/reflector?kitID=ehcache.default&pageID=update.properties";
    private static final long START_TIME = System.currentTimeMillis();

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
            LOG.log(Level.WARNING, "Update check failed: " + t.toString());
        }
    }

    private void doCheck() throws IOException {
        LOG.log(Level.INFO, "Checking for update...");
        URL updateUrl = buildUpdateCheckUrl();
        Properties updateProps = getUpdateProperties(updateUrl);
        String currentVersion = new ProductInfo().getVersion();
        String propVal = updateProps.getProperty("general.notice");
        if (notBlank(propVal)) {
            LOG.log(Level.INFO, propVal);
        }
        propVal = updateProps.getProperty(currentVersion + ".notice");
        if (notBlank(propVal)) {
            LOG.log(Level.INFO, propVal);
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
                propVal = updateProps
                        .getProperty(newVersion + ".release-notes");
                if (notBlank(propVal)) {
                    sb.append(" [");
                    sb.append(propVal);
                    sb.append("]");
                }
            }
            if (sb.length() > 0) {
                LOG.log(Level.INFO, "New update(s) found: " + sb.toString());
            }
        } else {
            LOG.log(Level.INFO, "No update found.");
        }
    }

    private Properties getUpdateProperties(URL updateUrl) throws IOException {
        URLConnection connection = updateUrl.openConnection();
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

    private URL buildUpdateCheckUrl() throws MalformedURLException,
            UnsupportedEncodingException {
        String url = System.getProperty("ehcache.update-check.url",
                UPDATE_CHECK_URL);
        String connector = url.indexOf('?') > 0 ? "&" : "?";
        return new URL(url + connector + buildParamsString());
    }

    private String buildParamsString() throws UnsupportedEncodingException {
        ProductInfo productInfo = new ProductInfo();
        StringBuilder sb = new StringBuilder();
        sb.append("id=");
        sb.append(getClientId());
        sb.append("&os-name=");
        sb.append(urlEncode(getProperty("os.name")));
        sb.append("&jvm-name=");
        sb.append(urlEncode(getProperty("java.vm.name")));
        sb.append("&jvm-version=");
        sb.append(urlEncode(getProperty("java.version")));
        sb.append("&platform=");
        sb.append(urlEncode(getProperty("os.arch")));
        sb.append("&tc-version=");
        sb.append(NOT_AVAILABLE);
        sb.append("&tc-product=");
        sb.append(urlEncode(productInfo.getName() + " "
                + productInfo.getVersion()));
        sb.append("&source=");
        sb.append(urlEncode(productInfo.getName()));
        sb.append("&uptime-secs=");
        sb.append(getUptimeInSeconds());
        sb.append("&patch=");
        sb.append(urlEncode(productInfo.getPatchLevel()));
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

    private String urlEncode(String param) throws UnsupportedEncodingException {
        return URLEncoder.encode(param, "UTF-8");
    }

    private String getProperty(String prop) {
        return System.getProperty(prop, NOT_AVAILABLE);
    }

    private boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }
}
