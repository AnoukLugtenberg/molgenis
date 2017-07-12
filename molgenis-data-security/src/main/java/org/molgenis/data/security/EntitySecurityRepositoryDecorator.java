package org.molgenis.data.security;

import org.molgenis.auth.Group;
import org.molgenis.data.*;
import org.molgenis.data.aggregation.AggregateQuery;
import org.molgenis.data.aggregation.AggregateResult;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.security.acl.EntityAce;
import org.molgenis.data.security.acl.EntityAclManager;
import org.molgenis.data.security.acl.SecurityId;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.security.core.Permission;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.user.UserService;
import org.molgenis.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.molgenis.data.security.acl.SecurityId.Type.GROUP;
import static org.molgenis.data.security.acl.SecurityId.Type.USER;
import static org.molgenis.security.core.utils.SecurityUtils.currentUserIsSuOrSystem;
import static org.molgenis.util.EntityUtils.asStream;

/**
 * TODO Deleting entity type should delete ACLs (RepositoryCollectionDecorator?)
 * TODO decide on behavior of deleteAll: delete all entities that this user can retrieve via findAll or try to delete all entities of this entity type
 * TODO decide on behavior of update with regard to ref entities
 * TODO createCurrentUserSids is expensive, is this a performance bottleneck?
 */
public class EntitySecurityRepositoryDecorator extends AbstractRepositoryDecorator<Entity>
{
	private final Repository<Entity> decoratedRepository;
	private final UserService userService;
	private final EntityAclManager entityAclManager;

	public EntitySecurityRepositoryDecorator(Repository<Entity> decoratedRepository, UserService userService,
			EntityAclManager entityAclManager)
	{
		this.decoratedRepository = requireNonNull(decoratedRepository);
		this.userService = requireNonNull(userService);
		this.entityAclManager = requireNonNull(entityAclManager);
	}

	@Override
	protected Repository<Entity> delegate()
	{
		return decoratedRepository;
	}

	@Override
	public void forEachBatched(Fetch fetch, Consumer<List<Entity>> consumer, int batchSize)
	{
		FilteredConsumer filteredConsumer = new FilteredConsumer(consumer, this);
		decoratedRepository.forEachBatched(fetch, filteredConsumer::filter, batchSize);
	}

	@Override
	public long count()
	{
		if (currentUserIsSuOrSystem())
		{
			return super.count();
		}
		// FIXME this requires READ row permissions instead of count
		return findAll(query()).count();
	}

	@Override
	public long count(Query<Entity> q)
	{
		if (currentUserIsSuOrSystem())
		{
			return super.count(q);
		}
		return findAll(q).count();
	}

	@Override
	public void add(Entity entity)
	{
		// current user is allowed to add entity, see RepositorySecurityDecorator
		createAcl(entity);
		decoratedRepository.add(entity);
	}

	private void createAcl(Entity entity)
	{
		System.out.println(entity.getEntityType().getId() + '.' + entity.getIdValue().toString());
		List<EntityAce> entityAces;
		if (!currentUserIsSuOrSystem())
		{
			SecurityId securityId = SecurityId.create(SecurityUtils.getCurrentUsername(), USER);
			EntityAce entityAce = EntityAce.create(Permission.WRITE, securityId, true);
			entityAces = singletonList(entityAce);
		}
		else
		{
			entityAces = emptyList();
		}
		entityAclManager.createAcl(entity, entityAces);
	}

	@Override
	public Integer add(Stream<Entity> entities)
	{
		// current user is allowed to add entities, see RepositorySecurityDecorator
		return decoratedRepository.add(entities.filter(entity ->
		{
			createAcl(entity);
			return true;
		}));
	}

	// TODO update of xrefs mrefs for entity types that are row level secured
	@Override
	public void update(Entity entity)
	{
		validateCurrentUserCanUpdateEntity(entity);
		decoratedRepository.update(entity);
	}

	@Override
	public void update(Stream<Entity> entities)
	{
		decoratedRepository.update(entities.filter(entity ->
		{
			validateCurrentUserCanUpdateEntity(entity);
			return true;
		}));
	}

