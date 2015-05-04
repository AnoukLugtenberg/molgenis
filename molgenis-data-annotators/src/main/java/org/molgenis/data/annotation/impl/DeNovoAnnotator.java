package org.molgenis.data.annotation.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.annotation.AnnotationService;
import org.molgenis.data.annotation.utils.AnnotatorUtils;
import org.molgenis.data.annotation.VariantAnnotator;
import org.molgenis.data.annotation.utils.VcfUtils;
import org.molgenis.data.annotation.impl.datastructures.Sample;
import org.molgenis.data.annotation.impl.datastructures.Trio;
import org.molgenis.data.support.AnnotationServiceImpl;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.vcf.VcfRepository;
import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.framework.server.MolgenisSimpleSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 
 * De novo variant annotator Uses trio data (child-mother-father), read from VCF pedigree data See:
 * http://samtools.github.io/hts-specs/VCFv4.2.pdf
 * 
 *
 **/
@Component("deNovoService")
public class DeNovoAnnotator extends VariantAnnotator
{
	private static final Logger LOG = LoggerFactory.getLogger(DeNovoAnnotator.class);

	private final MolgenisSettings molgenisSettings;
	private final AnnotationService annotatorService;
	private HashMap<String, Trio> pedigree;

	// helper lists for ease of use, derived from HashMap<String, Parents> pedigree
	private HashMap<String, Sample> motherToChild;
	private HashMap<String, Sample> fatherToChild;

	// need: matrix of gene x trio, with nr of denovo calls in each cell
	// <geneSymbol - <trioChildID - count>>
	LinkedHashMap<String, LinkedHashMap<String, Integer>> geneTrioCounts = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();

	public static final String DENOVO_LABEL = "DENOVO";
	public static final String DENOVO = VcfRepository.getInfoPrefix() + DENOVO_LABEL;

	private static final String NAME = "DENOVO";

	final List<String> infoFields = Arrays.asList(new String[]
	{ "##INFO=<ID=" + DENOVO.substring(VcfRepository.getInfoPrefix().length())
			+ ",Number=1,Type=String,Description=\"todo\">" });

	@Autowired
	public DeNovoAnnotator(MolgenisSettings molgenisSettings, AnnotationService annotatorService) throws IOException
	{
		this.molgenisSettings = molgenisSettings;
		this.annotatorService = annotatorService;
	}

	public DeNovoAnnotator(File exacFileLocation, File inputVcfFile, File outputVCFFile) throws Exception
	{
		// cast to a real logger to adjust to the log level
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LOG;
		root.setLevel(ch.qos.logback.classic.Level.ERROR);

		this.molgenisSettings = new MolgenisSimpleSettings();
		this.annotatorService = new AnnotationServiceImpl();

		PrintWriter outputVCFWriter = new PrintWriter(outputVCFFile, "UTF-8");

		VcfRepository vcfRepo = new VcfRepository(inputVcfFile, this.getClass().getName());
		Iterator<Entity> vcfIter = vcfRepo.iterator();

		VcfUtils.checkPreviouslyAnnotatedAndAddMetadata(inputVcfFile, outputVCFWriter, infoFields,
				DENOVO.substring(VcfRepository.getInfoPrefix().length()));

		pedigree = VcfUtils.getPedigree(inputVcfFile);
		motherToChild = new HashMap<String, Sample>();
		fatherToChild = new HashMap<String, Sample>();
		for (String key : pedigree.keySet())
		{
			Sample mom = pedigree.get(key).getMother();
			Sample dad = pedigree.get(key).getFather();
			motherToChild.put(mom.getId(), pedigree.get(key).getChild());
			fatherToChild.put(dad.getId(), pedigree.get(key).getChild());
		}

		for (String key : pedigree.keySet())
		{
			System.out.println(key + ", " + pedigree.get(key));
		}

		LOG.info("Now starting to process the data.");

		while (vcfIter.hasNext())
		{
			Entity record = vcfIter.next();

			List<Entity> annotatedRecord = annotateEntity(record);

			if (annotatedRecord.size() > 1)
			{
				outputVCFWriter.close();
				vcfRepo.close();
				throw new Exception("Multiple outputs for " + record.toString());
			}
			else if (annotatedRecord.size() == 0)
			{
				outputVCFWriter.println(VcfUtils.convertToVCF(record));
			}
			else
			{
				outputVCFWriter.println(VcfUtils.convertToVCF(annotatedRecord.get(0)));
			}
		}

		for (String child : geneTrioCounts.entrySet().iterator().next().getValue().keySet())
		{
			System.out.print("\t" + child);
		}
		System.out.print("\n");
		for (String gene : geneTrioCounts.keySet())
		{
			// System.out.println("GENE " + gene);
			StringBuilder line = new StringBuilder();
			line.append(gene);
			line.append("\t");
			for (String child : geneTrioCounts.get(gene).keySet())
			{
				// System.out.println("CHILD " + child);
				line.append(geneTrioCounts.get(gene).get(child));
				line.append("\t");
			}
			line.deleteCharAt(line.length() - 1);
			System.out.println(line.toString());
		}

		outputVCFWriter.close();
		vcfRepo.close();
		LOG.info("All done!");
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		annotatorService.addAnnotator(this);
	}

