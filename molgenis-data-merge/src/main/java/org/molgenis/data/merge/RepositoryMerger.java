package org.molgenis.data.merge;

import org.molgenis.MolgenisFieldTypes;

import org.molgenis.data.CrudRepository;
import org.molgenis.data.Repository;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.AbstractEntity;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by charbonb on 01/09/14.
 */
@Component
public class RepositoryMerger {

    private final static String ID = "ID";
    private DataService dataService;

    @Autowired
    public RepositoryMerger(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Create a new merged repository
     * Metadata is merged based on the common attributes (those remain at root level)
     * All non-common level attributes are organised in 1 compound attribute per repository
     * Data of all repositories is merged based on the common columns
     *
     * @param repositoryList list of repositories to be merged
     * @param commonAttributes list of common attributes, these columns are use to 'join'/'merge' on
     * @param mergedRepository the resulting repository
     * @return mergedRepository ElasticSearchRepository containing the merged data
     */
    public CrudRepository merge(List<Repository> repositoryList, List<AttributeMetaData> commonAttributes, CrudRepository mergedRepository){
        dataService.addRepository(mergedRepository);
        mergeData(repositoryList, mergedRepository.getEntityMetaData(), (CrudRepository) dataService.getRepositoryByEntityName(mergedRepository.getName()), commonAttributes);

        return mergedRepository;
    }

    /**
     * Merge the data of all repositories based on the common columns
     */
    private void mergeData(List<Repository> originalRepositoriesList, EntityMetaData mergedEntityMetaData, CrudRepository mergedElasticsearchRepository, List<AttributeMetaData> commonAttributes) {
        for(Repository repository : originalRepositoriesList){
            for(Entity entity : repository){
                boolean newEntity = false;
                AbstractEntity mergedEntity = getMergedEntity(mergedElasticsearchRepository, commonAttributes, entity);
                //if no entity for all the common columns exists, create a new one, containing these fields
                if(mergedEntity == null){
                    newEntity = true;
                    mergedEntity = createMergedEntity(commonAttributes, entity);
                }
                //add all data for non common fields
                for(AttributeMetaData attributeMetaData : entity.getEntityMetaData().getAtomicAttributes()){
                    if(!containsIgnoreCase(attributeMetaData.getName(),commonAttributes)){
                        mergedEntity.set(getMergedAttributeName(repository, attributeMetaData.getName()), entity.get(attributeMetaData.getName()));
                    }
                }
                if(newEntity){
                    mergedElasticsearchRepository.add(mergedEntity);
                }
                else{
                    mergedElasticsearchRepository.update(mergedEntity);
                }
            }
        }
    }

    /**
     * create a new entity based on the merged entity metadata
     */
    private AbstractEntity createMergedEntity(List<AttributeMetaData> commonAttributes, Entity entity) {
        AbstractEntity mergedEntity;
        mergedEntity = new MapEntity(new HashMap<String, Object>());
        mergedEntity.set(ID,UUID.randomUUID().toString());
        for(AttributeMetaData attributeMetaData : commonAttributes) {
            mergedEntity.set(attributeMetaData.getName(), entity.get(attributeMetaData.getName()));
        }
        return mergedEntity;
    }

    /**
     * check if an entity for the common attributes already exists and if so, return it
     */
    private AbstractEntity getMergedEntity(CrudRepository mergedElasticsearchRepository, List<AttributeMetaData> commonAttributes, Entity entity) {
        Query findMergedEntityQuery = new QueryImpl();
        for (AttributeMetaData attributeMetaData : commonAttributes)
            findMergedEntityQuery = findMergedEntityQuery.eq(attributeMetaData.getName(), entity.get(attributeMetaData.getName()));
        return (AbstractEntity)mergedElasticsearchRepository.findOne(findMergedEntityQuery);
    }

    /**
     * Create new EntityMetaData with the common attributes at root level,
     * and all other columns in a compound attribute per original repository
     */
    public EntityMetaData mergeMetaData(List<Repository> repositoryList, List<AttributeMetaData> commonAttributes, String outRepositoryName) {
        DefaultEntityMetaData mergedMetaData = new DefaultEntityMetaData(outRepositoryName);
        DefaultAttributeMetaData idAttribute = new DefaultAttributeMetaData(ID, MolgenisFieldTypes.FieldTypeEnum.STRING);
        idAttribute.setIdAttribute(true);
        idAttribute.setVisible(false);
        mergedMetaData.addAttributeMetaData(idAttribute);
        mergedMetaData.setIdAttribute(ID);
        for(AttributeMetaData attributeMetaData : commonAttributes){
            mergedMetaData.addAttributeMetaData(attributeMetaData);
        }
        for(Repository repository : repositoryList){
            mergeRepositoryMetaData(commonAttributes, mergedMetaData, repository);
        }
        return mergedMetaData;
    }

    /**
     * Add a compound attribute for a repository containing all "non-common" attributes
     */
    private void mergeRepositoryMetaData(List<AttributeMetaData> commonAttributes, DefaultEntityMetaData mergedMetaData, Repository repository) {
        EntityMetaData originalRepositoryMetaData = repository.getEntityMetaData();
        DefaultAttributeMetaData repositoryCompoundAttribute = new DefaultAttributeMetaData(repository.getName(), MolgenisFieldTypes.FieldTypeEnum.COMPOUND);
        List<AttributeMetaData> attributeParts = new ArrayList<AttributeMetaData>();
        for(AttributeMetaData originalRepositoryAttributeMetaData : originalRepositoryMetaData.getAttributes()) {
            if(!containsIgnoreCase(originalRepositoryAttributeMetaData.getName(), commonAttributes)&&!originalRepositoryAttributeMetaData.getName().equalsIgnoreCase(ID)){
                DefaultAttributeMetaData attributePartMetaData = new DefaultAttributeMetaData(getMergedAttributeName(repository, originalRepositoryAttributeMetaData.getName()), originalRepositoryAttributeMetaData.getDataType().getEnumType());
                attributePartMetaData.setLabel(getMergedAttributeLabel(repository, originalRepositoryAttributeMetaData.getLabel()));
                if(originalRepositoryAttributeMetaData.getDataType().getEnumType().equals(MolgenisFieldTypes.FieldTypeEnum.COMPOUND)){
                    addCompoundAttributeParts(repository, originalRepositoryAttributeMetaData, attributePartMetaData);
                }
                attributeParts.add(attributePartMetaData);
            }
        }
        repositoryCompoundAttribute.setAttributesMetaData(attributeParts);
        mergedMetaData.addAttributeMetaData(repositoryCompoundAttribute);
    }

    /**
     * Recursively add all the attributes in an compound attribute
     */
    private void addCompoundAttributeParts(Repository repository, AttributeMetaData originalRepositoryAttributeMetaData, DefaultAttributeMetaData attributePartMetaData) {
        List<AttributeMetaData> subAttributeParts = new ArrayList<AttributeMetaData>();
        for(AttributeMetaData originalRepositorySubAttributeMetaData : originalRepositoryAttributeMetaData.getAttributeParts()) {
            DefaultAttributeMetaData subAttributePartMetaData = new DefaultAttributeMetaData(getMergedAttributeName(repository, originalRepositorySubAttributeMetaData.getName()), originalRepositorySubAttributeMetaData.getDataType().getEnumType());
            subAttributePartMetaData.setLabel(getMergedAttributeLabel(repository, originalRepositorySubAttributeMetaData.getLabel()));
            if(subAttributePartMetaData.getDataType().getEnumType().equals(MolgenisFieldTypes.FieldTypeEnum.COMPOUND)){
                addCompoundAttributeParts(repository, originalRepositorySubAttributeMetaData, subAttributePartMetaData);
            }
            subAttributeParts.add(subAttributePartMetaData);
        }
        attributePartMetaData.setAttributesMetaData(subAttributeParts);
    }

    /**
     * Check if an specific attributename is present in a list of AttributeMetadata
     */
    private boolean containsIgnoreCase(String input, List<AttributeMetaData> list) {
        for (AttributeMetaData attributeMetaData : list) {
            if (input.equalsIgnoreCase(attributeMetaData.getName())) return true;
        }
        return false;
    }

    /**
     * Create a name for an attribute based on the attribute name in the original repository
     * and the original repository name itself.
     */
    private String getMergedAttributeName(Repository repository, String attributeName) {
        return repository.getName()+"_"+attributeName;
    }

    /**
     * Create a label for an attribute based on the attribute label in the original repository
     * and the original repository name itself.
     */
    private String getMergedAttributeLabel(Repository repository, String attributeLabel) {
        return attributeLabel+"("+repository.getName()+")";
    }
}