	@Override
	public void delete(Entity entity)
	{
		validateCurrentUserCanDeleteEntity(entity);
		decoratedRepository.delete(entity);
	}

	@Override
	public void deleteById(Object id)
	{
		validateCurrentUserCanDeleteEntityById(id);
		decoratedRepository.deleteById(id);
	}

	@Override
	public void deleteAll()
	{
		if (currentUserIsSuOrSystem())
		{
			decoratedRepository.deleteAll();
		}
		else
		{
			// delete all entities that the current user can read
			delete(query().findAll());
		}
	}

	@Override
	public void delete(Stream<Entity> entities)
	{
		decoratedRepository.delete(entities.filter(entity ->
		{
			validateCurrentUserCanDeleteEntity(entity);
			return true;
		}));
	}

	@Override
	public void deleteAll(Stream<Object> ids)
	{
		decoratedRepository.deleteAll(ids.filter(id ->
		{
			validateCurrentUserCanDeleteEntityById(id);
			return true;
		}));
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids)
	{
		if (currentUserIsSuOrSystem())
		{
			return decoratedRepository.findAll(ids);
		}

		return decoratedRepository.findAll(ids).filter(this::currentUserCanReadEntity);
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids, Fetch fetch)
	{
		if (currentUserIsSuOrSystem())
		{
			return decoratedRepository.findAll(ids, fetch);
		}

		return decoratedRepository.findAll(ids, fetch).filter(this::currentUserCanReadEntity);
	}

	@Override
	public Stream<Entity> findAll(Query<Entity> q)
	{
		if (currentUserIsSuOrSystem())
		{
			return decoratedRepository.findAll(q);
		}

		Query<Entity> qWithoutLimitOffset = new QueryImpl<>(q);
		qWithoutLimitOffset.offset(0).pageSize(Integer.MAX_VALUE);
		Stream<Entity> entityStream = decoratedRepository.findAll(qWithoutLimitOffset)
				.filter(this::currentUserCanReadEntity);
		if (q.getOffset() > 0)
		{
			entityStream = entityStream.skip(q.getOffset());
		}
		if (q.getPageSize() > 0)
		{
			entityStream = entityStream.limit(q.getPageSize());
		}
		return entityStream;
	}

	// TODO we do not use Elasticsearch aggregates here
	@Override
	public AggregateResult aggregate(AggregateQuery aggregateQuery)
	{
		if (currentUserIsSuOrSystem())
		{
			return decoratedRepository.aggregate(aggregateQuery);
		}

		if (aggregateQuery.getAttributeDistinct() != null)
		{
			throw new UnsupportedOperationException("not implemented");
		}

		Query<Entity> query = aggregateQuery.getQuery();
		Stream<Entity> entityStream = query != null ? findAll(query) : findAll(query());

		Attribute attributeX = aggregateQuery.getAttributeX();
		Attribute attributeY = aggregateQuery.getAttributeY();

		List<List<Long>> matrix;
		List<Object> xLabels, yLabels;
		if (attributeY == null)
		{
			Map<Object, Long> countMap = entityStream.map(entity -> entity.get(attributeX.getName()))
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			xLabels = newArrayList(countMap.keySet());
			yLabels = emptyList();
			matrix = singletonList(newArrayList(countMap.values()));
		}
		else
		{
			Map<Pair<Object, Object>, Long> countMap = entityStream
					.map(entity -> new Pair<>(entity.get(attributeX.getName()), entity.get(attributeY.getName())))
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			xLabels = newArrayList(countMap.keySet().stream().map(Pair::getA).collect(toSet()));
			yLabels = newArrayList(countMap.keySet().stream().map(Pair::getB).collect(toSet()));
			matrix = new ArrayList<>(xLabels.size());
			for (int i = 0; i < xLabels.size(); ++i)
			{
				ArrayList<Long> yValues = new ArrayList<>(yLabels.size());
				for (int y = 0; y < yLabels.size(); ++y)
				{
					yValues.add(0L);
				}
				matrix.add(yValues);
			}
			countMap.forEach((pair, count) ->
			{
				int xIndex = xLabels.indexOf(pair.getA());
				int yIndex = yLabels.indexOf(pair.getB());
				matrix.get(xIndex).set(yIndex, count);
			});
		}
		return new AggregateResult(matrix, xLabels, yLabels);
	}

