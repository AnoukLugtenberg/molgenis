package org.molgenis.oneclickimporter.service.Impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.oneclickimporter.model.Column;
import org.molgenis.oneclickimporter.model.DataCollection;
import org.molgenis.oneclickimporter.service.OneClickImporterService;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static org.molgenis.data.meta.AttributeType.*;
import static org.molgenis.data.meta.AttributeType.STRING;

@Component
public class OneClickImporterServiceImpl implements OneClickImporterService
{
	@Override
	public DataCollection buildDataCollection(String dataCollectionName, Sheet sheet)
	{
		Row headerRow = sheet.getRow(0);
		List<Column> columns = newArrayList();
		headerRow.cellIterator().forEachRemaining(cell -> columns.add(createColumnFromCell(sheet, cell)));

		return DataCollection.create(dataCollectionName, columns);
	}

	@Override
	public AttributeType guessAttributeType(List<Object> dataValues)
	{

		boolean guesCompleted = false;
		int rowCount = dataValues.size();
		int currentRowIndex = 1;
		AttributeType guess = getBasicAttributeType(dataValues.get(0));
		while (currentRowIndex < rowCount && !guesCompleted) {
			Object value = dataValues.get(currentRowIndex);
			currentRowIndex++;
			AttributeType basicType = getBasicAttributeType(value);
			// todo enrich type
			guess = getCommendType(guess, basicType);

			if(guess.equals(AttributeType.STRING)){
				guesCompleted = true;
			}
		}

		return guess;
	}

	private AttributeType getCommendType(AttributeType thisType, AttributeType thatType){

		if(thisType.equals(thatType)){
			return thisType;
		}

		if(thisType.equals(AttributeType.INT) && thatType.equals(AttributeType.DECIMAL)){
			return AttributeType.DECIMAL;
		}

		if(thisType.equals(AttributeType.INT) && thatType.equals(AttributeType.LONG)){
			return AttributeType.LONG;
		}

		if(thisType.equals(AttributeType.INT) && thatType.equals(AttributeType.BOOL)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.INT) && thatType.equals(AttributeType.STRING)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.DECIMAL) && thatType.equals(AttributeType.INT)){
			return AttributeType.DECIMAL;
		}

		if(thisType.equals(AttributeType.DECIMAL) && thatType.equals(AttributeType.LONG)){
			return AttributeType.DECIMAL;
		}

		if(thisType.equals(AttributeType.DECIMAL) && thatType.equals(AttributeType.BOOL)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.DECIMAL) && thatType.equals(AttributeType.STRING)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.LONG) && thatType.equals(AttributeType.INT)){
			return AttributeType.LONG;
		}

		if(thisType.equals(AttributeType.LONG) && thatType.equals(AttributeType.DECIMAL)){
			return AttributeType.DECIMAL;
		}

		if(thisType.equals(AttributeType.LONG) && thatType.equals(AttributeType.BOOL)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.LONG) && thatType.equals(AttributeType.STRING)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.BOOL) && thatType.equals(AttributeType.INT)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.BOOL) && thatType.equals(AttributeType.DECIMAL)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.BOOL) && thatType.equals(AttributeType.LONG)){
			return AttributeType.STRING;
		}

		if(thisType.equals(AttributeType.BOOL) && thatType.equals(AttributeType.STRING)){
			return AttributeType.STRING;
		}


		return AttributeType.STRING;
	}

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

	private Column createColumnFromCell(Sheet sheet, Cell cell)
	{
		return Column.create(cell.getStringCellValue(), cell.getColumnIndex(),
				getColumnData(sheet, cell.getColumnIndex()));
	}

	private List<Object> getColumnData(Sheet sheet, int columnIndex)
	{
		List<Object> dataValues = newLinkedList();
		sheet.rowIterator().forEachRemaining(row -> dataValues.add(getCellValue(row.getCell(columnIndex))));
		dataValues.remove(0); // Remove the header value
		return dataValues;
	}

	private Object getCellValue(Cell cell)
	{
		Object value;

		// Empty cells are null, instead of BLANK
		if (cell == null)
		{
			return null;
		}

		switch (cell.getCellTypeEnum())
		{
			case STRING:
				value = cell.getStringCellValue();
				break;
			case NUMERIC:
				value = cell.getNumericCellValue();
				break;
			case BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
			case FORMULA:
				value = getTypedFormulaValue(cell);
				break;
			default:
				value = null;
				break;
		}
		return value;
	}

	private Object getTypedFormulaValue(Cell cell)
	{
		Object value;
		switch (cell.getCachedFormulaResultTypeEnum())
		{
			case STRING:
				value = cell.getStringCellValue();
				break;
			case NUMERIC:
				value = cell.getNumericCellValue();
				break;
			case BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
			case BLANK:
				value = null;
				break;
			case ERROR:
				value = "#ERROR";
				break;
			default:
				value = null;
				break;
		}
		return value;
	}
}
