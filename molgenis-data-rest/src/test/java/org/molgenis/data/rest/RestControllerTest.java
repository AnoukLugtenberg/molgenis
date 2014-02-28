package org.molgenis.data.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.molgenis.data.rest.RestController.BASE_URI;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.mockito.Matchers;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.Query;
import org.molgenis.data.Queryable;
import org.molgenis.data.Repository;
import org.molgenis.data.Updateable;
import org.molgenis.data.rest.RestControllerTest.RestControllerConfig;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.util.GsonHttpMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@WebAppConfiguration
@ContextConfiguration(classes = RestControllerConfig.class)
public class RestControllerTest extends AbstractTestNGSpringContextTests
{
	private static String ENTITY_NAME = "Person";
	private static String HREF_ENTITY = BASE_URI + "/" + ENTITY_NAME;
	private static String HREF_ENTITY_META = HREF_ENTITY + "/meta";
	private static String HREF_ENTITY_ID = HREF_ENTITY + "/1";

	@Autowired
	private RestController restController;

	@Autowired
	private DataService dataService;

	private MockMvc mockMvc;

	@BeforeMethod
	public void beforeMethod()
	{
		reset(dataService);

		Repository repo = mock(Repository.class, withSettings().extraInterfaces(Updateable.class, Queryable.class));

		Entity entity = new MapEntity("id");
		entity.set("id", 1);
		entity.set("name", "Piet");

		when(dataService.getEntityNames()).thenReturn(Arrays.asList(ENTITY_NAME));
		when(dataService.getRepositoryByEntityName(ENTITY_NAME)).thenReturn(repo);

		when(dataService.add(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class))).thenReturn(1);
		when(dataService.findOne(ENTITY_NAME, 1)).thenReturn(entity);

		Query q = new QueryImpl().eq("name", "Piet").pageSize(10).offset(5);
		when(dataService.findAll(ENTITY_NAME, q)).thenReturn(Arrays.asList(entity));

		AttributeMetaData attrName = new DefaultAttributeMetaData("name", FieldTypeEnum.STRING);
		DefaultAttributeMetaData attrId = new DefaultAttributeMetaData("id", FieldTypeEnum.INT);
		attrId.setIdAttribute(true);
		attrId.setVisible(false);

		when(repo.getAttribute("name")).thenReturn(attrName);
		when(repo.getIdAttribute()).thenReturn(attrId);
		when(repo.getAttributes()).thenReturn(Arrays.<AttributeMetaData> asList(attrName, attrId));
		when(repo.getAtomicAttributes()).thenReturn(Arrays.<AttributeMetaData> asList(attrName, attrId));
		when(repo.getName()).thenReturn(ENTITY_NAME);

