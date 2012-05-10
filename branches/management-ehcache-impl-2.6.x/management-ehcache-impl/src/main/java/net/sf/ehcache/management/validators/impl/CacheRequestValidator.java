package net.sf.ehcache.management.validators.impl;

import org.terracotta.management.validators.RequestValidator;
import net.sf.ehcache.management.validators.EhcacheValidationUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Named("embeddedCacheReqValidator")
public class CacheRequestValidator implements RequestValidator {
  private final CacheManagerRequestValidator cacheManagerReqValidator;

  @Inject
  public CacheRequestValidator(CacheManagerRequestValidator cacheManagerReqValidator) {
    this.cacheManagerReqValidator = cacheManagerReqValidator;
  }

  @Override
  public void validateSafe(UriInfo info) {
    cacheManagerReqValidator.validateSafe(info);
  }

  @Override
  public void validate(UriInfo info) {
    cacheManagerReqValidator.validate(info);

    EhcacheValidationUtils.validateUnsafeCacheRequest(info);
  }
}
