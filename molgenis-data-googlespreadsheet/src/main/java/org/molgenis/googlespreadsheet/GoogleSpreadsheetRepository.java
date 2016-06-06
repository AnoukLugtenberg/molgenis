package org.molgenis.googlespreadsheet;

import static org.molgenis.MolgenisFieldTypes.STRING;
import static org.molgenis.util.ApplicationContextProvider.getApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.molgenis.data.Entity;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.AttributeMetaDataFactory;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.EntityMetaDataFactory;
import org.molgenis.data.support.AbstractRepository;
import org.molgenis.data.support.MapEntity;

import com.google.common.collect.Iterables;
import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.ServiceException;

public class GoogleSpreadsheetRepository extends AbstractRepository
{
	public enum Visibility
	{
		PUBLIC, PRIVATE;

		@Override
		public String toString()
		{
			return this.name().toLowerCase();
		}
	}

	private final SpreadsheetService spreadsheetService;
	private final String spreadsheetKey;
	private final String worksheetId;
	private final Visibility visibility;

	private EntityMetaData entityMetaData;

	public GoogleSpreadsheetRepository(SpreadsheetService spreadsheetService, String spreadsheetKey, String worksheetId)
			throws IOException, ServiceException
	{
		this(spreadsheetService, spreadsheetKey, worksheetId, Visibility.PUBLIC);
	}

	public GoogleSpreadsheetRepository(SpreadsheetService spreadsheetService, String spreadsheetKey,
			String worksheetId, Visibility visibility) throws IOException, ServiceException
	{
		if (spreadsheetService == null) throw new IllegalArgumentException("spreadsheetService is null");
		if (spreadsheetKey == null) throw new IllegalArgumentException("spreadsheetKey is null");
		if (worksheetId == null) throw new IllegalArgumentException("worksheetId is null");
		if (visibility == null) throw new IllegalArgumentException("visibility is null");
		this.spreadsheetService = spreadsheetService;
		this.spreadsheetKey = spreadsheetKey;
		this.worksheetId = worksheetId;
		this.visibility = visibility;
	}

	@Override
	public Iterator<Entity> iterator()
	{
		if (entityMetaData == null) entityMetaData = getEntityMetaData();

		ListFeed feed;
		try
		{
			feed = spreadsheetService.getFeed(
					FeedURLFactory.getDefault().getListFeedUrl(spreadsheetKey, worksheetId, visibility.toString(),
							"full"), ListFeed.class);
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		catch (ServiceException e)
		{
			throw new RuntimeException(e);
		}

		final Iterator<ListEntry> it = feed.getEntries().iterator();
		return new Iterator<Entity>()
		{
			@Override
			public boolean hasNext()
			{
				return it.hasNext();
			}

			@Override
			public Entity next()
			{
				MapEntity entity = new MapEntity();
				CustomElementCollection customElements = it.next().getCustomElements();
				for (AttributeMetaData attributeMetaData : entityMetaData.getAttributes())
				{
					// see remark in getEntityMetaData
					String colName = attributeMetaData.getLabel();
					String normalizedColName = colName.replaceAll("_", "").toLowerCase();
					String value = customElements.getValue(normalizedColName);
					entity.set(colName, value);
				}
				return entity;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		if (entityMetaData == null)
		{
			EntityMetaDataFactory entityMetaFactory = getApplicationContext().getBean(EntityMetaDataFactory.class);
			AttributeMetaDataFactory attrMetaFactory = getApplicationContext().getBean(AttributeMetaDataFactory.class);

			// ListFeed does not give you the true column names, use CellFeed instead
			CellFeed feed;
			try
			{
				URL cellFeedUrl = FeedURLFactory.getDefault().getCellFeedUrl(spreadsheetKey, worksheetId,
						visibility.toString(), "full");
				cellFeedUrl = new URL(cellFeedUrl.toString() + "?min-row=1&max-row=1");
				feed = spreadsheetService.getFeed(cellFeedUrl, CellFeed.class);
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(e);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			catch (ServiceException e)
			{
				throw new RuntimeException(e);
			}

			EntityMetaData entityMetaData = entityMetaFactory.create().setSimpleName(feed.getTitle().getPlainText());

			for (CellEntry cellEntry : feed.getEntries())
			{
				Cell cell = cellEntry.getCell();
				if (cell.getRow() == 1)
				{
					entityMetaData.addAttribute(attrMetaFactory.create().setName(cell.getValue()).setDataType(STRING));
				}
			}
			this.entityMetaData = entityMetaData;
		}

		return entityMetaData;
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		return Collections.emptySet();
	}

	@Override
	public long count()
	{
		return Iterables.size(this);
	}
}
