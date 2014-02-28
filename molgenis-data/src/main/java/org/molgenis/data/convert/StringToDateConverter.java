package org.molgenis.data.convert;

import java.text.ParseException;
import java.util.Date;

import org.molgenis.util.MolgenisDateFormat;
import org.springframework.core.convert.converter.Converter;

public class StringToDateConverter implements Converter<String, Date>
{
	@Override
	public Date convert(String source)
	{
		try
		{
			return MolgenisDateFormat.getDateTimeFormat().parse(source);
		}
		catch (ParseException pe)
		{
			throw new IllegalArgumentException("Invalid dateformat [" + source + "] should be of format "
					+ MolgenisDateFormat.DATEFORMAT_DATETIME);
		}
	}
}
