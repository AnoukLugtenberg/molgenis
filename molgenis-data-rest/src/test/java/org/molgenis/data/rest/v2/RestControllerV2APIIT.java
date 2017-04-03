package org.molgenis.data.rest.v2;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.molgenis.data.rest.RestControllerIT.Permission.*;
import static org.molgenis.data.rest.convert.RestTestUtils.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.collections.Maps.newHashMap;

/**
 * Tests each endpoint of the V2 Rest Api through http calls
 */
public class RestControllerV2APIIT
{
	private static final Logger LOG = getLogger(RestControllerV2APIIT.class);

	private static final String REST_TEST_USER = "api_v2_test_user";
	private static final String REST_TEST_USER_PASSWORD = "api_v2_test_user_password";
	private static final String V1_TEST_FILE = "/RestControllerV1_TestEMX.xlsx";
	private static final String V2_DELETE_TEST_FILE = "/RestControllerV2_DeleteEMX.xlsx";
	private static final String V2_COPY_TEST_FILE = "/RestControllerV2_CopyEMX.xlsx";
	private static final String API_V2 = "api/v2/";

	private static final String PACKAGE_PERMISSION_ID = "package_permission_ID";
	private static final String ENTITY_TYPE_PERMISSION_ID = "entityType_permission_ID";
	private static final String ATTRIBUTE_PERMISSION_ID = "attribute_permission_ID";
	private static final String FILE_META_PERMISSION_ID = "file_meta_permission_ID";
	private static final String OWNED_PERMISSION_ID = "owned_permission_ID";

	private static final String TYPE_TEST_PERMISSION_ID = "typeTest_permission_ID";
	private static final String TYPE_TEST_REF_PERMISSION_ID = "typeTestRef_permission_ID";
	private static final String LOCATION_PERMISSION_ID = "location_permission_ID";
	private static final String PERSONS_PERMISSION_ID = "persons_permission_ID";

	private static final String API_TEST_1_PERMISSION_ID = "api_test_1_permission_ID";
	private static final String API_TEST_2_PERMISSION_ID = "api_test_2_permission_ID";

	private static final String API_COPY_PERMISSION_ID = "api_copy_permission_ID";

	private String testUserToken;
	private String adminToken;
	private String testUserId;

	@BeforeClass
	public void beforeClass()
	{
		LOG.info("Read environment variables");
		String envHost = System.getProperty("REST_TEST_HOST");
		RestAssured.baseURI = Strings.isEmpty(envHost) ? DEFAULT_HOST : envHost;
		LOG.info("baseURI: " + baseURI);

		String envAdminName = System.getProperty("REST_TEST_ADMIN_NAME");
		String adminUserName = Strings.isEmpty(envAdminName) ? DEFAULT_ADMIN_NAME : envAdminName;
		LOG.info("adminUserName: " + adminUserName);

		String envAdminPW = System.getProperty("REST_TEST_ADMIN_PW");
		String adminPassword = Strings.isEmpty(envHost) ? DEFAULT_ADMIN_PW : envAdminPW;
		LOG.info("adminPassword: " + adminPassword);

		adminToken = login(adminUserName, adminPassword);

		LOG.info("Importing Test data");
		uploadEMX(adminToken, V1_TEST_FILE);
		uploadEMX(adminToken, V2_DELETE_TEST_FILE);
		uploadEMX(adminToken, V2_COPY_TEST_FILE);
		LOG.info("Importing Done");

		createUser(adminToken, REST_TEST_USER, REST_TEST_USER_PASSWORD);

		testUserId = getUserId(adminToken, REST_TEST_USER);
		LOG.info("testUserId: " + testUserId);

		grantSystemRights(adminToken, PACKAGE_PERMISSION_ID, testUserId, "sys_md_Package", WRITE);
		grantSystemRights(adminToken, ENTITY_TYPE_PERMISSION_ID, testUserId, "sys_md_EntityType", WRITE);
		grantSystemRights(adminToken, ATTRIBUTE_PERMISSION_ID, testUserId, "sys_md_Attribute", WRITE);

		grantSystemRights(adminToken, FILE_META_PERMISSION_ID, testUserId, "sys_FileMeta", WRITE);
		grantSystemRights(adminToken, OWNED_PERMISSION_ID, testUserId, "sys_sec_Owned", READ);

		grantRights(adminToken, TYPE_TEST_PERMISSION_ID, testUserId, "TypeTest", WRITE);
		grantRights(adminToken, TYPE_TEST_REF_PERMISSION_ID, testUserId, "TypeTestRef", WRITE);
		grantRights(adminToken, LOCATION_PERMISSION_ID, testUserId, "Location", WRITE);
		grantRights(adminToken, PERSONS_PERMISSION_ID, testUserId, "Person", WRITE);

		grantRights(adminToken, API_TEST_1_PERMISSION_ID, testUserId, "v2APITest1", WRITEMETA);
		grantRights(adminToken, API_TEST_2_PERMISSION_ID, testUserId, "v2APITest2", WRITEMETA);

		grantRights(adminToken, API_COPY_PERMISSION_ID, testUserId, "APICopyTest", WRITEMETA);

		testUserToken = login(REST_TEST_USER, REST_TEST_USER_PASSWORD);
	}

