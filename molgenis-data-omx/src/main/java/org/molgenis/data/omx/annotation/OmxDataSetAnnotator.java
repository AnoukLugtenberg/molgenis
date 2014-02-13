package org.molgenis.data.omx.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.CrudRepository;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryAnnotator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.ObservationSet;
import org.molgenis.omx.observ.ObservedValue;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.value.StringValue;
import org.molgenis.omx.search.DataSetsIndexer;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * This class uses a repository and calls an annotation service to add features and values to an existing data set.
 * </p>
 * 
 * @author mdehaan
 * 
 * */
public class OmxDataSetAnnotator
{
    //FIXME unit test this class!
	private static final String PROTOCOL_SUFFIX = "_annotator_id";
	DataService dataService;
	DataSetsIndexer indexer;

	/**
	 * @param dataService
	 * @param indexer
	 * 
	 * */
	public OmxDataSetAnnotator(DataService dataService, DataSetsIndexer indexer)
	{
		this.dataService = dataService;
		this.indexer = indexer;
	}

	/**
	 * <p>
	 * This method calls an annotation service and creates an entity of annotations based on one or more columns. Input
	 * and output meta data is collected for the creating Observable features. The protocol holding the added features
	 * from the annotator and the annotation values are then added to the data set of the supplied repository.
	 * </p>
	 * <p>
	 * The resulting annotated data set is then indexed so users or admins do not have to re-index manually.
	 * </p>
	 * 
	 * @param annotator
	 * @param repo
	 * @param createCopy
	 * 
	 * */
	@Transactional
	public void annotate(RepositoryAnnotator annotator, Repository repo, boolean createCopy)
	{

		Iterator<Entity> entityIterator = annotator.annotate(repo.iterator());

		List<String> inputMetadataNames = getMetadataNamesAsList(annotator.getInputMetaData());
		DataSet dataSet = dataService.findOne(DataSet.ENTITY_NAME,
				new QueryImpl().eq(DataSet.IDENTIFIER, repo.getName()), DataSet.class);
		if (createCopy)
		{
			dataSet = copy(dataSet);
		}
		Protocol resultProtocol = dataService.findOne(Protocol.ENTITY_NAME,
				new QueryImpl().eq(Protocol.IDENTIFIER, annotator.getName() + PROTOCOL_SUFFIX), Protocol.class);
		if (resultProtocol == null)
		{
			resultProtocol = createAnnotationResultProtocol(annotator, dataSet, annotator.getOutputMetaData().getAttributes());
		}
		if (!dataSet.getProtocolUsed().getSubprotocols().contains(resultProtocol))
		{
			addAnnotationResultProtocol(dataSet, resultProtocol);
		}
		addAnnotationResults(inputMetadataNames, getMetadataNamesAsList(annotator.getOutputMetaData()), dataSet, entityIterator, annotator);

		indexResultDataSet(dataSet);
	}

	private void addAnnotationResults(List<String> inputMetadataNames, List<String> outputMetadataNames,
			DataSet dataSet, Iterator<Entity> entityIterator, RepositoryAnnotator annotator)
	{
		Iterable<ObservationSet> osSet = dataService.findAll(ObservationSet.ENTITY_NAME,
				new QueryImpl().eq(ObservationSet.PARTOFDATASET, dataSet), ObservationSet.class);

		CrudRepository valueRepo = (CrudRepository) dataService.getRepositoryByEntityName(ObservedValue.ENTITY_NAME);
		Map<String, Object> inputValues;
		while (entityIterator.hasNext())
		{
			Entity entity = entityIterator.next();
			for (ObservationSet os : osSet)
			{
				inputValues = createInputValueMap(inputMetadataNames, valueRepo, os);

				if (entityEqualsObservationSet(entity, inputValues, inputMetadataNames))
				{
					for (String columnName : outputMetadataNames)
					{
						addValue(entity, os, columnName, annotator.getName());
					}
				}
			}
		}
	}

	private void indexResultDataSet(DataSet dataSet)
	{
		ArrayList<Integer> datasetIds = new ArrayList<Integer>();
		datasetIds.add(dataSet.getId());
		indexer.indexDataSets(datasetIds);
	}

	private Protocol createAnnotationResultProtocol(RepositoryAnnotator annotator, DataSet dataSet,
			Iterable<AttributeMetaData> outputMetadataNames)
	{

		Protocol resultProtocol = new Protocol();
		resultProtocol.setIdentifier(annotator.getName() + PROTOCOL_SUFFIX);
		resultProtocol.setName(annotator.getName());
		dataService.add(Protocol.ENTITY_NAME, resultProtocol);
		addOutputFeatures(resultProtocol, outputMetadataNames, annotator.getName());
		return resultProtocol;
	}

	private void addAnnotationResultProtocol(DataSet dataSet, Protocol resultProtocol)
	{
		Protocol rootProtocol = dataService.findOne(Protocol.ENTITY_NAME,
				new QueryImpl().eq(Protocol.IDENTIFIER, dataSet.getProtocolUsed().getIdentifier()), Protocol.class);

		rootProtocol.getSubprotocols().add(resultProtocol);
		dataService.update(Protocol.ENTITY_NAME, rootProtocol);
	}

