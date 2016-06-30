package org.molgenis.app.promise.mapper;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;

import java.time.LocalDate;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.join;
import static org.molgenis.app.promise.mapper.RadboudMapper.*;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.*;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.REF_GENDER_TYPES;

class RadboudSampleMap
{
	private static final String XML_MICROBIOOM = "MICROBIOOM";
	private static final String XML_GENDER = "GESLACHT";
	private static final String XML_BIRTHDATE = "GEBOORTEDATUM";
	private static final String XML_DEELBIOBANKS = "DEELBIOBANKS";
	private static final String XML_VOORGESCH = "VOORGESCH";
	private static final String XML_FAMANAM = "FAMANAM";
	private static final String XML_BEHANDEL = "BEHANDEL";
	private static final String XML_FOLLOWUP = "FOLLOWUP";
	private static final String XML_BEELDEN = "BEELDEN";
	private static final String XML_VRAGENLIJST = "VRAGENLIJST";
	private static final String XML_OMICS = "OMICS";
	private static final String XML_ROUTINEBEP = "ROUTINEBEP";
	private static final String XML_GWAS = "GWAS";
	private static final String XML_HISTOPATH = "HISTOPATH";
	private static final String XML_OUTCOME = "OUTCOME";
	private static final String XML_ANDERS = "ANDERS";
	private static final String XML_DNA = "DNA";
	private static final String XML_DNABEENMERG = "DNABEENMERG";
	private static final String XML_BLOED = "BLOED";
	private static final String XML_BLOEDPLASMA = "BLOEDPLASMA";
	private static final String XML_BLOEDSERUM = "BLOEDSERUM";
	private static final String XML_WEEFSELSOORT = "WEEFSELSOORT";
	private static final String XML_URINE = "URINE";
	private static final String XML_SPEEKSEL = "SPEEKSEL";
	private static final String XML_FECES = "FECES";
	private static final String XML_RNA = "RNA";
	private static final String XML_RNABEENMERG = "RNABEENMERG";
	private static final String XML_GASTROINTMUC = "GASTROINTMUC";
	private static final String XML_LIQUOR = "LIQUOR";
	private static final String XML_CELLBEENMERG = "CELLBEENMERG";
	private static final String XML_MONONUCLBLOED = "MONONUCLBLOED";
	private static final String XML_MONONUCMERG = "MONONUCMERG";
	private static final String XML_GRANULOCYTMERG = "GRANULOCYTMERG";
	private static final String XML_MONOCYTMERG = "MONOCYTMERG";
	private static final String XML_GWASOMNI = "GWASOMNI";
	private static final String XML_GWAS370CNV = "GWAS370CNV";
	private static final String XML_EXOOMCHIP = "EXOOMCHIP";

	private Map<String, AggregatedSampleInfo> sampleInfos = newHashMap();

	private DataService dataService;

	RadboudSampleMap(DataService dataService)
	{
		this.dataService = requireNonNull(dataService);
	}

	void addSample(Entity radboudSampleEntity)
	{
		String biobankId = getBiobankId(radboudSampleEntity);
		sampleInfos.putIfAbsent(biobankId, new AggregatedSampleInfo());
		AggregatedSampleInfo info = sampleInfos.get(biobankId);

		info.addDataCategoryIds(collectDataCategoryIds(radboudSampleEntity));
		info.addMaterialIds(collectMaterialIds(radboudSampleEntity));
		info.addOmicsIds(collectOmicsIds(radboudSampleEntity));
		info.addSexIds(collectSexIds(radboudSampleEntity));
		info.incrementSize();

		Integer age = collectAge(radboudSampleEntity);
		info.setAgeMax(age);
		info.setAgeMin(age);
	}

