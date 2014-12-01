package org.molgenis.study;

import java.util.Date;
import java.util.List;

import org.molgenis.catalog.CatalogFolder;

public class StudyDefinitionImpl implements StudyDefinition
{
	private String id;
	private String name;
	private String description;
	private String version;
	private Date dateCreated;
	private Status status;
	private List<String> authors;
	private String authorEmail;
	private Iterable<CatalogFolder> items;
	private String requestForm;
	private String externalId;

	public StudyDefinitionImpl()
	{
	}

	public StudyDefinitionImpl(StudyDefinition studyDefinition)
	{
		if (studyDefinition == null) throw new IllegalArgumentException("Study definition is null");
		setId(studyDefinition.getId());
		setName(studyDefinition.getName());
		setDescription(studyDefinition.getDescription());
		setVersion(studyDefinition.getVersion());
		setDateCreated(studyDefinition.getDateCreated());
		setStatus(studyDefinition.getStatus());
		setAuthors(studyDefinition.getAuthors());
		setAuthorEmail(studyDefinition.getAuthorEmail());
		setItems(studyDefinition.getItems());
		setRequestForm(studyDefinition.getRequestProposalForm());
		setExternalId(studyDefinition.getExternalId());
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public void setId(String id)
	{
		this.id = id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	@Override
	public Date getDateCreated()
	{
		return dateCreated != null ? new Date(dateCreated.getTime()) : null;
	}

	public void setDateCreated(Date dateCreated)
	{
		this.dateCreated = dateCreated != null ? new Date(dateCreated.getTime()) : null;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	public void setStatus(Status status)
	{
		this.status = status;
	}

	@Override
	public List<String> getAuthors()
	{
		return authors;
	}

	public void setAuthors(List<String> authors)
	{
		this.authors = authors;
	}

	@Override
	public String getAuthorEmail()
	{
		return authorEmail;
	}

	public void setAuthorEmail(String authorEmail)
	{
		this.authorEmail = authorEmail;
	}

	@Override
	public String getRequestProposalForm()
	{
		return requestForm;
	}

	public void setRequestForm(String requestForm)
	{
		this.requestForm = requestForm;
	}

	@Override
	public Iterable<CatalogFolder> getItems()
	{
		return items;
	}

	@Override
	public void setItems(Iterable<CatalogFolder> items)
	{
		this.items = items;
	}

	@Override
	public boolean containsItem(CatalogFolder item)
	{
		throw new UnsupportedOperationException(); // FIXME
	}

	@Override
	public void setRequestProposalForm(String fileName)
	{
		this.requestForm = fileName;
	}

	@Override
	public String getExternalId()
	{
		return externalId;
	}

	@Override
	public void setExternalId(String externalId)
	{
		this.externalId = externalId;
	}
}
