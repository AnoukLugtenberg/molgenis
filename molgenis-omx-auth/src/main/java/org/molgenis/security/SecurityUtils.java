package org.molgenis.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils
{
	public static final String AUTHORITY_PLUGIN_PREFIX = "ROLE_PLUGIN_";
	public static final String AUTHORITY_ENTITY_PREFIX = "ROLE_ENTITY_";
	public static final String AUTHORITY_READ_POSTFIX = "_READ_USER";
	public static final String AUTHORITY_WRITE_POSTFIX = "_WRITE_USER";

	public static final GrantedAuthority GRANTED_AUTHORITY_SU = new SimpleGrantedAuthority("ROLE_SU");

	public static String getCurrentUsername()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) return null;

		Object principal = authentication.getPrincipal();
		if (principal instanceof UserDetails) return ((UserDetails) principal).getUsername();
		else return principal.toString();
	}

	public static boolean currentUserIsSu()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) return false;

		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		if (authorities == null) throw new IllegalStateException("No current user logged in");

		for (GrantedAuthority authority : authorities)
		{
			if (authority.getAuthority().equals(GRANTED_AUTHORITY_SU.getAuthority())) return true;
		}

		return false;
	}

	public static boolean currentUserIsAuthenticated()
	{
		String username;
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserDetails) username = ((UserDetails) principal).getUsername();
		else username = principal.toString();
		return authentication.isAuthenticated() && !username.equals("anonymousUser");
	}

	public static String[] defaultPluginAuthorities(String pluginId)
	{
		return new String[]
		{ GRANTED_AUTHORITY_SU.getAuthority(), getPluginReadAuthority(pluginId), getPluginWriteAuthority(pluginId) };
	}

	private static String getPluginReadAuthority(String pluginId)
	{
		return AUTHORITY_PLUGIN_PREFIX + pluginId.toUpperCase() + AUTHORITY_READ_POSTFIX;
	}

	private static String getPluginWriteAuthority(String pluginId)
	{
		return AUTHORITY_PLUGIN_PREFIX + pluginId.toUpperCase() + AUTHORITY_WRITE_POSTFIX;
	}
}