		mockMvc = MockMvcBuilders.standaloneSetup(restController).setMessageConverters(new GsonHttpMessageConverter())
				.build();
	}

	@Test
	public void create() throws Exception
	{
		mockMvc.perform(post(HREF_ENTITY).content("{name:Piet}").contentType(APPLICATION_JSON))
				.andExpect(status().isCreated()).andExpect(header().string("Location", HREF_ENTITY_ID));

		verify(dataService).add(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class));
	}

	@Test
	public void createFromFormPost() throws Exception
	{
		mockMvc.perform(post(HREF_ENTITY).contentType(APPLICATION_FORM_URLENCODED).param("name", "Piet"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", HREF_ENTITY_ID));

		verify(dataService).add(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class));
	}

	@Test
	public void deleteDelete() throws Exception
	{
		mockMvc.perform(delete(HREF_ENTITY_ID)).andExpect(status().isNoContent());
		verify(dataService).delete(ENTITY_NAME, 1);
	}

	@Test
	public void deletePost() throws Exception
	{
		mockMvc.perform(post(HREF_ENTITY_ID).param("_method", "DELETE")).andExpect(status().isNoContent());
		verify(dataService).delete(ENTITY_NAME, 1);
	}

	@Test
	public void getEntityMetaData() throws Exception
	{
		mockMvc.perform(get(HREF_ENTITY_META))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(
						content().string(
								"{\"href\":\"" + HREF_ENTITY_META + "\",\"name\":\"" + ENTITY_NAME
										+ "\",\"attributes\":{\"name\":{\"href\":\"" + HREF_ENTITY_META + "/name\"}}}"));
	}

	@Test
	public void getEntityMetaDataSelectAttributes() throws Exception
	{
		mockMvc.perform(get(HREF_ENTITY_META).param("attributes", "name"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string("{\"href\":\"" + HREF_ENTITY_META + "\",\"name\":\"" + ENTITY_NAME + "\"}"));
	}

	@Test
	public void getEntityMetaDataExpandAttributes() throws Exception
	{
		mockMvc.perform(get(HREF_ENTITY_META).param("expand", "attributes"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(
						content()
								.string("{\"href\":\""
										+ HREF_ENTITY_META
										+ "\",\"name\":\""
										+ ENTITY_NAME
										+ "\",\"attributes\":{\"name\":{\"href\":\""
										+ HREF_ENTITY_META
										+ "/name\",\"fieldType\":\"STRING\",\"name\":\"name\",\"label\":\"name\",\"nillable\":true,\"readOnly\":false,\"labelAttribute\":false,\"unique\":false}}}"));

	}

	@Test
	public void retrieve() throws Exception
	{
		mockMvc.perform(get(HREF_ENTITY_ID)).andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string("{\"href\":\"" + HREF_ENTITY_ID + "\",\"name\":\"Piet\"}"));

	}

	@Test
	public void retrieveSelectAttributes() throws Exception
	{
		mockMvc.perform(get(HREF_ENTITY_ID).param("attributes", "notname")).andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(content().string("{\"href\":\"" + HREF_ENTITY_ID + "\"}"));

	}

	@Test
	public void retrieveEntityCollection() throws Exception
	{
		mockMvc.perform(
				get(HREF_ENTITY).param("start", "5").param("num", "10").param("q[0].operator", "EQUALS")
						.param("q[0].field", "name").param("q[0].value", "Piet"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(
						content().string(
								"{\"href\":\"" + HREF_ENTITY + "\",\"start\":5,\"num\":10,\"total\":0,\"prevHref\":\""
										+ HREF_ENTITY + "?start=0&num=10\",\"items\":[{\"href\":\"" + HREF_ENTITY_ID
										+ "\",\"name\":\"Piet\"}]}"));

	}

	@Test
	public void retrieveEntityCollectionPost() throws Exception
	{
		String json = "{start:5, num:10, q:[{operator:EQUALS,field:name,value:Piet}]}";

		mockMvc.perform(post(HREF_ENTITY).param("_method", "GET").content(json).contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(
						content().string(
								"{\"href\":\"" + HREF_ENTITY + "\",\"start\":5,\"num\":10,\"total\":0,\"prevHref\":\""
										+ HREF_ENTITY + "?start=0&num=10\",\"items\":[{\"href\":\"" + HREF_ENTITY_ID
										+ "\",\"name\":\"Piet\"}]}"));

	}

	@Test
	public void update() throws Exception
	{
		mockMvc.perform(put(HREF_ENTITY_ID).content("{name:Klaas}").contentType(APPLICATION_JSON)).andExpect(
				status().isOk());

		verify(dataService).update(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class));
	}

	@Test
	public void updateFromFormPost() throws Exception
	{
		mockMvc.perform(
				post(HREF_ENTITY_ID).param("_method", "PUT").param("name", "Klaas")
						.contentType(APPLICATION_FORM_URLENCODED)).andExpect(status().isNoContent());

		verify(dataService).update(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class));
	}

	@Test
	public void updatePost() throws Exception
	{
		mockMvc.perform(
				post(HREF_ENTITY_ID).param("_method", "PUT").content("{name:Klaas}").contentType(APPLICATION_JSON))
				.andExpect(status().isOk());

		verify(dataService).update(Matchers.eq(ENTITY_NAME), Matchers.any(MapEntity.class));
	}

	@Test
	public void unknownEntity() throws Exception
	{
		mockMvc.perform(get(BASE_URI + "/bogus/1")).andExpect(status().isNotFound());
	}

	@Test
	public void molgenisDataAccessException() throws Exception
	{
		when(dataService.findOne(ENTITY_NAME, 1)).thenThrow(new MolgenisDataAccessException());
		mockMvc.perform(get(HREF_ENTITY_ID)).andExpect(status().isUnauthorized());
	}

	@Configuration
	public static class RestControllerConfig extends WebMvcConfigurerAdapter
	{
		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public RestController restController()
		{
			return new RestController(dataService());
		}
	}

}
