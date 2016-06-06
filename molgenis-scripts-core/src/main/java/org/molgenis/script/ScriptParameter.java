package org.molgenis.script;

import static org.molgenis.script.ScriptParameterMetaData.NAME;
import static org.molgenis.script.ScriptParameterMetaData.SCRIPT_PARAMETER;

import org.molgenis.data.Entity;
import org.molgenis.data.meta.SystemEntity;

public class ScriptParameter extends SystemEntity
{
	public ScriptParameter(Entity entity)
	{
		super(entity, SCRIPT_PARAMETER);
	}

	public ScriptParameter(ScriptParameterMetaData scriptParameterMetaData)
	{
		super(scriptParameterMetaData);
	}

	public ScriptParameter(String name, ScriptParameterMetaData scriptParameterMetaData)
	{
		super(scriptParameterMetaData);
		setName(name);
	}

	public void setName(String name)
	{
		set(NAME, name);
	}

	public String getName()
	{
		return getString(NAME);
	}
}
