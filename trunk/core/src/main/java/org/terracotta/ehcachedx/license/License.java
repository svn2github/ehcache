/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
package org.terracotta.ehcachedx.license;

import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 
 * @author hhuynh
 *
 */
public interface License {
  /**
   * @return product name
   */
  String product();

  /**
   * @return edition
   */
  String edition();

  /**
   * @return license type
   */
  String type();

  /**
   * @return customer name
   */
  String licensee();

  /**
   * @return license number
   */
  String number();

  /**
   * @return list of enterprise capabilities the license allows
   */
  List<String> capabilities();

  /**
   * @return list of allowed features for Ehcache
   */
  List<String> ehcacheFeatures();

  /**
   * @return license expiration date if exists, null otherwise
   */
  Date expirationDate();

  /**
   * @return signature that was used to sign the license
   */
  String signature();

  /**
   * @param key
   * @return value from license
   */
  String getProperty(String key);
  
  /**
   * @param key
   * @return required value from license, throws LicenseException if not found
   */
  String getRequiredProperty(String key);
  
  /**
   * @return all properties in the license
   */
  Properties getProperties();

  /**
   * @return true if Ehcache enterprise enabled
   */
  boolean isEnterpriseEhcacheEnabled();

  /**
   * @param feature
   * @return true if Ehcache feature is enabled
   */
  boolean isEnterpriseFeatureEnabled(String feature);
}
