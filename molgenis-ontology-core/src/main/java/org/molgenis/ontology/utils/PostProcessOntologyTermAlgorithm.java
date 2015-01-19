package org.molgenis.ontology.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.ontology.beans.ComparableEntity;
import org.molgenis.ontology.repository.OntologyTermQueryRepository;
import org.molgenis.ontology.service.OntologyServiceImpl;

/**
 * An algorithm container to deal with the case where matching term is mapped to multiple synonyms of the same ontology
 * term at the same time leading to a low final score. E.g. protruding eyeball (exophthalmos, proptosis) mapped to
 * 'protruding eye' by 30%, 'exophthalmos' by 30% and 'proptosis' by 26%. For such cases, the score should be
 * re-calculated by combining multiple synonyms together in order to give a fair score
 * 
 * @author chaopang
 *
 */
public class PostProcessOntologyTermAlgorithm
{
	// A map where key indicates which field of the input entity is used in the matching, such as 'name' and 'HP', value
	// is list of ontology terms matched
	private final Map<String, List<ComparableEntity>> matchedFieldToEntity;

	// A map representing the input entity converted to Map data structure
	private final Map<String, Object> inputData;

	public PostProcessOntologyTermAlgorithm(Map<String, Object> inputData)
	{
		matchedFieldToEntity = new HashMap<String, List<ComparableEntity>>();
		this.inputData = inputData;
	}

	public void addOntologyTerm(String matchedField, ComparableEntity entity)
	{
		if (!matchedFieldToEntity.containsKey(matchedField))
		{
			matchedFieldToEntity.put(matchedField, new ArrayList<ComparableEntity>());
		}
		matchedFieldToEntity.get(matchedField).add(entity);
	}

	public void process(List<ComparableEntity> allEntities)
	{
		for (Entry<String, List<ComparableEntity>> entry : matchedFieldToEntity.entrySet())
		{
			// Get input data query
			String inputDataQuery = inputData.get(entry.getKey()).toString();
			// Get a list of mapped candidate ontology terms
			List<ComparableEntity> synonymousEntities = entry.getValue();
			if (synonymousEntities.size() > 0)
			{
				StringBuilder combinedSynonym = new StringBuilder();
				ComparableEntity firstEntity = synonymousEntities.get(0);
				String firstSynonym = firstEntity.getString(OntologyTermQueryRepository.SYNONYMS);
				Double firstScore = Double.parseDouble(firstEntity.get(OntologyServiceImpl.SCORE).toString());
				combinedSynonym.append(firstSynonym);
				for (int i = 1; i < synonymousEntities.size(); i++)
				{
					ComparableEntity nextEntity = synonymousEntities.get(i);
					String nextEntitySynonym = nextEntity.getString(OntologyTermQueryRepository.SYNONYMS);
					if (!combinedSynonym.toString().contains(nextEntitySynonym))
					{
						StringBuilder tempCombinedSynonym = new StringBuilder().append(combinedSynonym)
								.append(OntologyTermQueryRepository.SINGLE_WHITESPACE).append(nextEntitySynonym);

						Double newScore = NGramMatchingModel.stringMatching(
								inputDataQuery.replaceAll(OntologyTermQueryRepository.ILLEGAL_CHARACTERS_PATTERN,
										OntologyTermQueryRepository.SINGLE_WHITESPACE),
								tempCombinedSynonym.toString().replaceAll(
										OntologyTermQueryRepository.ILLEGAL_CHARACTERS_PATTERN,
										OntologyTermQueryRepository.SINGLE_WHITESPACE));
						if (newScore.intValue() > firstScore.intValue())
						{
							firstScore = newScore;
							firstEntity.set(OntologyServiceImpl.SCORE, newScore);
							combinedSynonym.delete(0, combinedSynonym.length()).append(tempCombinedSynonym);
						}
						allEntities.remove(nextEntity);
					}
				}
			}
		}
	}
}