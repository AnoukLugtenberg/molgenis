package org.molgenis.security.usermanager;

import java.util.List;

import org.molgenis.framework.db.DatabaseException;
import org.molgenis.omx.auth.MolgenisGroup;
import org.molgenis.omx.auth.MolgenisUser;

public interface UserManagerService
{
	public List<MolgenisUser> getAllMolgenisUsers() throws DatabaseException;

	public List<MolgenisGroup> getAllMolgenisGroups() throws DatabaseException;

	public List<MolgenisGroup> getGroupsWhereUserIsMember(Integer userId) throws DatabaseException;

	public List<MolgenisGroup> getGroupsWhereUserIsNotMember(Integer userId) throws DatabaseException;

	public List<MolgenisUser> getUsersMemberInGroup(Integer groupId) throws DatabaseException;

	public List<MolgenisUser> addGroup(Integer molgenisGroup_id, Integer molgenisUser_id) throws DatabaseException;
}