	@Override
	public String getSimpleName()
	{
		return NAME;
	}

	@Override
	public List<Entity> annotateEntity(Entity entity) throws IOException, InterruptedException
	{
		Map<String, Object> resultMap = annotateEntityWithDeNovo(entity);
		return Collections.<Entity> singletonList(AnnotatorUtils.getAnnotatedEntity(this, entity, resultMap));
	}

	private synchronized Map<String, Object> annotateEntityWithDeNovo(Entity entity) throws IOException
	{
		Map<String, Object> resultMap = new HashMap<String, Object>();

		String chrom = entity.get(VcfRepository.CHROM).toString();
		if (chrom.equals("X") || chrom.equals("Y"))
		{
			LOG.info("Skipping allosomal variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}

		// only look at variants that PASS the filter
		String filter = entity.get(VcfRepository.FILTER).toString();
		if (!filter.equals("PASS"))
		{
			LOG.info("Skipping low quality variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}

		// only keep variants with overall high quality
		Double qual = Double.parseDouble(entity.get(VcfRepository.QUAL).toString());
		if (qual < 30)
		{
			LOG.info("Skipping low qual (<30) variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}
		

		/**
		 * AB
		 */
		
		Double ABHet = entity.get("ABHet") != null ? Double.parseDouble(entity.get("ABHet").toString()) : 0;
		if (ABHet < 0.3 || ABHet > 0.5)
		{
			LOG.info("Skipping bad het AB variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}
		
		// only keep variants with overall high quality
		Double ABHom = Double.parseDouble(entity.get("ABHom").toString());
		if (ABHom < 0.5)
		{
			LOG.info("Skipping bad hom AB variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}
		
		/**
		 * Strand bias
		 */
		Double SB = Double.parseDouble(entity.get("SB").toString());
		if (SB > 0.5)
		{
			LOG.info("Skipping bad SB variant: " + entity);
			resultMap.put(DENOVO, 0);
			return resultMap;
		}
		
		

		String geneSymbol = SnpEffServiceAnnotator.getGeneNameFromEntity(entity);

		// disable for now, but useful in some analyses..
		if (false)
		{
			// allele frequency filter
			double thousandGenomesMAF = entity.getDouble(ThousandGenomesServiceAnnotator.THGEN_MAF) != null ? entity
					.getDouble(ThousandGenomesServiceAnnotator.THGEN_MAF) : 0;
			double exacMAF = entity.getDouble(ExACServiceAnnotator.EXAC_MAF) != null ? entity
					.getDouble(ExACServiceAnnotator.EXAC_MAF) : 0;
			double gonlMAF = entity.getDouble(GoNLServiceAnnotator.GONL_MAF) != null ? entity
					.getDouble(GoNLServiceAnnotator.GONL_MAF) : 0;
			if (thousandGenomesMAF > 0.01 || exacMAF > 0.01 || gonlMAF > 0.01)
			{
				LOG.info("Skipping 'common' variant (>1% AF in GoNL/ExAC/1000G): " + entity);
				resultMap.put(DENOVO, 0);
				return resultMap;
			}

			// impact
			String[] annSplit = entity.getString(VcfRepository.getInfoPrefix() + "ANN").split("\\|", -1);
			SnpEffServiceAnnotator.impact impact = SnpEffServiceAnnotator.impact.valueOf(annSplit[2]);
			if (impact.equals(SnpEffServiceAnnotator.impact.MODIFIER)
					|| impact.equals(SnpEffServiceAnnotator.impact.LOW))
			{
				LOG.info("Skipping MODIFIER/LOW impact variant: " + entity);
				resultMap.put(DENOVO, 0);
				return resultMap;
			}
		}

		Iterable<Entity> samples = entity.getEntities("Samples");

		HashMap<String, Trio> childToParentGenotypes = new HashMap<String, Trio>();

		// System.out.println("Variant: " + entity.get(VcfRepository.CHROM) + " " + entity.get(VcfRepository.POS) + " "
		// + entity.get(VcfRepository.REF) + " " + entity.get(VcfRepository.ALT));
		for (Entity sample : samples)
		{
			String sampleID = sample.get("NAME").toString()
					.substring(sample.get("NAME").toString().lastIndexOf("_") + 1);
			// String gt = sample.get("GT") == null ? null : sample.get("GT").toString();

			// System.out.println("SAMPLEID: " + sampleID);
			// System.out.println("GT: " + gt);

			// found a child
			if (pedigree.keySet().contains(sampleID))
			{
				// child not previously added by finding a parent first, add here plus empty trio object
				if (!childToParentGenotypes.containsKey(sampleID))
				{
					// System.out.println("CHILD - NEW TRIO");
					Trio t = new Trio();
					t.setChild(new Sample(sampleID, sample));
					childToParentGenotypes.put(sampleID, t);
				}
				// child may have been added because the parent was found first, in that case there is only a key, so
				// make child object + genotype!
				else if (childToParentGenotypes.containsKey(sampleID) && sample != null)
				{
					// System.out.println("CHILD - UPDATING GENOTYPE");
					if (childToParentGenotypes.get(sampleID).getChild() != null)
					{
						throw new IOException("Child genotype for '" + sampleID + "' already known !");
					}
					Sample child = new Sample(sampleID, sample);
					childToParentGenotypes.get(sampleID).setChild(child);
				}
			}

			// found a mother
			else if (motherToChild.containsKey(sampleID))
			{
				// child not seen yet, look it up and add it here, but without genotype (!)
				if (!childToParentGenotypes.containsKey(motherToChild.get(sampleID).getId()))
				{
					// System.out.println("MOTHER - NEW TRIO");
					Trio t = new Trio();
					t.setMother(new Sample(sampleID, sample));
					childToParentGenotypes.put(motherToChild.get(sampleID).getId(), t);
				}
				// child seen, check if mother was already known, we do not expect/want this to happen
				else
				{
					if (childToParentGenotypes.get(motherToChild.get(sampleID).getId()).getMother() != null)
					{
						throw new IOException("Mother '" + sampleID + "' already known for child '"
								+ motherToChild.get(sampleID) + "' !");
					}
					// System.out.println("MOTHER - UPDATING GENOTYPE");
					childToParentGenotypes.get(motherToChild.get(sampleID).getId()).setMother(
							new Sample(sampleID, sample));
				}
			}
			else if (fatherToChild.containsKey(sampleID))
			{
				// child not seen yet, look it up and add it here, but without genotype (!)
				if (!childToParentGenotypes.containsKey(fatherToChild.get(sampleID).getId()))
				{
					// System.out.println("FATHER - NEW TRIO");
					Trio t = new Trio();
					t.setFather(new Sample(sampleID, sample));
					childToParentGenotypes.put(fatherToChild.get(sampleID).getId(), t);
				}
				// child seen, check if father was already known, we do not expect/want this to happen
				else
				{
					if (childToParentGenotypes.get(fatherToChild.get(sampleID).getId()).getFather() != null)
					{
						throw new IOException("Father '" + sampleID + "' already known for child '"
								+ fatherToChild.get(sampleID) + "' !");
					}
					// System.out.println("FATHER - UPDATING GENOTYPE");
					childToParentGenotypes.get(fatherToChild.get(sampleID).getId()).setFather(
							new Sample(sampleID, sample));
				}
			}
			else
			{
				LOG.warn("Sample ID '" + sampleID
						+ "' not in list of children, mothers or fathers !! ignoring for further analysis !!");
			}

			// System.out.println("sampleID " + sampleID);
			// System.out.println("\t" + sample.get("GT") + " is a " + (pedigree.containsKey(sampleID) ? "child" :
			// "parent"));
		}

		LOG.info("Found " + childToParentGenotypes.size() + " trios..");

		HashMap<String, Trio> completeTrios = getCompleteTrios(childToParentGenotypes);

		LOG.info("Of which " + completeTrios.size() + " complete, having child+mother+father..");

		int totalDenovoForVariant = 0;

		for (String child : completeTrios.keySet())
		{

			int denovoForTrio = findDeNovoVariants(completeTrios.get(child));
			totalDenovoForVariant += denovoForTrio;

			if (geneSymbol != null)
			{
				// matrix of gene x trio x denovo counts
				if (geneTrioCounts.containsKey(geneSymbol))
				{
					if (geneTrioCounts.get(geneSymbol).containsKey(child))
					{
						geneTrioCounts.get(geneSymbol).put(child,
								geneTrioCounts.get(geneSymbol).get(child) + denovoForTrio);
						// System.out.println("GENE see, CHILD seen, for gene " + geneSymbol + " child " + child);
					}
					else
					{
						// System.out.println("new child for: " + geneSymbol + " child " + child);
						geneTrioCounts.get(geneSymbol).put(child, denovoForTrio);
					}

					// System.out.println("gene seen but NEW trio, adding: " + geneSymbol + " - " + denovoForTrio +
					// " for trio " + child) ;

				}
				else
				{
					// System.out.println("all new! adding: " + geneSymbol + " - " + denovoForTrio + " for trio " +
					// child) ;
					LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
					counts.put(child, denovoForTrio);
					geneTrioCounts.put(geneSymbol, counts);
				}

				// System.out.println(child + ", " + childToParentGenotypes.get(child));
			}

		}

		// also add total
		if (geneTrioCounts.get(geneSymbol).containsKey("TOTAL"))
		{
			geneTrioCounts.get(geneSymbol).put("TOTAL",
					geneTrioCounts.get(geneSymbol).get("TOTAL") + totalDenovoForVariant);
		}
		else
		{
			geneTrioCounts.get(geneSymbol).put("TOTAL", totalDenovoForVariant);
		}

		resultMap.put(DENOVO, totalDenovoForVariant);
		return resultMap;
	}

	/**
	 * Filter trios for those that are complete (c+f+m)
	 * 
	 * @param trios
	 * @return
	 */
	public HashMap<String, Trio> getCompleteTrios(HashMap<String, Trio> trios)
	{
		HashMap<String, Trio> result = new HashMap<String, Trio>();

		for (String key : trios.keySet())
		{
			if (trios.get(key).getChild() != null && trios.get(key).getMother() != null
					&& trios.get(key).getFather() != null)
			{
				result.put(key, trios.get(key));
				// System.out.println("TRIO OK: " + trios.get(key));
			}
			else
			{
				// System.out.println("TRIO BAD: " + trios.get(key));
			}
		}

		return result;
	}

	/**
	 * Find de novo variants in complete trios Genotype may be missing
	 * 
	 * @param t
	 * @return
	 */
	public int findDeNovoVariants(Trio t)
	{

		/**
		 * Null checks
		 */

		if (t.getMother().getGenotype().get("GT") == null)
		{
			LOG.warn("Maternal genotype null, skipping trio for child " + t.getChild().getId());
			return 0;
		}
		if (t.getFather().getGenotype().get("GT") == null)
		{
			LOG.warn("Paternal genotype null, skipping trio for child " + t.getChild().getId());
			return 0;
		}
		if (t.getChild().getGenotype().get("GT") == null)
		{
			LOG.warn("Child genotype null, skipping trio for child " + t.getChild().getId());
			return 0;
		}

		/**
		 * Quality checks: genotype completeness
		 */

		String[] mat_all = t.getMother().getGenotype().get("GT").toString().split("/", -1);
		if (mat_all.length != 2)
		{
			LOG.warn("Maternal genotype split by '/' does not have 2 elements, skipping trio for child "
					+ t.getChild().getId());
			return 0;
		}

		String[] pat_all = t.getFather().getGenotype().get("GT").toString().split("/", -1);
		if (pat_all.length != 2)
		{
			LOG.warn("Paternal genotype split by '/' does not have 2 elements, skipping trio for child "
					+ t.getChild().getId());
			return 0;
		}

		String[] chi_all = t.getChild().getGenotype().get("GT").toString().split("/", -1);
		if (chi_all.length != 2)
		{
			LOG.warn("Child genotype split by '/' does not have 2 elements, skipping trio for child "
					+ t.getChild().getId());
			return 0;
		}

		/**
		 * Quality checks: read depth
		 */
		int minDepth = 20;

		int mat_dp = Integer.parseInt(t.getMother().getGenotype().get("DP").toString());
		if (mat_dp < minDepth)
		{
			LOG.warn("Maternal genotype has less than " + minDepth + " reads (" + mat_dp
					+ "), skipping trio for child " + t.getChild().getId());
			return 0;
		}

		int pat_dp = Integer.parseInt(t.getFather().getGenotype().get("DP").toString());
		if (pat_dp < minDepth)
		{
			LOG.warn("Paternal genotype has less than " + minDepth + " reads (" + pat_dp
					+ "), skipping trio for child " + t.getChild().getId());
			return 0;
		}

		int child_dp = Integer.parseInt(t.getChild().getGenotype().get("DP").toString());
		if (child_dp < minDepth)
		{
			LOG.warn("Child genotype has less than " + minDepth + " reads (" + child_dp + "), skipping trio for child "
					+ t.getChild().getId());
			return 0;
		}

		/**
		 * Quality checks: genotype quality
		 */
		double minQual = 30.00;

		double mat_gq = Double.parseDouble(t.getMother().getGenotype().get("GQ").toString());
		if (mat_gq < minQual)
		{
			LOG.warn("Maternal genotype has less than " + minQual + " quality (" + mat_gq
					+ "), skipping trio for child " + t.getChild().getId());
			return 0;
		}

		double pat_gq = Double.parseDouble(t.getFather().getGenotype().get("GQ").toString());
		if (pat_gq < minQual)
		{
			LOG.warn("Paternal genotype has less than " + minQual + " quality (" + pat_gq
					+ "), skipping trio for child " + t.getChild().getId());
			return 0;
		}

		double child_gq = Double.parseDouble(t.getChild().getGenotype().get("GQ").toString());
		if (child_gq < minQual)
		{
			LOG.warn("Child genotype has less than " + minQual + " quality (" + child_gq
					+ "), skipping trio for child " + t.getChild().getId());
			return 0;
		}

		/**
		 * Test if any combination of parent alleles can form the child alleles and return if possible
		 */

		for (String ma : mat_all)
		{
			for (String pa : pat_all)
			{
				// if child alleles in any combinations match, return 0
				if ((chi_all[0].equals(ma) && chi_all[1].equals(pa))
						|| (chi_all[0].equals(pa) && chi_all[1].equals(ma)))
				{
					return 0;
				}
			}
		}

		/**
		 * If none pass, we found a de novo variant
		 */
		// System.out.println("De novo variant found for trio " + t);
		LOG.info("De novo variant found for trio " + t);
		return 1;
	}

	@Override
	public EntityMetaData getOutputMetaData()
	{
		DefaultEntityMetaData metadata = new DefaultEntityMetaData(this.getClass().getName(), MapEntity.class);
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(DENOVO, FieldTypeEnum.STRING).setLabel(DENOVO_LABEL));
		return metadata;
	}

}
