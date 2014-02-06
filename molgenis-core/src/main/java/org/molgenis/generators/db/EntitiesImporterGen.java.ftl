<#--helper functions-->
<#include "GeneratorHelper.ftl">

<#--#####################################################################-->
<#--                                                                   ##-->
<#--         START OF THE OUTPUT                                       ##-->
<#--                                                                   ##-->
<#--#####################################################################-->
/* 
 * 
 * generator:   ${generator} ${version}
 *
 * 
 * THIS FILE HAS BEEN GENERATED, PLEASE DO NOT EDIT!
 */
package ${package};

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntitySource;
import org.molgenis.data.Repository;
import org.molgenis.data.validation.ConstraintViolation;
import org.molgenis.data.validation.MolgenisValidationException;
import org.molgenis.data.DatabaseAction;
import org.molgenis.framework.db.EntitiesImporter;
import org.molgenis.framework.db.EntityImportReport;
import org.molgenis.framework.db.EntityImporter;

<#list entities as entity>
<#if !entity.abstract && !entity.system>
import ${entity.namespace}.db.${JavaName(entity)}EntityImporter;
</#if>
</#list>

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

@Component
public class EntitiesImporterImpl implements EntitiesImporter
{
	/** importable entity names (lowercase) */
	private static final Map<String, EntityImporter> ENTITIES_IMPORTABLE;
	
	static {
		// entities added in import order
		ENTITIES_IMPORTABLE = new LinkedHashMap<String, EntityImporter>();
	<#list entities as entity>
		<#if !entity.abstract && !entity.system>
		ENTITIES_IMPORTABLE.put("${entity.name?lower_case}", new ${JavaName(entity)}EntityImporter());
		</#if>
	</#list>
	}
	
	private final DataService dataService;

	@Autowired
	public EntitiesImporterImpl(DataService dataService)
	{
		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		this.dataService = dataService;
	}

	@Override
	@Transactional(rollbackFor =
	{ IOException.class})
	public EntityImportReport importEntities(File file, DatabaseAction dbAction) throws IOException
	{
		return importEntities(dataService.createEntitySource(file), dbAction);
	}
	
	@Override
	@Transactional(rollbackFor =
	{ IOException.class})
	public EntityImportReport importEntities(final Repository repository, final String entityName,
			DatabaseAction dbAction) throws IOException
	{

		return importEntities(new EntitySource()
		{

			@Override
			public Iterable<String> getEntityNames()
			{
				return Collections.singleton(entityName);
			}

			@Override
			public Repository getRepositoryByEntityName(String name)
			{
				return repository;
			}

			@Override
			public void close() throws IOException
			{
				repository.close();
			}

			@Override
			public String getUrl()
			{
				return null;
			}

		}, dbAction);
	}

	@Override
	@Transactional(rollbackFor =
	{ IOException.class})
	public EntityImportReport importEntities(EntitySource entitySource, DatabaseAction dbAction) throws IOException
	{
		EntityImportReport importReport = new EntityImportReport();

		try
		{
			// map entity names on repositories
			Map<String, Repository> repositoryMap = new HashMap<String, Repository>();
			for (String entityName : entitySource.getEntityNames())
			{
				repositoryMap.put(entityName.toLowerCase(), entitySource.getRepositoryByEntityName(entityName));
			}
			
			List<ConstraintViolation> violations = Lists.newArrayList();
			
			
			// import entities in order defined by entities map
			for (Map.Entry<String, EntityImporter> entry : ENTITIES_IMPORTABLE.entrySet())
			{
				String entityName = entry.getKey();
				Repository repository = repositoryMap.get(entityName);
				if (repository != null)
				{
					EntityImporter entityImporter = entry.getValue();
					
					try
					{
						int nr = entityImporter.importEntity(repository, dataService, dbAction);
						if (nr > 0)
						{
							importReport.addEntityCount(entry.getKey(), nr);
							importReport.addNrImported(nr);
						}
					} 
					catch (MolgenisValidationException e)
					{
						for (ConstraintViolation violation : e.getViolations())
						{
							violation.setImportInfo(String.format("Sheet: '%s'", entityName));
							violations.add(violation);
						}

						// Stop if 10 exceptions
						if (violations.size() >= 10)
						{
							throw new MolgenisValidationException(violations);
						}
					}
				}
			}
			
			if (!violations.isEmpty())
			{
				throw new MolgenisValidationException(violations);
			}
		}
		finally
		{
			entitySource.close();
		}

		return importReport;
	}
}