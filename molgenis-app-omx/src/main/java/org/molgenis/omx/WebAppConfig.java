package org.molgenis.omx;

import org.molgenis.DatabaseConfig;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.data.DataService;
import org.molgenis.data.jpa.JpaEntitySourceRegistrator;
import org.molgenis.elasticsearch.config.EmbeddedElasticSearchConfig;
import org.molgenis.omx.catalogmanager.OmxCatalogManagerService;
import org.molgenis.omx.config.DataExplorerConfig;
import org.molgenis.omx.studymanager.OmxStudyManagerService;
import org.molgenis.search.SearchSecurityConfig;
import org.molgenis.studymanager.StudyManagerService;
import org.molgenis.ui.MolgenisWebAppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableTransactionManagement
@EnableWebMvc
@EnableAsync
@ComponentScan("org.molgenis")
@Import(
{ WebAppSecurityConfig.class, DatabaseConfig.class, OmxConfig.class, EmbeddedElasticSearchConfig.class,
		DataExplorerConfig.class, SearchSecurityConfig.class })
public class WebAppConfig extends MolgenisWebAppConfig implements ApplicationListener<ContextRefreshedEvent>
{
	@Autowired
	private DataService dataService;

	@Bean
	public ApplicationListener<?> jpaEntitySourceRegistrator()
	{
		return new JpaEntitySourceRegistrator(dataService);
	}

	@Bean
	public CatalogManagerService catalogManagerService()
	{
		return new OmxCatalogManagerService(dataService);
	}

	@Bean
	public StudyManagerService studyDefinitionManagerService()
	{
		return new OmxStudyManagerService(dataService);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		dataService.registerEntitySource("excel:///Users/tommydeboer/git/molgenis/molgenis/molgenis-charts/src/test/resources/heatmap.xlsx");
	}

}