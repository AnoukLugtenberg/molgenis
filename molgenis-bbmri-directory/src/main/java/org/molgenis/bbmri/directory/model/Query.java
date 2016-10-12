package org.molgenis.bbmri.directory.model;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;

import java.util.List;

@AutoValue
@AutoGson(autoValueClass = AutoValue_Query.class)
public abstract class Query
{
	public abstract String getURL();

	public abstract List<Collection> getCollections();

	public abstract Filter getFilters();

	public abstract String getNToken();

	public static Query createQuery(String url, List<Collection> collections, Filter filter, String nToken)
	{
		return new AutoValue_Query(url, collections, filter, nToken);
	}
}
