package org.molgenis.data.index;

import org.molgenis.data.MolgenisDataException;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class UnknownIndexException extends MolgenisDataException
{
	private static final long serialVersionUID = 1L;

	public UnknownIndexException(String indexName)
	{
		super(String.format("Index '%s' not found.", indexName));
	}

	public UnknownIndexException(String[] indexNames)
	{
		super(String.format("One or more indexes '%s' not found.", stream(indexNames).collect(joining(", "))));
	}
}
