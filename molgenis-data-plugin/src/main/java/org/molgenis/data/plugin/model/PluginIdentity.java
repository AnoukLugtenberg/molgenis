package org.molgenis.data.plugin.model;

import org.springframework.security.acls.domain.ObjectIdentityImpl;

public class PluginIdentity extends ObjectIdentityImpl
{
	public static final String PLUGIN = "plugin";

	public PluginIdentity(Plugin plugin)
	{
		this(plugin.getId());
	}

	public PluginIdentity(String pluginId)
	{
		super(PLUGIN, pluginId);
	}
}