	//	@Autowired
	//	@RequestMapping(value = "/version", method = GET)
	//	@ResponseBody
	//	public Map<String, String> getVersion(@Value("${molgenis.version:@null}") String molgenisVersion,
	//			@Value("${molgenis.build.date:@null}") String molgenisBuildDate)
	//	@Test
	public void testGetVersion()
	{

	}

	@Test
	public void testRetrieveEntity()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.get(API_V2 + "it_emx_datatypes_TypeTestRef/ref1").then().log().all();
		validateRetrieveEntityWithoutAttributeFilter(response);
	}

	@Test
	public void testRetrieveEntityPost()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.post(API_V2 + "it_emx_datatypes_TypeTestRef/ref1?_method=GET").then().log().all();
		validateRetrieveEntityWithoutAttributeFilter(response);
	}

	@Test
	public void testRetrieveEntityWithAttributeFilter()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.param("attrs", newArrayList("label")).get(API_V2 + "it_emx_datatypes_TypeTestRef/ref1").then().log()
				.all();
		validateRetrieveEntityWithAttributeFilter(response);
	}

	@Test
	public void testRetrieveEntityWithAttributeFilterPost()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.param("attrs", newArrayList("label")).post(API_V2 + "it_emx_datatypes_TypeTestRef/ref1?_method=GET")
				.then().log().all();
		validateRetrieveEntityWithAttributeFilter(response);
	}

	@Test
	public void testDeleteEntity()
	{
		given().log().all().header(X_MOLGENIS_TOKEN, testUserToken).delete(API_V2 + "base_v2APITest1/ref1").then().log()
				.all().statusCode(NO_CONTENT);

		given().log().all().header(X_MOLGENIS_TOKEN, testUserToken).get(API_V2 + "base_v2APITest1").then().log().all()
				.body("total", equalTo(4), "items[0].value", equalTo("ref2"), "items[1].value", equalTo("ref3"),
						"items[2].value", equalTo("ref4"), "items[3].value", equalTo("ref5"));
	}

	@Test
	public void testDeleteEntityCollection()
	{
		Map<String, List<String>> requestBody = newHashMap();
		requestBody.put("entityIds", newArrayList("ref1", "ref2", "ref3", "ref4"));
		given().log().all().header(X_MOLGENIS_TOKEN, testUserToken).contentType(APPLICATION_JSON).body(requestBody)
				.delete(API_V2 + "base_v2APITest2").then().log().all().statusCode(NO_CONTENT);

		given().log().all().header(X_MOLGENIS_TOKEN, testUserToken).get(API_V2 + "base_v2APITest2").then().log().all()
				.body("total", equalTo(1), "items[0].value", equalTo("ref5"));
	}

	@Test
	public void testRetrieveEntityCollection()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.get(API_V2 + "it_emx_datatypes_TypeTestRef").then().log().all();
		validateRetrieveEntityCollection(response);
	}

	@Test
	public void testRetrieveEntityCollectionPost()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.post(API_V2 + "it_emx_datatypes_TypeTestRef?_method=GET").then().log().all();
		validateRetrieveEntityCollection(response);
	}

	@Test
	public void testRetrieveEntityAttributeMeta()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.get(API_V2 + "it_emx_datatypes_TypeTestRef/meta/value").then().log().all();
		validateRetrieveEntityAttributeMeta(response);
	}

	@Test
	public void testRetrieveEntityAttributeMetaPost()
	{
		ValidatableResponse response = given().log().all().header(X_MOLGENIS_TOKEN, testUserToken)
				.post(API_V2 + "it_emx_datatypes_TypeTestRef/meta/value?_method=GET").then().log().all();
		validateRetrieveEntityAttributeMeta(response);
	}

	//	@Transactional
	//	@RequestMapping(value = "/{entityName}", method = POST, produces = APPLICATION_JSON_VALUE)
	//	@ResponseBody
	//	public EntityCollectionBatchCreateResponseBodyV2 createEntities(@PathVariable("entityName") String entityName,
	//			@RequestBody @Valid EntityCollectionBatchRequestV2 request, HttpServletResponse response) throws Exception

	@Test
	public void testCreateEntities()
	{
		JSONObject jsonObject = new JSONObject();
		JSONArray entities = new JSONArray();

		JSONObject entity1 = new JSONObject();
		entity1.put("value", "ref55");
		entity1.put("label", "label55");
		entities.add(entity1);

		JSONObject entity2 = new JSONObject();
		entity2.put("value", "ref57");
		entity2.put("label", "label57");
		entities.add(entity2);

		jsonObject.put("entities", entities);

		given().log().all().body(jsonObject.toJSONString()).contentType(APPLICATION_JSON)
				.header(X_MOLGENIS_TOKEN, testUserToken).post(API_V2 + "it_emx_datatypes_TypeTest").then().log().all()
				.statusCode(CREATED)
				.body("location", equalTo("/api/v2/it_emx_datatypes_TypeTestv2?q=id=in=(\"55\",\"57\")"),
						"resources[0].href", equalTo("/api/v2/it_emx_datatypes_TypeTest/55"), "resources[1].href",
						equalTo("/api/v2/it_emx_datatypes_TypeTest/57"));
	}

	@Test
	public void testCopyEntity()
	{
		Map<String, String> request = newHashMap();
		request.put("newEntityName", "CopiedEntity");

		given().log().all().contentType(APPLICATION_JSON).body(request).header(X_MOLGENIS_TOKEN, testUserToken)
				.post(API_V2 + "copy/base_APICopyTest").then().log().all();

		given().log().all().header(X_MOLGENIS_TOKEN, testUserToken).get(API_V2 + "base_CopiedEntity").then().log().all()
				.body("href", equalTo("/api/v2/base_CopiedEntity"), "items[0].label", equalTo("Copied!"));
	}

	@Test
	public void testUpdateEntities()
	{
		Map<String, Object> request = newHashMap();
		request.put("id", "ref1");
		request.put("xstring", "This is an updated entity!");

		given().log().all().contentType(APPLICATION_JSON).body(request).header(X_MOLGENIS_TOKEN, testUserToken)
				.put(API_V2 + "it_emx_datatypes_TypeTestRef").then().log().all().statusCode(OKE);
	}

	//	@RequestMapping(value = "/{entityName}/{attributeName}", method = PUT)
	//	@ResponseStatus(OK)
	//	public synchronized void updateAttribute(@PathVariable("entityName") String entityName,
	//			@PathVariable("attributeName") String attributeName,
	//			@RequestBody @Valid EntityCollectionBatchRequestV2 request, HttpServletResponse response) throws Exception

	@Test
	public void testUpdateAttribute()
	{
	}

	//	@RequestMapping(value = "/i18n", method = GET, produces = APPLICATION_JSON_VALUE)
	//	@ResponseBody
	//	public Map<String, String> getI18nStrings()

	@Test
	public void testGetI18nStrings()
	{
	}

	private void validateRetrieveEntityWithoutAttributeFilter(ValidatableResponse response)
	{
		response.statusCode(OKE);
		response.body("_meta.href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "_meta.hrefCollection",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "_meta.name", equalTo("it_emx_datatypes_TypeTestRef"),
				"_meta.label", equalTo("TypeTestRef"), "_meta.description",
				equalTo("MOLGENIS Data types test ref entity"), "_meta.attributes[0].href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/value"), "_meta.attributes[0].fieldType",
				equalTo("STRING"), "_meta.attributes[0].name", equalTo("value"), "_meta.attributes[0].label",
				equalTo("value label"), "_meta.attributes[0].description", equalTo("TypeTestRef value attribute"),
				"_meta.attributes[0].attributes", equalTo(newArrayList()), "_meta.attributes[0].maxLength",
				equalTo(255), "_meta.attributes[0].auto", equalTo(false), "_meta.attributes[0].nillable",
				equalTo(false), "_meta.attributes[0].readOnly", equalTo(true), "_meta.attributes[0].labelAttribute",
				equalTo(false), "_meta.attributes[0].unique", equalTo(true), "_meta.attributes[0].visible",
				equalTo(true), "_meta.attributes[0].lookupAttribute", equalTo(true),
				"_meta.attributes[0].isAggregatable", equalTo(false), "_meta.attributes[1].href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/label"), "_meta.attributes[1].fieldType",
				equalTo("STRING"), "_meta.attributes[1].name", equalTo("label"), "_meta.attributes[1].label",
				equalTo("label label"), "_meta.attributes[1].description", equalTo("TypeTestRef label attribute"),
				"_meta.attributes[1].attributes", equalTo(newArrayList()), "_meta.attributes[1].maxLength",
				equalTo(255), "_meta.attributes[1].auto", equalTo(false), "_meta.attributes[1].nillable",
				equalTo(false), "_meta.attributes[1].readOnly", equalTo(false), "_meta.attributes[1].labelAttribute",
				equalTo(true), "_meta.attributes[1].unique", equalTo(false), "_meta.attributes[1].visible",
				equalTo(true), "_meta.attributes[1].lookupAttribute", equalTo(true),
				"_meta.attributes[1].isAggregatable", equalTo(false), "_meta.labelAttribute", equalTo("label"),
				"_meta.idAttribute", equalTo("value"), "_meta.lookupAttributes",
				equalTo(newArrayList("value", "label")), "_meta.isAbstract", equalTo(false), "_meta.writable",
				equalTo(true), "_meta.languageCode", equalTo("en"), "_href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref1"), "value", equalTo("ref1"), "label",
				equalTo("label1"));
	}

	private void validateRetrieveEntityWithAttributeFilter(ValidatableResponse response)
	{
		response.statusCode(OKE);
		response.body("_meta.href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "_meta.hrefCollection",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "_meta.name", equalTo("it_emx_datatypes_TypeTestRef"),
				"_meta.label", equalTo("TypeTestRef"), "_meta.description",
				equalTo("MOLGENIS Data types test ref entity"), "_meta.attributes[0].href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/label"), "_meta.attributes[0].fieldType",
				equalTo("STRING"), "_meta.attributes[0].name", equalTo("label"), "_meta.attributes[0].label",
				equalTo("label label"), "_meta.attributes[0].description", equalTo("TypeTestRef label attribute"),
				"_meta.attributes[0].attributes", equalTo(newArrayList()), "_meta.attributes[0].maxLength",
				equalTo(255), "_meta.attributes[0].auto", equalTo(false), "_meta.attributes[0].nillable",
				equalTo(false), "_meta.attributes[0].readOnly", equalTo(false), "_meta.attributes[0].labelAttribute",
				equalTo(true), "_meta.attributes[0].unique", equalTo(false), "_meta.attributes[0].visible",
				equalTo(true), "_meta.attributes[0].lookupAttribute", equalTo(true),
				"_meta.attributes[0].isAggregatable", equalTo(false), "_meta.labelAttribute", equalTo("label"),
				"_meta.idAttribute", equalTo("value"), "_meta.lookupAttributes",
				equalTo(newArrayList("value", "label")), "_meta.isAbstract", equalTo(false), "_meta.writable",
				equalTo(true), "_meta.languageCode", equalTo("en"), "_href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref1"), "label", equalTo("label1"));
	}

	private void validateRetrieveEntityCollection(ValidatableResponse response)
	{
		response.statusCode(OKE);
		response.body("href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "meta.href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "meta.hrefCollection",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef"), "meta.name", equalTo("it_emx_datatypes_TypeTestRef"),
				"meta.label", equalTo("TypeTestRef"), "meta.description",
				equalTo("MOLGENIS Data types test ref entity"), "meta.attributes[0].href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/value"), "meta.attributes[0].fieldType",
				equalTo("STRING"), "meta.attributes[0].name", equalTo("value"), "meta.attributes[0].label",
				equalTo("value label"), "meta.attributes[0].description", equalTo("TypeTestRef value attribute"),
				"meta.attributes[0].attributes", equalTo(newArrayList()), "meta.attributes[0].maxLength", equalTo(255),
				"meta.attributes[0].auto", equalTo(false), "meta.attributes[0].nillable", equalTo(false),
				"meta.attributes[0].readOnly", equalTo(true), "meta.attributes[0].labelAttribute", equalTo(false),
				"meta.attributes[0].unique", equalTo(true), "meta.attributes[0].visible", equalTo(true),
				"meta.attributes[0].lookupAttribute", equalTo(true), "meta.attributes[0].isAggregatable",
				equalTo(false), "meta.attributes[1].href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/label"),
				"meta.attributes[1].fieldType", equalTo("STRING"), "meta.attributes[1].name", equalTo("label"),
				"meta.attributes[1].label", equalTo("label label"), "meta.attributes[1].description",
				equalTo("TypeTestRef label attribute"), "meta.attributes[1].attributes", equalTo(newArrayList()),
				"meta.attributes[1].maxLength", equalTo(255), "meta.attributes[1].auto", equalTo(false),
				"meta.attributes[1].nillable", equalTo(false), "meta.attributes[1].readOnly", equalTo(false),
				"meta.attributes[1].labelAttribute", equalTo(true), "meta.attributes[1].unique", equalTo(false),
				"meta.attributes[1].visible", equalTo(true), "meta.attributes[1].lookupAttribute", equalTo(true),
				"meta.labelAttribute", equalTo("label"), "meta.idAttribute", equalTo("value"), "meta.lookupAttributes",
				equalTo(newArrayList("value", "label")), "meta.isAbstract", equalTo(false), "meta.writable",
				equalTo(true), "meta.languageCode", equalTo("en"), "start", equalTo(0), "num", equalTo(100), "total",
				equalTo(5), "items[0]._href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref1"), "items[0].value",
				equalTo("ref1"), "items[0].label", equalTo("label1"), "items[1]._href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref2"), "items[1].value", equalTo("ref2"),
				"items[1].label", equalTo("label2"), "items[2]._href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref3"), "items[2].value", equalTo("ref3"),
				"items[2].label", equalTo("label3"), "items[3]._href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref4"), "items[3].value", equalTo("ref4"),
				"items[3].label", equalTo("label4"), "items[4]._href",
				equalTo("/api/v2/it_emx_datatypes_TypeTestRef/ref5"), "items[4].value", equalTo("ref5"),
				"items[4].label", equalTo("label5"));
	}

	private void validateRetrieveEntityAttributeMeta(ValidatableResponse response)
	{
		response.statusCode(OKE);
		response.body("href", equalTo("/api/v2/it_emx_datatypes_TypeTestRef/meta/value"), "fieldType",
				equalTo("STRING"), "name", equalTo("value"), "label", equalTo("value label"), "description",
				equalTo("TypeTestRef value attribute"), "attributes", equalTo(newArrayList()), "maxLength",
				equalTo(255), "auto", equalTo(false), "nillable", equalTo(false), "readOnly", equalTo(true),
				"labelAttribute", equalTo(false), "unique", equalTo(true), "visible", equalTo(true), "lookupAttribute",
				equalTo(true), "isAggregatable", equalTo(false));
	}

	@AfterClass
	public void afterClass()
	{
		// Clean up TestEMX
		removeEntity(adminToken, "it_emx_datatypes_TypeTest");
		removeEntity(adminToken, "it_emx_datatypes_TypeTestRef");
		removeEntity(adminToken, "it_emx_datatypes_Location");
		removeEntity(adminToken, "it_emx_datatypes_Person");

		removeEntity(adminToken, "base_v2ApiTest1");
		removeEntity(adminToken, "base_v2ApiTest2");

		removeEntity(adminToken, "base_APICopyTest");
		removeEntity(adminToken, "base_CopiedEntity");

		// Clean up permissions
		removeRight(adminToken, PACKAGE_PERMISSION_ID);
		removeRight(adminToken, ENTITY_TYPE_PERMISSION_ID);
		removeRight(adminToken, ATTRIBUTE_PERMISSION_ID);

		removeRight(adminToken, TYPE_TEST_PERMISSION_ID);
		removeRight(adminToken, TYPE_TEST_REF_PERMISSION_ID);
		removeRight(adminToken, LOCATION_PERMISSION_ID);
		removeRight(adminToken, PERSONS_PERMISSION_ID);

		removeRight(adminToken, API_TEST_1_PERMISSION_ID);
		removeRight(adminToken, API_TEST_2_PERMISSION_ID);

		// Clean up Token for user
		given().header(X_MOLGENIS_TOKEN, testUserToken).when().post(API_V2 + "logout");

		// Clean up user
		given().header(X_MOLGENIS_TOKEN, adminToken).when().delete("api/v1/sys_sec_User/" + testUserId);
	}

}


