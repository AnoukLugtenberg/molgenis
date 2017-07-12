package org.molgenis.data.security.acl;

import org.molgenis.data.Entity;

import java.util.List;

/**
 * Entity access control list creation, retrieval, updates and deletions.
 */
public interface EntityAclManager extends EntityAclService
{
	EntityAcl createAcl(Entity entity, List<EntityAce> entityAces);

	void updateAcl(EntityAcl entityAcl);

	void deleteAcl(Entity entity);
}
