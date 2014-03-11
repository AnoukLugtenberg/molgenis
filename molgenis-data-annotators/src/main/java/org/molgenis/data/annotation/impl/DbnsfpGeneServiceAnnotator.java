package org.molgenis.data.annotation.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.annotation.LocusAnnotator;
import org.molgenis.data.annotation.AnnotationService;
import org.molgenis.data.annotation.impl.datastructures.HGNCLoc;
import org.molgenis.data.annotation.impl.datastructures.Locus;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.framework.server.MolgenisSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * <p>
 * This class uses a location to retrieve a gene from the ... class. This gene can then be used to approach the dbNSFP
 * gene file. It extracts relevant gene annotations from this file. This can be run seperately from the dbNSFP variant
 * service.
 * </p>
 * 
 * <p>
 * <b>dbNSFP gene returns:</b>
 * 
 * Gene_name Ensembl_gene chr Gene_old_names Gene_other_names Uniprot_acc Uniprot_id Entrez_gene_id CCDS_id Refseq_id
 * ucsc_id MIM_id Gene_full_name Pathway(Uniprot) Pathway(ConsensusPathDB) Function_description Disease_description
 * MIM_phenotype_id MIM_disease Trait_association(GWAS) GO_Slim_biological_process GO_Slim_cellular_component
 * GO_Slim_molecular_function Expression(egenetics) Expression(GNF/Atlas) Interactions(IntAct) Interactions(BioGRID)
 * Interactions(ConsensusPathDB) P(HI) P(rec) Known_rec_info Essential_gene
 * </p>
 * 
 * @author mdehaan
 * 
 * @version dbNSFP version 2.3 downloaded January 26, 2014
 * 
 * */
@Component("dbnsfpGeneService")
public class DbnsfpGeneServiceAnnotator extends LocusAnnotator
{
	@Autowired
	private MolgenisSettings molgenisSettings;

	@Autowired
	AnnotationService annotatorService;

	private static final String NAME = "dbNSFP-Gene";
	public static final String GENE_FILE_LOCATION_PROPERTY = "dbnsfp_gene_location";

	private static final String GENE_NAME = "Gene_name";
	private static final String ENSEMBL_GENE = "Ensembl_gene";
	private static final String CHR = "chr";
	private static final String GENE_OLD_NAMES = "Gene_old_names";
	private static final String GENE_OTHER_NAMES = "Gene_other_names";
	private static final String UNIPROT_ACC = "Uniprot_acc";
	private static final String UNIPROT_ID = "Uniprot_id";
	private static final String ENTREZ_GENE_ID = "Entrez_gene_id";
	private static final String CCDS_ID = "CCDS_id";
	private static final String REFSEQ_ID = "Refseq_id";
	private static final String UCSC_ID = "ucsc_id";
	private static final String MIM_ID = "MIM_id";
	private static final String GENE_FULL_NAME = "Gene_full_name";
	private static final String PATHWAY_UNIPROT = "Pathway(Uniprot)";
	private static final String PATHWAY_CONSENSUSPATHDB = "Pathway(ConsensusPathDB)";
	private static final String FUNCTION_DESCRIPTION = "Function_description";
	private static final String DISEASE_DESCRIPTION = "Disease_description";
	private static final String MIM_PHENOTYPE_ID = "MIM_phenotype_id";
	private static final String MIM_DISEASE = "MIM_disease";
	private static final String TRAIT_ASSOCIATION_GWAS = "Trait_association(GWAS)";
	private static final String GO_SLIM_BIOLOGICAL_PROCESS = "GO_Slim_biological_process";
	private static final String GO_SLIM_CELLULAR_COMPONENT = "GO_Slim_cellular_component";
	private static final String GO_SLIM_MOLECULAR_FUNCTION = "GO_Slim_molecular_function";
	private static final String EXPRESSION_EGENETICS = "Expression(egenetics)";
	private static final String EXPRESSION_GNF_ATLAS = "Expression(GNF/Atlas)";
	private static final String INTERACTIONS_INTACT = "Interactions(IntAct)";
	private static final String INTERACTIONS_BIOGRID = "Interactions(BioGRID)";
	private static final String INTERACTIONS_CONSENSUSPATHDB = "Interactions(ConsensusPathDB)";
	private static final String P_HI = "P(HI)";
	private static final String P_REC = "P(rec)";
	private static final String KNOWN_REC_INFO = "Known_rec_info";
	private static final String ESSENTIAL_GENE = "Essential_gene";

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		annotatorService.addAnnotator(this);
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public List<Entity> annotateEntity(Entity entity)
	{
		List<Entity> results = new ArrayList<Entity>();
		BufferedReader bufferedReader = null;

		try
		{
			FileReader reader = new FileReader(new File(molgenisSettings.getProperty(GENE_FILE_LOCATION_PROPERTY)));
			bufferedReader = new BufferedReader(reader);

			HashMap<String, HGNCLoc> hgncLocs = OmimHpoAnnotator.getHgncLocs();

			Long position = entity.getLong(POSITION);
			String chromosome = entity.getString(CHROMOSOME);

			List<Locus> locus = new ArrayList<Locus>(Arrays.asList(new Locus(chromosome, position)));
			List<String> geneSymbols = OmimHpoAnnotator.locationToHGNC(hgncLocs, locus);

			if (geneSymbols != null)
			{
				while (bufferedReader.ready())
				{
					String line = bufferedReader.readLine();
					String[] lineSplit = line.split("\t");

					for (String gene : geneSymbols)
					{
						if (lineSplit[0].equals(gene))
						{
							int lineSplitIndex = 0;
							HashMap<String, Object> resultMap = new HashMap<String, Object>();

							for (AttributeMetaData feature : getOutputMetaData().getAtomicAttributes())
							{
								if (feature != null)
								{
									resultMap.put(feature.getName(), lineSplit[lineSplitIndex]);
									lineSplitIndex = lineSplitIndex + 1;
								}

							}

							resultMap.put(CHROMOSOME, chromosome);
							resultMap.put(POSITION, position);

							results.add(new MapEntity(resultMap));
						}
					}
				}
			}
		}

		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			try
			{
				bufferedReader.close();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		return results;
	}

	@Override
	public EntityMetaData getOutputMetaData()
	{
		DefaultEntityMetaData metadata = new DefaultEntityMetaData(this.getClass().getName());

		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GENE_NAME, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(ENSEMBL_GENE, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(CHR, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GENE_OLD_NAMES, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GENE_OTHER_NAMES, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(UNIPROT_ACC, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(UNIPROT_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(ENTREZ_GENE_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(CCDS_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(REFSEQ_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(UCSC_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(MIM_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GENE_FULL_NAME, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(PATHWAY_UNIPROT, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(PATHWAY_CONSENSUSPATHDB, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(FUNCTION_DESCRIPTION, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(DISEASE_DESCRIPTION, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(MIM_PHENOTYPE_ID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(MIM_DISEASE, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(TRAIT_ASSOCIATION_GWAS, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GO_SLIM_BIOLOGICAL_PROCESS, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GO_SLIM_CELLULAR_COMPONENT, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(GO_SLIM_MOLECULAR_FUNCTION, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(EXPRESSION_EGENETICS, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(EXPRESSION_GNF_ATLAS, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(INTERACTIONS_INTACT, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(INTERACTIONS_BIOGRID, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(INTERACTIONS_CONSENSUSPATHDB, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(P_HI, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(P_REC, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(KNOWN_REC_INFO, FieldTypeEnum.TEXT));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(ESSENTIAL_GENE, FieldTypeEnum.TEXT));

		return metadata;
	}
}
