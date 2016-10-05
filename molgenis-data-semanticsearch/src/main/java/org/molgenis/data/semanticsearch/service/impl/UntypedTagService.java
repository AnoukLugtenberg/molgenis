package org.molgenis.data.semanticsearch.service.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.meta.model.*;
import org.molgenis.data.meta.model.Package;
import org.molgenis.data.semantic.LabeledResource;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semantic.SemanticTag;
import org.molgenis.data.semanticsearch.repository.TagRepository;
import org.molgenis.data.semanticsearch.service.TagService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.security.core.runas.RunAsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.meta.model.AttributeMetaDataMetaData.ATTRIBUTE_META_DATA;
import static org.molgenis.data.meta.model.EntityTypeMetadata.ATTRIBUTES;
import static org.molgenis.data.meta.model.EntityTypeMetadata.ENTITY_META_DATA;
import static org.molgenis.data.meta.model.PackageMetaData.PACKAGE;

/**
 * Service to tag metadata with simple String terms.
 */
public class UntypedTagService implements TagService<LabeledResource, LabeledResource>
{
	private static final Logger LOG = LoggerFactory.getLogger(UntypedTagService.class);

	private final DataService dataService;
	private final TagRepository tagRepository;

	public UntypedTagService(DataService dataService, TagRepository tagRepository)
	{
		this.dataService = dataService;
		this.tagRepository = tagRepository;
	}

	private Entity findAttributeEntity(EntityType entityType, String attributeName)
	{
		Entity EntityTypeEntity = dataService.findOneById(ENTITY_META_DATA, entityType.getName());
		Optional<Entity> result = stream(EntityTypeEntity.getEntities(ATTRIBUTES).spliterator(), false)
				.filter(att -> attributeName.equals(att.getString(AttributeMetaDataMetaData.NAME))).findFirst();
		return result.isPresent() ? result.get() : null;
	}

	private Entity findEntity(EntityType emd)
	{
		return dataService.findOneById(ENTITY_META_DATA, emd.getName());
	}

	@Override
	public void removeAttributeTag(EntityType entityType,
			SemanticTag<AttributeMetaData, LabeledResource, LabeledResource> removeTag)
	{
		AttributeMetaData attributeMetaData = removeTag.getSubject();
		Entity attributeEntity = findAttributeEntity(entityType, attributeMetaData.getName());
		List<Entity> tags = new ArrayList<Entity>();
		for (Entity tagEntity : attributeEntity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			SemanticTag<AttributeMetaData, LabeledResource, LabeledResource> tag = SemanticTag
					.asTag(attributeMetaData, tagEntity);
			if (!removeTag.equals(tag))
			{
				tags.add(tagEntity);
			}
		}
		attributeEntity.set(AttributeMetaDataMetaData.TAGS, tags);
		dataService.update(ATTRIBUTE_META_DATA, attributeEntity);
	}

	@Override
	@RunAsSystem
	public Multimap<Relation, LabeledResource> getTagsForAttribute(EntityType entityType,
			AttributeMetaData attributeMetaData)
	{
		Entity entity = findAttributeEntity(entityType, attributeMetaData.getName());
		if (entity == null) return ArrayListMultimap.<Relation, LabeledResource>create();

		Multimap<Relation, LabeledResource> tags = ArrayListMultimap.<Relation, LabeledResource>create();
		for (Entity tagEntity : entity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			SemanticTag<AttributeMetaData, LabeledResource, LabeledResource> tag = SemanticTag
					.asTag(attributeMetaData, tagEntity);
			tags.put(tag.getRelation(), tag.getObject());
		}
		return tags;
	}

	@Override
	@RunAsSystem
	public Iterable<SemanticTag<EntityType, LabeledResource, LabeledResource>> getTagsForEntity(
			EntityType entityType)
	{
		List<SemanticTag<EntityType, LabeledResource, LabeledResource>> result = new ArrayList<SemanticTag<EntityType, LabeledResource, LabeledResource>>();
		Entity entity = findEntity(entityType);
		if (entity == null)
		{
			LOG.warn("No known entity with name " + entityType.getName() + ".");
		}
		else
		{
			for (Entity tagEntity : entity.getEntities(EntityTypeMetadata.TAGS))
			{
				result.add(SemanticTag.asTag(entityType, tagEntity));
			}
		}
		return result;
	}

	@Override
	public void addAttributeTag(EntityType entityType,
			SemanticTag<AttributeMetaData, LabeledResource, LabeledResource> tag)
	{
		Entity entity = findAttributeEntity(entityType, tag.getSubject().getName());
		List<Entity> tags = new ArrayList<Entity>();
		for (Entity tagEntity : entity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			tags.add(tagEntity);
		}
		tags.add(getTagEntity(tag));
		entity.set(AttributeMetaDataMetaData.TAGS, tags);
		dataService.update(ATTRIBUTE_META_DATA, entity);
	}

	@Override
	public void addEntityTag(SemanticTag<EntityType, LabeledResource, LabeledResource> tag)
	{
		Entity entity = findEntity(tag.getSubject());
		if (entity == null)
		{
			throw new UnknownEntityException("Unknown entity [" + tag.getSubject().getName() + "]");
		}
		ImmutableList<SemanticTag<EntityType, LabeledResource, LabeledResource>> existingTags = ImmutableList.<SemanticTag<EntityType, LabeledResource, LabeledResource>>copyOf(
				getTagsForEntity(tag.getSubject()));
		if (existingTags.contains(tag))
		{
			LOG.debug("Tag already present");
			return;
		}

		ImmutableList.Builder<Entity> builder = ImmutableList.<Entity>builder();
		builder.addAll(entity.getEntities(EntityTypeMetadata.TAGS));
		builder.add(getTagEntity(tag));
		entity.set(EntityTypeMetadata.TAGS, builder.build());
		dataService.update(ENTITY_META_DATA, entity);
	}

	public Entity getTagEntity(SemanticTag<?, LabeledResource, LabeledResource> tag)
	{
		return tagRepository.getTagEntity(tag.getObject().getIri(), tag.getObject().getLabel(), tag.getRelation(),
				tag.getCodeSystem().getIri());
	}

	@Override
	@RunAsSystem
	public Iterable<SemanticTag<Package, LabeledResource, LabeledResource>> getTagsForPackage(Package p)
	{
		Entity packageEntity = dataService
				.findOne(PACKAGE, new QueryImpl<Entity>().eq(PackageMetaData.FULL_NAME, p.getName()));

		if (packageEntity == null)
		{
			throw new UnknownEntityException("Unknown package [" + p.getName() + "]");
		}

		List<SemanticTag<Package, LabeledResource, LabeledResource>> tags = Lists.newArrayList();
		for (Entity tagEntity : packageEntity.getEntities(PackageMetaData.TAGS))
		{
			tags.add(SemanticTag.asTag(p, tagEntity));
		}

		return tags;
	}

	@Override
	public void removeEntityTag(SemanticTag<EntityType, LabeledResource, LabeledResource> tag)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void removeAllTagsFromEntity(String entityName)
	{
		// TODO Auto-generated method stub

	}
}