	Iterable<Entity> getDataCategories(Entity radboudBiobankEntity)
	{
		String biobankId = getBiobankId(radboudBiobankEntity);
		Set<String> dataCategoryTypeIds = new LinkedHashSet<>();

		dataCategoryTypeIds.addAll(sampleInfos.get(biobankId).getDataCategoryIds());

		{ // get rest of data category ids from the biobank entity itself
			if ("1".equals(radboudBiobankEntity.getString(XML_VOORGESCH)))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_FAMANAM)))
			{
				dataCategoryTypeIds.add("GENEALOGICAL_RECORDS");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_BEHANDEL)))
			{
				dataCategoryTypeIds.add("MEDICAL_RECORDS");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_FOLLOWUP)))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_BEELDEN)))
			{
				dataCategoryTypeIds.add("IMAGING_DATA");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_VRAGENLIJST)))
			{
				dataCategoryTypeIds.add("SURVEY_DATA");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_OMICS)))
			{
				dataCategoryTypeIds.add("PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_ROUTINEBEP)))
			{
				dataCategoryTypeIds.add("PHYSIOLOGICAL_BIOCHEMICAL_MEASUREMENTS");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_GWAS)))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_HISTOPATH)))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_OUTCOME)))
			{
				dataCategoryTypeIds.add("NATIONAL_REGISTRIES");
			}

			if ("1".equals(radboudBiobankEntity.getString(XML_ANDERS)))
			{
				dataCategoryTypeIds.add("OTHER");
			}

			if (dataCategoryTypeIds.isEmpty())
			{
				dataCategoryTypeIds.add("NAV");
			}
		}

		return getTypeEntities(REF_DATA_CATEGORY_TYPES, dataCategoryTypeIds);
	}

	Iterable<Entity> getMaterials(String biobankId)
	{
		return getTypeEntities(REF_MATERIAL_TYPES, sampleInfos.get(biobankId).getMaterialIds());
	}

	Iterable<Entity> getOmics(String biobankId)
	{
		return getTypeEntities(REF_OMICS_DATA_TYPES, sampleInfos.get(biobankId).getOmicsIds());
	}

	Iterable<Entity> getSex(String biobankId) throws RuntimeException
	{
		return getTypeEntities(REF_GENDER_TYPES, sampleInfos.get(biobankId).getSexIds());
	}

	int getAgeMin(String biobankId)
	{
		return sampleInfos.get(biobankId).getAgeMin();
	}

	int getAgeMax(String biobankId)
	{
		return sampleInfos.get(biobankId).getAgeMax();
	}

	int getSize(String biobankId)
	{
		return sampleInfos.get(biobankId).getSize();
	}

	private Set<String> collectDataCategoryIds(Entity radboudSampleEntity)
	{
		Set<String> dataCategoryTypeIds = newHashSet();
		String deelbiobanks = radboudSampleEntity.getString(XML_DEELBIOBANKS);
		if (deelbiobanks != null && Integer.valueOf(deelbiobanks) >= 1)
		{
			dataCategoryTypeIds.add("BIOLOGICAL_SAMPLES");
		}
		return dataCategoryTypeIds;
	}

	private Iterable<Entity> getTypeEntities(String typeEntity, Set<String> types)
	{
		List<Object> typeIds = newArrayList();
		if (types.isEmpty())
		{
			typeIds.add("NAV");
		}
		else
		{
			typeIds.addAll(types);
		}
		Iterable<Entity> typeEntities = dataService.findAll(typeEntity, typeIds.stream()).collect(toList());
		if (!typeIds.iterator().hasNext())
		{
			throw new RuntimeException("Unknown '" + typeEntity + "' [" + join(typeIds, ',') + "]");
		}
		return typeEntities;
	}

	private Set<String> collectMaterialIds(Entity radboudSampleEntity)
	{
		Set<String> materialTypeIds = new HashSet<>();

		if ("1".equals(radboudSampleEntity.getString(XML_DNA)) || "1"
				.equals(radboudSampleEntity.getString(XML_DNABEENMERG)))
		{
			materialTypeIds.add("DNA");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_BLOED)))
		{
			materialTypeIds.add("WHOLE_BLOOD");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_BLOEDPLASMA)))
		{
			materialTypeIds.add("PLASMA");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_BLOEDSERUM)))
		{
			materialTypeIds.add("SERUM");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_WEEFSELSOORT)))
		{
			materialTypeIds.add("TISSUE_PARAFFIN_EMBEDDED");
		}
		else if ("2".equals(radboudSampleEntity.getString(XML_WEEFSELSOORT)))
		{
			materialTypeIds.add("TISSUE_FROZEN");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_URINE)))
		{
			materialTypeIds.add("URINE");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_SPEEKSEL)))
		{
			materialTypeIds.add("SALIVA");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_FECES)))
		{
			materialTypeIds.add("FECES");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_RNA)) || "1"
				.equals(radboudSampleEntity.getString(XML_RNABEENMERG)))
		{
			materialTypeIds.add("MICRO_RNA");
		}

		if ("1".equals(radboudSampleEntity.getString(XML_GASTROINTMUC)) || "1"
				.equals(radboudSampleEntity.getString(XML_LIQUOR)) || "1"
				.equals(radboudSampleEntity.getString(XML_CELLBEENMERG)) || "1"
				.equals(radboudSampleEntity.getString(XML_MONONUCLBLOED)) || "1"
				.equals(radboudSampleEntity.getString(XML_MONONUCMERG)) || "1"
				.equals(radboudSampleEntity.getString(XML_GRANULOCYTMERG)) || "1"
				.equals(radboudSampleEntity.getString(XML_MONOCYTMERG)) || "1"
				.equals(radboudSampleEntity.getString(XML_MICROBIOOM)))
		{
			materialTypeIds.add("OTHER");
		}

		return materialTypeIds;
	}

	private Set<String> collectOmicsIds(Entity radboudSampleEntity)
	{
		Set<String> omicsTypeIds = new HashSet<>();

		if ("1".equals(radboudSampleEntity.getString(XML_GWASOMNI)) || "1"
				.equals(radboudSampleEntity.getString(XML_GWAS370CNV)) || "1"
				.equals(radboudSampleEntity.getString(XML_EXOOMCHIP)))
		{
			omicsTypeIds.add("GENOMICS");
		}

		return omicsTypeIds;
	}

	private Set<String> collectSexIds(Entity radboudSampleEntity)
	{
		Set<String> genderTypeIds = new HashSet<>();

		String genderValue = radboudSampleEntity.getString(XML_GENDER);
		if ("1".equals(genderValue))
		{
			genderTypeIds.add("FEMALE");
		}
		if ("2".equals(genderValue))
		{
			genderTypeIds.add("MALE");
		}
		if ("3".equals(genderValue))
		{
			genderTypeIds.add("UNKNOWN");
		}

		return genderTypeIds;
	}

	private Integer collectAge(Entity radboudSampleEntity)
	{
		String birthDate = radboudSampleEntity.getString(XML_BIRTHDATE);
		if (birthDate != null && !birthDate.isEmpty())
		{
			LocalDate start = parse(birthDate, ISO_DATE_TIME);
			LocalDate end = now();
			Long age = YEARS.between(start, end);
			return age.intValue();
		}
		else
		{
			return null;
		}
	}

	private class AggregatedSampleInfo
	{
		private Set<String> materialIds = newHashSet();
		private Set<String> dataCategoryIds = newHashSet();
		private Set<String> omicsIds = newHashSet();
		private Set<String> sexIds = newHashSet();
		private int size = 0;
		private int ageMin = 0;
		private int ageMax = 0;

		Set<String> getMaterialIds()
		{
			return materialIds;
		}

		void addMaterialIds(Set<String> materialTypes)
		{
			this.materialIds.addAll(materialTypes);
		}

		Set<String> getDataCategoryIds()
		{
			return dataCategoryIds;
		}

		void addDataCategoryIds(Set<String> dataCategories)
		{
			this.dataCategoryIds.addAll(dataCategories);
		}

		Set<String> getOmicsIds()
		{
			return omicsIds;
		}

		void addOmicsIds(Set<String> omics)
		{
			this.omicsIds.addAll(omics);
		}

		Set<String> getSexIds()
		{
			return sexIds;
		}

		void addSexIds(Set<String> sex)
		{
			this.sexIds.addAll(sex);
		}

		int getSize()
		{
			return size;
		}

		void incrementSize()
		{
			size++;
		}

		public int getAgeMin()
		{
			return ageMin;
		}

		public void setAgeMin(Integer ageMin)
		{
			if (ageMin == null) return;
			if (this.ageMin > ageMin) this.ageMin = ageMin;
		}

		public int getAgeMax()
		{
			return ageMax;
		}

		public void setAgeMax(Integer ageMax)
		{
			if (ageMax == null) return;
			if (this.ageMax < ageMax) this.ageMax = ageMax;
		}
	}
}


