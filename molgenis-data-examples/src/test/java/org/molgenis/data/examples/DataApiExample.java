package org.molgenis.data.examples;

import static org.molgenis.data.examples.UserMetaData.USER;

import java.io.File;

import org.molgenis.data.DataService;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.util.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(classes = AppConfig.class)
public class DataApiExample extends AbstractTestNGSpringContextTests
{
	@Autowired
	DataService dataService;

	// @Test
	public void testStatic()
	{
		// Add some users
		dataService.add(USER, new User("Piet", true));
		dataService.add(USER, new User("Klaas", false));

		// Retrieve them and print
		printUsers();

		// Find Klaas
		User klaas = dataService.findOneById(USER, "Klaas", User.class);
		System.out.println(klaas);

		// Make klaas active
		klaas.setActive(true);
		dataService.update(USER, klaas);

		// Find all active
		dataService.query(USER, User.class).eq(UserMetaData.ACTIVE, true).findAll()
				.forEach(System.out::println);
		// OR ??
		dataService.getRepository(USER).query().eq(UserMetaData.ACTIVE, true)
				.forEach(System.out::println);

		// Delete one
		dataService.deleteById(USER, "Piet");
		printUsers();

		// Discover capabilities of repo
		dataService.getCapabilities(USER).forEach(System.out::println);

		// Add streaming
		File usersCsv = ResourceUtils.getFile("users.csv");
		dataService.add(USER, new CsvRepository(usersCsv, null).stream());
		printUsers();
	}

	private void printUsers()
	{
		dataService.findAll(USER, User.class).forEach(System.out::println);
	}

	// @Test
	public void testDynamic()
	{
		//		// Create new dynamic repo
		//		EntityMetaData emd = new EntityMetaDataImpl("City");
		//		emd.addAttribute("name", ROLE_ID);
		//		emd.addAttribute("population").setDataType(MolgenisFieldTypes.INT);
		//
		//		Repository<Entity> repo = dataService.getMeta().addEntityMeta(emd);
		//
		//		// Add entities to it
		//		Entity amsterdam = new MapEntity();
		//		amsterdam.set("name", "Amsterdam");
		//		amsterdam.set("population", 813562);
		//		repo.add(amsterdam);
		//
		//		Entity london = new MapEntity();
		//		london.set("name", "London");
		//		london.set("population", 8416535);
		//		repo.add(london);
		//
		//		// Retrieve all entities of repo
		//		dataService.findAll("City").forEach((entity) -> System.out.println(entity.get("name")));
		//
		//		// Add attribute
		//		emd.addAttribute("country");
		//		dataService.getMeta().updateEntityMeta(emd);
		//
		//		// Print attributes
		//		dataService.getEntityMetaData("City").getAtomicAttributes()
		//				.forEach((attr) -> System.out.println(attr.getName()));
		//
		//		// Update entity
		//		amsterdam.set("country", "Netherlands");
		//		dataService.update("City", amsterdam);
		//		Entity entity = dataService.findOneById("City", "Amsterdam");
		//		System.out.println(entity.get("name") + ": " + entity.get("country"));
	}

	@Test
	public void testRepositoryCollections()
	{
		//		// Print all available backends
		//		dataService.getMeta().forEach((backend) -> System.out.println(backend.getName()));
		//
		//		// Add cities to MyRepo
		//		EntityMetaData emd = new EntityMetaDataImpl("City1");
		//		emd.setBackend("MyRepos");
		//		emd.addAttribute("name", ROLE_ID);
		//		emd.addAttribute("population").setDataType(MolgenisFieldTypes.INT);
		//
		//		Repository<Entity> repo = dataService.getMeta().addEntityMeta(emd);
		//		System.out.println(repo);
		//
		//		// Add entities to it
		//		Entity amsterdam = new MapEntity();
		//		amsterdam.set("name", "Amsterdam");
		//		amsterdam.set("population", 813562);
		//		repo.add(amsterdam);
		//
		//		Entity london = new MapEntity();
		//		london.set("name", "London");
		//		london.set("population", 8416535);
		//		repo.add(london);
		//
		//		// Retrieve all entities of repo
		//		repo.forEach((entity) -> System.out.println(entity.get("name")));
	}
}
