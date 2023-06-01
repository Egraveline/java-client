package io.weaviate.integration.client.data;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.integration.client.WeaviateTestGenerics;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientDataMultiTenancyTest {
  private WeaviateClient client;
  private WeaviateTestGenerics testGenerics;

  private static final Map<String, List<String>> IDS_BY_CLASS = new HashMap<>();
  static {
    IDS_BY_CLASS.put("Pizza", Arrays.asList(
      WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID,
      WeaviateTestGenerics.PIZZA_FRUTTI_DI_MARE_ID,
      WeaviateTestGenerics.PIZZA_HAWAII_ID,
      WeaviateTestGenerics.PIZZA_DOENER_ID
    ));
    IDS_BY_CLASS.put("Soup", Arrays.asList(
      WeaviateTestGenerics.SOUP_CHICKENSOUP_ID,
      WeaviateTestGenerics.SOUP_BEAUTIFUL_ID
    ));
  }

  @ClassRule
  public static DockerComposeContainer<?> compose = new DockerComposeContainer<>(
    new File("src/test/resources/docker-compose-test.yaml")
  ).withExposedService("weaviate_1", 8080, Wait.forHttp("/v1/.well-known/ready").forStatusCode(200));

  @Before
  public void before() {
    String host = compose.getServiceHost("weaviate_1", 8080);
    Integer port = compose.getServicePort("weaviate_1", 8080);
    Config config = new Config("http", host + ":" + port);

    client = new WeaviateClient(config);
    testGenerics = new WeaviateTestGenerics();
  }

  @After
  public void after() {
    testGenerics.cleanupWeaviate(client);
  }


  @Test
  public void shouldAddObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);

    Map<String, Object> pizzaProps = new HashMap<>();
    pizzaProps.put("name", "Quattro Formaggi");
    pizzaProps.put("description", "Pizza quattro formaggi Italian: [ˈkwattro forˈmaddʒi] (four cheese pizza) is a variety of pizza in Italian cuisine that is topped with a combination of four kinds of cheese, usually melted together, with (rossa, red) or without (bianca, white) tomato sauce. It is popular worldwide, including in Italy,[1] and is one of the iconic items from pizzerias's menus.");
    pizzaProps.put("price", 1.4f);
    pizzaProps.put("bestBefore", "2022-01-02T03:04:05+01:00");

    Map<String, Object> soupProps = new HashMap<>();
    soupProps.put("name", "ChickenSoup");
    soupProps.put("description", "Used by humans when their inferior genetics are attacked by microscopic organisms.");
    soupProps.put("price", 2.0f);
    soupProps.put("bestBefore", "2022-05-06T07:08:09+05:00");

    Arrays.stream(tenants).forEach(tenant -> {
      pizzaProps.put("tenantName", tenant);

      Result<WeaviateObject> pizzaCreateStatus = client.data().creator()
        .withClassName("Pizza")
        .withID(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID)
        .withProperties(pizzaProps)
        .withTenantKey(tenant)
        .run();

      assertThat(pizzaCreateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult)
        .returns(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID, WeaviateObject::getId)
        .returns("Pizza", WeaviateObject::getClassName)
        .extracting(WeaviateObject::getProperties)
        .returns("Quattro Formaggi", p -> p.get("name"))
        .returns(1.4d, p -> p.get("price"))
        .returns("Pizza quattro formaggi Italian: [ˈkwattro forˈmaddʒi] (four cheese pizza) is a variety of pizza in Italian cuisine that is topped with a combination of four kinds of cheese, usually melted together, with (rossa, red) or without (bianca, white) tomato sauce. It is popular worldwide, including in Italy,[1] and is one of the iconic items from pizzerias's menus.", p -> p.get("description"))
        .returns("2022-01-02T03:04:05+01:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));

      soupProps.put("tenantName", tenant);

      Result<WeaviateObject> soupCreateStatus = client.data().creator()
        .withClassName("Soup")
        .withID(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID)
        .withProperties(soupProps)
        .withTenantKey(tenant)
        .run();

      assertThat(soupCreateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult)
        .returns(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID, WeaviateObject::getId)
        .returns("Soup", WeaviateObject::getClassName)
        .extracting(WeaviateObject::getProperties)
        .returns("ChickenSoup", p -> p.get("name"))
        .returns(2.0d, p -> p.get("price"))
        .returns("Used by humans when their inferior genetics are attacked by microscopic organisms.", p -> p.get("description"))
        .returns("2022-05-06T07:08:09+05:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));
    });
  }

  @Test
  public void shouldGetObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);
    testGenerics.createFoodDataForTenants(client, tenants);

    Arrays.stream(tenants).forEach(tenant ->
      IDS_BY_CLASS.forEach((className, classIds) -> {
        // TODO should fetch all for tenant?
//        Result<List<WeaviateObject>> getResult = client.data().objectsGetter()
//          .withTenantKey(tenant)
//          .withClassName(className)
//          .run();
//
//        assertThat(getResult).isNotNull()
//          .returns(false, Result::hasErrors);

        classIds.forEach(id -> {
          Result<List<WeaviateObject>> getOneResult = client.data().objectsGetter()
            .withTenantKey(tenant)
            .withClassName(className)
            .withID(id)
            .run();

          assertThat(getOneResult).isNotNull()
            .returns(false, Result::hasErrors)
            .extracting(Result::getResult).asList()
            .hasSize(1)
            .first()
            .extracting(o -> (WeaviateObject) o)
            .returns(id, WeaviateObject::getId)
            .returns(className, WeaviateObject::getClassName)
            .extracting(WeaviateObject::getProperties)
            .returns(tenant, p -> p.get("tenantName"));
        });
      })
    );
  }

  @Test
  public void shouldCheckObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);
    testGenerics.createFoodDataForTenants(client, tenants);

    Arrays.stream(tenants).forEach(tenant ->
        IDS_BY_CLASS.forEach((className, classIds) ->
          classIds.forEach(id -> {
            Result<Boolean> checkResult = client.data().checker()
              .withTenantKey(tenant)
              .withClassName(className)
              .withID(id)
              .run();

            assertThat(checkResult).isNotNull()
              .returns(false, Result::hasErrors)
              .returns(true, Result::getResult);
          })
        )
    );
  }

  @Test
  public void shouldDeleteObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);
    testGenerics.createFoodDataForTenants(client, tenants);

    Arrays.stream(tenants).forEach(tenant ->
      IDS_BY_CLASS.forEach((className, classIds) ->
        classIds.forEach(id -> {
          Result<Boolean> deleteStatus = client.data().deleter()
            .withTenantKey(tenant)
            .withClassName(className)
            .withID(id)
            .run();

          assertThat(deleteStatus).isNotNull()
            .returns(false, Result::hasErrors)
            .returns(true, Result::getResult);

          Result<List<WeaviateObject>> getOneResult = client.data().objectsGetter()
            .withTenantKey(tenant)
            .withClassName(className)
            .withID(id)
            .run();

          assertThat(getOneResult).isNotNull()
            .returns(false, Result::hasErrors)
            .extracting(Result::getResult).isNull();
        })
      )
    );
  }

  @Test
  public void shouldUpdateObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);
    testGenerics.createFoodDataForTenants(client, tenants);

    Map<String, Object> pizzaProps = new HashMap<>();
    pizzaProps.put("name", "Quattro Formaggi");
    pizzaProps.put("description", "updated Quattro Formaggi description");
    pizzaProps.put("price", 1000.1f);
    pizzaProps.put("bestBefore", "2022-01-02T03:04:05+01:00");

    Map<String, Object> soupProps = new HashMap<>();
    soupProps.put("name", "ChickenSoup");
    soupProps.put("description", "updated ChickenSoup description");
    soupProps.put("price", 2000.2f);
    soupProps.put("bestBefore", "2022-05-06T07:08:09+05:00");

    Arrays.stream(tenants).forEach(tenant -> {
      pizzaProps.put("tenantName", tenant);

      Result<Boolean> pizzaUpdateStatus = client.data().updater()
        .withClassName("Pizza")
        .withID(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID)
        .withProperties(pizzaProps)
        .withTenantKey(tenant)
        .run();

      assertThat(pizzaUpdateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .returns(true, Result::getResult);

      Result<List<WeaviateObject>> getPizzaResult = client.data().objectsGetter()
        .withTenantKey(tenant)
        .withClassName("Pizza")
        .withID(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID)
        .run();

      assertThat(getPizzaResult).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult).asList()
        .hasSize(1)
        .first()
        .extracting(o -> ((WeaviateObject)o).getProperties())
        .returns("Quattro Formaggi", p -> p.get("name"))
        .returns("updated Quattro Formaggi description", p -> p.get("description"))
        .returns(1000.1d, p -> p.get("price"))
        .returns("2022-01-02T03:04:05+01:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));

      soupProps.put("tenantName", tenant);

      Result<Boolean> soupUpdateStatus = client.data().updater()
        .withClassName("Soup")
        .withID(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID)
        .withProperties(soupProps)
        .withTenantKey(tenant)
        .run();

      assertThat(soupUpdateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .returns(true, Result::getResult);

      Result<List<WeaviateObject>> getSoupResult = client.data().objectsGetter()
        .withTenantKey(tenant)
        .withClassName("Soup")
        .withID(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID)
        .run();

      assertThat(getSoupResult).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult).asList()
        .hasSize(1)
        .first()
        .extracting(o -> ((WeaviateObject)o).getProperties())
        .returns("ChickenSoup", p -> p.get("name"))
        .returns("updated ChickenSoup description", p -> p.get("description"))
        .returns(2000.2d, p -> p.get("price"))
        .returns("2022-05-06T07:08:09+05:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));
    });
  }

  @Test
  public void shouldMergeObjects() {
    testGenerics.createFoodSchemaForTenants(client);
    String[] tenants = new String[]{"TenantNo1", "TenantNo2", "TenantNo3"};
    testGenerics.createTenants(client, tenants);
    testGenerics.createFoodDataForTenants(client, tenants);

    Map<String, Object> pizzaProps = new HashMap<>();
    pizzaProps.put("description", "updated Quattro Formaggi description");
    pizzaProps.put("price", 1000.1f);

    Map<String, Object> soupProps = new HashMap<>();
    soupProps.put("description", "updated ChickenSoup description");
    soupProps.put("price", 2000.2f);

    Arrays.stream(tenants).forEach(tenant -> {
      Result<Boolean> pizzaUpdateStatus = client.data().updater()
        .withClassName("Pizza")
        .withID(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID)
        .withProperties(pizzaProps)
        .withTenantKey(tenant)
        .withMerge()
        .run();

      assertThat(pizzaUpdateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .returns(true, Result::getResult);

      Result<List<WeaviateObject>> getPizzaResult = client.data().objectsGetter()
        .withTenantKey(tenant)
        .withClassName("Pizza")
        .withID(WeaviateTestGenerics.PIZZA_QUATTRO_FORMAGGI_ID)
        .run();

      assertThat(getPizzaResult).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult).asList()
        .hasSize(1)
        .first()
        .extracting(o -> ((WeaviateObject)o).getProperties())
        .returns("Quattro Formaggi", p -> p.get("name"))
        .returns("updated Quattro Formaggi description", p -> p.get("description"))
        .returns(1000.1d, p -> p.get("price"))
        .returns("2022-01-02T03:04:05+01:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));

      Result<Boolean> soupUpdateStatus = client.data().updater()
        .withClassName("Soup")
        .withID(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID)
        .withProperties(soupProps)
        .withTenantKey(tenant)
        .withMerge()
        .run();

      assertThat(soupUpdateStatus).isNotNull()
        .returns(false, Result::hasErrors)
        .returns(true, Result::getResult);

      Result<List<WeaviateObject>> getSoupResult = client.data().objectsGetter()
        .withTenantKey(tenant)
        .withClassName("Soup")
        .withID(WeaviateTestGenerics.SOUP_CHICKENSOUP_ID)
        .run();

      assertThat(getSoupResult).isNotNull()
        .returns(false, Result::hasErrors)
        .extracting(Result::getResult).asList()
        .hasSize(1)
        .first()
        .extracting(o -> ((WeaviateObject)o).getProperties())
        .returns("ChickenSoup", p -> p.get("name"))
        .returns("updated ChickenSoup description", p -> p.get("description"))
        .returns(2000.2d, p -> p.get("price"))
        .returns("2022-05-06T07:08:09+05:00", p -> p.get("bestBefore"))
        .returns(tenant, p -> p.get("tenantName"));
    });
  }
}

