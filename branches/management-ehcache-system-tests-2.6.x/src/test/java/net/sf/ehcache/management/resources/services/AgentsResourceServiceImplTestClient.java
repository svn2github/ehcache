package net.sf.ehcache.management.resources.services;

import net.sf.ehcache.tests.AbstractEhcacheManagementClientBase;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author Ludovic Orban
 */
public class AgentsResourceServiceImplTestClient extends AbstractEhcacheManagementClientBase {

  public AgentsResourceServiceImplTestClient(String[] args) {
    super(args);
 }

  public static void main(String[] args) {
    new AgentsResourceServiceImplTestClient(args).run();
  }

  @Test
  public void testGetAgents() {
    given()
        .expect()
          .statusCode(400)
        .when()
          .get("/agents;ids=some-unknown-id");

    given()
        .expect()
          .statusCode(200)
          .body("get(0).agentId", equalTo("embedded"))
        .when()
          .get("/agents;ids=embedded");

    given()
        .expect()
          .statusCode(200)
          .body("get(0).agentId", equalTo("embedded"))
        .when()
          .get("/agents");
  }

  @Test
  public void testGetAgentIds() {
    given()
        .expect()
          .statusCode(200)
          .body("get(0).agentId", equalTo("embedded"))
        .when()
          .get("/agents/info");
  }

}
