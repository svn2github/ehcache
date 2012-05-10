package net.sf.ehcache.management.validators.impl;

import net.sf.ehcache.management.validators.EhcacheValidationUtils;
import org.terracotta.management.validators.RequestValidator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Named("embeddedCacheManagerReqValidator")
public class CacheManagerRequestValidator implements RequestValidator {
  private final AgentRequestValidator agentReqValidator;

  @Inject
  public CacheManagerRequestValidator(AgentRequestValidator agentReqValidator) {
    this.agentReqValidator = agentReqValidator;
  }

  @Override
  public void validateSafe(UriInfo info) {
    agentReqValidator.validateSafe(info);
  }

  @Override
  public void validate(UriInfo info) {
    agentReqValidator.validateSafe(info);

    EhcacheValidationUtils.validateUnsafeCacheManagerRequest(info);
  }
}
