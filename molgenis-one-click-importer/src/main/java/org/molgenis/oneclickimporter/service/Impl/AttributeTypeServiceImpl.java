package org.molgenis.oneclickimporter.service.Impl;

import org.molgenis.data.meta.AttributeType;
import org.molgenis.oneclickimporter.service.AttributeTypeService;
import org.molgenis.util.MolgenisDateFormat;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.molgenis.data.meta.AttributeType.*;

@Component
public class AttributeTypeServiceImpl implements AttributeTypeService
{
	private static final int MAX_STRING_LENGTH = 255;

	@Override
	public AttributeType guessAttributeType(List<Object> dataValues)
	{
		boolean guessCompleted = false;
		int rowCount = dataValues.size();
		int currentRowIndex = 0;

		AttributeType guess = getBasicAttributeType(dataValues.get(0));
		while (currentRowIndex < rowCount && !guessCompleted)
		{
			Object value = dataValues.get(currentRowIndex);
			if (value == null)
			{
				break;
			}

			AttributeType basicType = getBasicAttributeType(value);

			guess = getCommonType(guess, basicType);
			guess = getEnrichedType(guess, value);

			if (guess.equals(STRING) || guess.equals(TEXT))
			{
				guessCompleted = true;
			}
			currentRowIndex++;
		}

		return guess;
	}

	/**
	 * Returns an enriched AttributeType for when the value meets certain criteria
	 * i.e. if a string value is longer dan 255 characters, the type should be TEXT
	 */
	private AttributeType getEnrichedType(AttributeType guess, Object value)
	{
		if (guess.equals(STRING))
		{
			String stringValue = value.toString();
			if (stringValue.length() > MAX_STRING_LENGTH)
			{
				return TEXT;
			}

			try
			{
				// If parseInstant() succeeds, return DATE
				MolgenisDateFormat.parseInstant(stringValue);
				return DATE;
			}
			catch (DateTimeParseException e)
			{
				return guess;
			}
		}
		return guess;
	}

	/**
	 * Returns the AttributeType shared by both types
	 */
	private AttributeType getCommonType(AttributeType existingGuess, AttributeType newGuess)
	{
		if (existingGuess.equals(newGuess))
		{
			return existingGuess;
		}

		switch (existingGuess)
		{
			case INT:
				//noinspection Duplicates
				if (newGuess.equals(DECIMAL))
				{
					return DECIMAL;
				}
				else if (newGuess.equals(LONG))
				{
					return LONG;
				}
				else
				{
					return STRING;
				}
			case DECIMAL:
				if (newGuess.equals(INT) || newGuess.equals(LONG))
				{
					return DECIMAL;
				}
				else
				{
					return STRING;
				}
			case LONG:
				//noinspection Duplicates
				if (newGuess.equals(INT))
				{
					return LONG;
				}
				else if (newGuess.equals(DECIMAL))
				{
					return DECIMAL;
				}
				else
				{
					return STRING;
				}
			case BOOL:
				if (!newGuess.equals(BOOL))
				{
					return STRING;
				}
			case DATE:
				if (!newGuess.equals(DATE))
				{
					return STRING;
				}
			default:
				return STRING;
		}
	}

	/**
	 * Sets the basic type based on instance of the value Object
	 */
	private AttributeType getBasicAttributeType(Object value)
	{
		if (value == null)
		{
			return STRING;
		}
		if (value instanceof Integer)
		{
			return INT;
		}
		else if (value instanceof Double || value instanceof Float)
		{
			return DECIMAL;
		}
		else if (value instanceof Long)
		{
			return LONG;
		}
		else if (value instanceof Boolean)
		{
			return BOOL;
		}
		else
		{
			return STRING;
		}
	}
}
