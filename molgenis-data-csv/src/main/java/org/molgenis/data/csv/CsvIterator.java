package org.molgenis.data.csv;

import static org.molgenis.data.csv.CsvRepositoryCollection.EXTENSION_CSV;
import static org.molgenis.data.csv.CsvRepositoryCollection.EXTENSION_TSV;
import static org.molgenis.data.csv.CsvRepositoryCollection.EXTENSION_TXT;
import static org.molgenis.data.csv.CsvRepositoryCollection.EXTENSION_ZIP;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.processor.AbstractCellProcessor;
import org.molgenis.data.processor.CellProcessor;
import org.molgenis.data.support.MapEntity;
import org.molgenis.util.CloseableIterator;
import org.springframework.util.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

public class CsvIterator implements CloseableIterator<Entity>
{
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private final String repositoryName;
	private ZipFile zipFile;
	private CSVReader csvReader;
	private final List<CellProcessor> cellProcessors;
	private final Map<String, Integer> colNamesMap; // column names index
	private MapEntity next;
	private boolean getNext = true;

	public CsvIterator(File file, String repositoryName, List<CellProcessor> cellProcessors)
	{
		this.repositoryName = repositoryName;
		this.cellProcessors = cellProcessors;

		try
		{
			if (StringUtils.getFilenameExtension(file.getName()).equalsIgnoreCase(EXTENSION_ZIP))
			{
				zipFile = new ZipFile(file.getAbsolutePath());
				for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();)
				{
					ZipEntry entry = e.nextElement();
					if (StringUtils.stripFilenameExtension(entry.getName()).equalsIgnoreCase(repositoryName))
					{
						csvReader = createCSVReader(entry.getName(), zipFile.getInputStream(entry));
						break;
					}
				}

			}
			else if (file.getName().toLowerCase().startsWith(repositoryName.toLowerCase()))
			{
				csvReader = createCSVReader(file.getName(), new FileInputStream(file));
			}

			if (csvReader == null)
			{
				throw new UnknownEntityException("Unknown entity [" + repositoryName + "] ");
			}

			colNamesMap = toColNamesMap(csvReader.readNext());
		}
		catch (IOException e)
		{
			throw new MolgenisDataException("Exception reading [" + file.getAbsolutePath() + "]", e);
		}
	}

	public Map<String, Integer> getColNamesMap()
	{
		return colNamesMap;
	}

	@Override
	public boolean hasNext()
	{
		boolean next = get() != null;
		if (!next)
		{
			close();
		}

		return next;
	}

	@Override
	public MapEntity next()
	{
		MapEntity entity = get();
		getNext = true;
		return entity;
	}

	private MapEntity get()
	{
		if (getNext)
		{
			try
			{
				String[] values = csvReader.readNext();

				if (values != null)
				{
					for (int i = 0; i < values.length; ++i)
					{
						// subsequent separators indicate
						// null
						// values instead of empty strings
						String value = values[i].isEmpty() ? null : values[i];
						values[i] = processCell(value, false);
					}

					next = new MapEntity();

					List<String> valueList = Arrays.asList(values);
					for (String name : colNamesMap.keySet())
					{
						next.set(name, valueList.get(colNamesMap.get(name)));
					}
				}
				else
				{
					next = null;
				}

				getNext = false;
			}
			catch (IOException e)
			{
				throw new MolgenisDataException("Exception reading line of csv file [" + repositoryName + "]", e);
			}
		}

		return next;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void close()
	{
		IOUtils.closeQuietly(csvReader);

		if (zipFile != null)
		{
			IOUtils.closeQuietly(zipFile);
		}
	}

	private CSVReader createCSVReader(String fileName, InputStream in)
	{
		Reader reader = new InputStreamReader(in, CHARSET);

		if (fileName.toLowerCase().endsWith("." + EXTENSION_CSV)
				|| fileName.toLowerCase().endsWith("." + EXTENSION_TXT))
		{
			return new CSVReader(reader);
		}

		if (fileName.toLowerCase().endsWith("." + EXTENSION_TSV))
		{
			return new CSVReader(reader, '\t');
		}

		throw new MolgenisDataException("Unknown file type: [" + fileName + "] for csv repository");
	}

	private Map<String, Integer> toColNamesMap(String[] headers)
	{
		if ((headers == null) || (headers.length == 0)) return Collections.emptyMap();

		int capacity = (int) (headers.length / 0.75) + 1;
		Map<String, Integer> columnIdx = new LinkedHashMap<String, Integer>(capacity);
		for (int i = 0; i < headers.length; ++i)
		{
			String header = processCell(headers[i], true);
			columnIdx.put(header, i);
		}

		return columnIdx;
	}

	private String processCell(String value, boolean isHeader)
	{
		return AbstractCellProcessor.processCell(value, isHeader, cellProcessors);
	}

}