	private List<String> getMetadataNamesAsList(EntityMetaData metadata)
	{
		Iterator<AttributeMetaData> metadataIterator = metadata.getAttributes().iterator();

		List<String> inputFeatureNames = new ArrayList<String>();
		while (metadataIterator.hasNext())
		{
			AttributeMetaData attributeMetaData = metadataIterator.next();
			inputFeatureNames.add(attributeMetaData.getName());
		}
		return inputFeatureNames;
	}

	private void addOutputFeatures(Protocol resultProtocol, Iterable<AttributeMetaData> metaData, String prefix)
	{
		for (AttributeMetaData attributeMetaData: metaData)
		{
			ObservableFeature newFeature = new ObservableFeature();
			if (dataService.findOne(ObservableFeature.ENTITY_NAME,
					new QueryImpl().eq(ObservableFeature.IDENTIFIER, attributeMetaData.getName())) == null)
			{
				newFeature.setIdentifier(prefix + attributeMetaData.getName());
				newFeature.setName(attributeMetaData.getLabel());
                newFeature.setDataType(attributeMetaData.getDataType().toString());
				dataService.add(ObservableFeature.ENTITY_NAME, newFeature);

				resultProtocol.getFeatures().add(newFeature);
			}
			dataService.update(Protocol.ENTITY_NAME, resultProtocol);
		}
	}

	private void addValue(Entity entity, ObservationSet os, String columnName, String prefix)
	{
		StringValue sv = new StringValue();
		sv.setValue(entity.get(columnName).toString());
		dataService.add(StringValue.ENTITY_NAME, sv);

		ObservedValue ov = new ObservedValue();

		ObservableFeature thisFeature = dataService.findOne(ObservableFeature.ENTITY_NAME,
				new QueryImpl().eq(ObservableFeature.IDENTIFIER, prefix + columnName), ObservableFeature.class);

		ov.setFeature(thisFeature);
		ov.setObservationSet(os);
		ov.setValue(sv);
		dataService.add(ObservedValue.ENTITY_NAME, ov);
	}

	private Map<String, Object> createInputValueMap(List<String> inputFeatureNames, CrudRepository valueRepo,
			ObservationSet os)
	{
		Map<String, Object> inputValueMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
		for (String inputFeatureName : inputFeatureNames)
		{
			ObservableFeature inputFeature = dataService.findOne(ObservableFeature.ENTITY_NAME,
                    new QueryImpl().eq(ObservableFeature.IDENTIFIER, inputFeatureName), ObservableFeature.class);

			// retrieve a value from this observation set based on a specified feature
			ObservedValue value = valueRepo.findOne(
					new QueryImpl().eq(ObservedValue.OBSERVATIONSET, os).eq(ObservedValue.FEATURE, inputFeature),
					ObservedValue.class);
			String inputValue = value.getValue().getString("value");
			inputValueMap.put(inputFeature.getIdentifier(), inputValue);
		}
		return inputValueMap;
	}

	private boolean entityEqualsObservationSet(Entity entity, Map<String, Object> inputValues,
			List<String> inputFeatureNames)
	{
		boolean areEqual = true;
		for (String inputFeatureName : inputFeatureNames)
		{
			if (!entity.get(inputFeatureName).equals(inputValues.get(inputFeatureName)))
			{
				areEqual = false;
				break;
			}
		}
		return areEqual;
	}

	@Transactional
	public DataSet copy(DataSet original)
	{
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy_HH:mm");
		String dateString = sdf.format(date);

		List<ObservationSet> newObservationSets = new ArrayList<ObservationSet>();
		List<ObservedValue> newObservedValues = new ArrayList<ObservedValue>();

		Protocol newRootProtocol = new Protocol();
		newRootProtocol.setIdentifier(original.getProtocolUsed().getIdentifier() + cal.getTimeInMillis());
		newRootProtocol.setName(original.getName() + "_results_" + dateString);

		List subprotocols = new ArrayList<Protocol>();
		subprotocols.add(original.getProtocolUsed());
		newRootProtocol.setSubprotocols(subprotocols);
		dataService.add(Protocol.ENTITY_NAME, newRootProtocol);

		DataSet copy = new DataSet();
		copy.setProtocolUsed(newRootProtocol);
		copy.setName(original.getName() + "_results_" + dateString);
		copy.setIdentifier(original.getIdentifier() + cal.getTimeInMillis());
		copy.setStartTime(date);
		copy.setEndTime(date);

		Iterable<ObservationSet> observationSets = dataService.findAll(ObservationSet.ENTITY_NAME,
				new QueryImpl().eq(ObservationSet.PARTOFDATASET, original), ObservationSet.class);
		dataService.add(DataSet.ENTITY_NAME, copy);
		for (ObservationSet observationSet : observationSets)
		{
			Iterable<ObservedValue> observedValues = dataService.findAll(ObservedValue.ENTITY_NAME,
					new QueryImpl().eq(ObservedValue.OBSERVATIONSET, observationSet), ObservedValue.class);
			observationSet.setPartOfDataSet(copy);
			observationSet.setIdentifier(observationSet.getIdentifier() + new Date().getTime());
			observationSet.setId(null);
			newObservationSets.add(observationSet);
			for (ObservedValue observedValue : observedValues)
			{
				observedValue.setObservationSet(observationSet);
				observedValue.setId(null);
				newObservedValues.add(observedValue);
			}
		}

		dataService.add(ObservationSet.ENTITY_NAME, newObservationSets);
		dataService.add(ObservedValue.ENTITY_NAME, newObservedValues);

		return copy;
	}
}