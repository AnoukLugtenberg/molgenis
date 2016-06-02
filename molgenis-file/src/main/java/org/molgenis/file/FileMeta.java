package org.molgenis.file;

import static org.molgenis.file.FileMetaMetaData.CONTENT_TYPE;
import static org.molgenis.file.FileMetaMetaData.FILENAME;
import static org.molgenis.file.FileMetaMetaData.ID;
import static org.molgenis.file.FileMetaMetaData.SIZE;
import static org.molgenis.file.FileMetaMetaData.URL;

import org.molgenis.data.Entity;
import org.molgenis.data.meta.SystemEntity;
import org.molgenis.data.support.OwnedEntityMetaData;

public class FileMeta extends SystemEntity
{
	public FileMeta(Entity entity)
	{
		super(entity);
	}

	public FileMeta(FileMetaMetaData fileMetaMetaData)
	{
		super(fileMetaMetaData);
	}

	public FileMeta(String id, FileMetaMetaData fileMetaMetaData)
	{
		super(fileMetaMetaData);
		setId(id);
	}

	public String getId()
	{
		return getString(ID);
	}

	public void setId(String id)
	{
		set(ID, id);
	}

	public String getFilename()
	{
		return getString(FILENAME);
	}

	public void setFilename(String filename)
	{
		set(FILENAME, filename);
	}

	public String getContentType()
	{
		return getString(CONTENT_TYPE);
	}

	public void setContentType(String contentType)
	{
		set(CONTENT_TYPE, contentType);
	}

	public Long getSize()
	{
		return getLong(SIZE);
	}

	public void setSize(Long size)
	{
		set(SIZE, size);
	}

	public String getUrl()
	{
		return getString(URL);
	}

	public void setUrl(String url)
	{
		set(URL, url);
	}

	public String getOwnerUsername()
	{
		return getString(OwnedEntityMetaData.ATTR_OWNER_USERNAME);
	}
}