	private List<SecurityId> createCurrentUserSecurityIds()
	{
		String currentUsername = SecurityUtils.getCurrentUsername();
		List<String> groupIds = asStream(userService.getUserGroups(currentUsername)).map(Group::getId)
				.collect(toList());
		List<SecurityId> sids = new ArrayList<>(groupIds.size() + 1);
		sids.add(SecurityId.create(currentUsername, USER));
		groupIds.forEach(groupId -> sids.add(SecurityId.create(groupId, GROUP)));
		return sids;
	}

	private boolean currentUserCanReadEntity(Entity entity)
	{
		return currentUserIsSuOrSystem() || currentUserCanAccessEntity(entity, Permission.READ);
	}

	private void validateCurrentUserCanUpdateEntity(Entity entity)
	{
		if (currentUserIsSuOrSystem())
		{
			return;
		}
		validateCurrentUserCanAccessEntity(entity, Permission.WRITE);
	}

	private void validateCurrentUserCanDeleteEntity(Entity entity)
	{
		if (currentUserIsSuOrSystem())
		{
			return;
		}
		validateCurrentUserCanAccessEntity(entity, Permission.WRITE);
	}

	private void validateCurrentUserCanDeleteEntityById(Object entityId)
	{
		if (currentUserIsSuOrSystem())
		{
			return;
		}

		Entity entity = findOneById(entityId);
		if (entity == null)
		{
			String entityIdStr = entityId.toString();
			String entityTypeIdStr = getEntityType().getIdValue().toString();
			throw new UnknownEntityException(format("Unknown entity [%s] of type [%s]", entityIdStr, entityTypeIdStr));
		}

		validateCurrentUserCanAccessEntity(entity, Permission.WRITE);
	}

	private boolean currentUserCanAccessEntity(Entity entity, Permission permission)
	{
		List<Permission> permissions = expandPermissions(permission);
		List<SecurityId> securityIds = createCurrentUserSecurityIds();
		return entityAclManager.isGranted(entity, permissions, securityIds);
	}

	private static List<Permission> expandPermissions(Permission permission)
	{
		switch (permission)
		{
			case NONE:
				return singletonList(Permission.NONE);
			case COUNT:
				return Arrays.asList(Permission.COUNT, Permission.READ, Permission.WRITE, Permission.WRITEMETA);
			case READ:
				return Arrays.asList(Permission.READ, Permission.WRITE, Permission.WRITEMETA);
			case WRITE:
				return Arrays.asList(Permission.WRITE, Permission.WRITEMETA);
			case WRITEMETA:
				return singletonList(Permission.WRITEMETA);
			default:
				throw new RuntimeException(String.format("Unknown permission '%s'", permission.toString()));
		}
	}

	private void validateCurrentUserCanAccessEntity(Entity entity, Permission permission)
	{
		boolean isGranted = currentUserCanAccessEntity(entity, permission);
		if (!isGranted)
		{
			String entityIdStr = entity.getIdValue().toString();
			String entityTypeIdStr = getEntityType().getIdValue().toString();
			throw new MolgenisDataAccessException(
					format("Updating entity [%s] of type [%s] is not allowed", entityIdStr, entityTypeIdStr));
		}
	}

	private class FilteredConsumer
	{
		private final Consumer<List<Entity>> consumer;
		private final EntitySecurityRepositoryDecorator entitySecurityRepositoryDecorator;

		FilteredConsumer(Consumer<List<Entity>> consumer,
				EntitySecurityRepositoryDecorator entitySecurityRepositoryDecorator)
		{
			this.consumer = requireNonNull(consumer);
			this.entitySecurityRepositoryDecorator = requireNonNull(entitySecurityRepositoryDecorator);
		}

		public void filter(List<Entity> entities)
		{
			Stream<Entity> filteredEntities = entities.stream()
					.filter(entitySecurityRepositoryDecorator::currentUserCanReadEntity);
			consumer.accept(filteredEntities.collect(toList()));
		}
	}
}