package org.molgenis.data.vcf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.support.AbstractRepository;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.genotype.GenotypeDataException;
import org.molgenis.vcf.VcfInfo;
import org.molgenis.vcf.VcfReader;
import org.molgenis.vcf.VcfRecord;
import org.molgenis.vcf.VcfSample;
import org.molgenis.vcf.meta.VcfMeta;
import org.molgenis.vcf.meta.VcfMetaFormat;
import org.molgenis.vcf.meta.VcfMetaInfo;

/**
 * Repository implementation for vcf files.
 * 
 * The filename without the extension is considered to be the entityname
 */
public class VcfRepository extends AbstractRepository
{
	private static final Logger logger = Logger.getLogger(VcfRepository.class);

	public static final String BASE_URL = "vcf://";
	public static final String CHROM = "#CHROM";
	public static final String ALT = "ALT";
	public static final String POS = "POS";
	public static final String REF = "REF";
	public static final String FILTER = "FILTER";
	public static final String QUAL = "QUAL";
	public static final String ID = "ID";
	public static final String INFO = "INFO";
	public static final String SAMPLES = "SAMPLES";

	private final File file;
	private final String entityName;

	private DefaultEntityMetaData entityMetaData;
	private List<VcfReader> vcfReaderRegistry;
	private DefaultEntityMetaData sampleEntityMetaData;
	private boolean hasFormatMetaData;

	public VcfRepository(File file, String entityName) throws IOException
	{
		super(BASE_URL + file.getName());
		this.file = file;
		this.entityName = entityName;
	}

	@Override
	public Iterator<Entity> iterator()
	{
		VcfReader vcfReader;
		try
		{
			vcfReader = createVcfReader();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		final VcfReader finalVcfReader = vcfReader;

		return new Iterator<Entity>()
		{
			Iterator<VcfRecord> vcfRecordIterator = finalVcfReader.iterator();

			@Override
			public boolean hasNext()
			{
				return vcfRecordIterator.hasNext();
			}

			@Override
			public Entity next()
			{

				Entity entity = new MapEntity();
				try
				{
					VcfRecord vcfRecord = vcfRecordIterator.next();
					entity.set(CHROM, vcfRecord.getChromosome());
					entity.set(ALT, StringUtils.join(vcfRecord.getAlternateAlleles(), ','));
					entity.set(POS, vcfRecord.getPosition());
					entity.set(REF, vcfRecord.getReferenceAllele());
					entity.set(FILTER, vcfRecord.getFilterStatus());
					entity.set(QUAL, vcfRecord.getQuality());
					entity.set(ID, StringUtils.join(vcfRecord.getIdentifiers(), ','));
					for (VcfInfo vcfInfo : vcfRecord.getInformation())
					{
						Object val = vcfInfo.getVal();
						if (val instanceof List<?>)
						{
							// TODO support list of primitives datatype
							val = StringUtils.join((List<?>) val, ',');
						}
						entity.set(vcfInfo.getKey(), val);
					}
					if (hasFormatMetaData)
					{
						List<Entity> samples = new ArrayList<Entity>();
						Iterator<VcfSample> sampleIterator = vcfRecord.getSamples().iterator();
						Iterator<String> sampleNameIterator = finalVcfReader.getVcfMeta().getSampleNames().iterator();
						while (sampleIterator.hasNext())
						{
							String[] format = vcfRecord.getFormat();
							VcfSample sample = sampleIterator.next();
							Entity sampleEntity = new MapEntity(sampleEntityMetaData);
							for (int i = 0; i < format.length; i = i + 1)
							{
								entity.set(format[i], sample.getData(i));
							}
							sampleEntity.set(ID, sampleNameIterator.next());
							samples.add(sampleEntity);
						}
						entity.set(SAMPLES, samples);
					}
				}
				catch (Exception e)
				{

				}
				return entity;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		if (entityMetaData == null)
		{
			entityMetaData = new DefaultEntityMetaData(entityName);
			try
			{
				VcfReader vcfReader = createVcfReader();
				VcfMeta vcfMeta;
				try
				{
					vcfMeta = vcfReader.getVcfMeta();
                    Iterable<VcfMetaFormat> formatMetaData = vcfReader.getVcfMeta().getFormatMeta();
                    createSampleEntityMetaData(formatMetaData);
				}
				finally
				{
					if (vcfReader != null) vcfReader.close();
				}
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(CHROM,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(ALT,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(POS,
						MolgenisFieldTypes.FieldTypeEnum.LONG));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(REF,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(FILTER,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(QUAL,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(ID,
						MolgenisFieldTypes.FieldTypeEnum.STRING));
				DefaultAttributeMetaData infoMetaData = new DefaultAttributeMetaData(INFO,
						MolgenisFieldTypes.FieldTypeEnum.COMPOUND);
				List<AttributeMetaData> metadataInfoField = new ArrayList<AttributeMetaData>();
				for (VcfMetaInfo info : vcfMeta.getInfoMeta())
				{
					DefaultAttributeMetaData attributeMetaData = new DefaultAttributeMetaData(info.getId(),
							vcfReaderFormatToMolgenisType(info));
					attributeMetaData.setDescription(info.getDescription());
					metadataInfoField.add(attributeMetaData);
				}
				infoMetaData.setAttributesMetaData(metadataInfoField);
				entityMetaData.addAttributeMetaData(infoMetaData);
				if (hasFormatMetaData)
				{
					DefaultAttributeMetaData samplesAttributeMeta = new DefaultAttributeMetaData(SAMPLES,
							MolgenisFieldTypes.FieldTypeEnum.MREF);
					samplesAttributeMeta.setRefEntity(sampleEntityMetaData);
					entityMetaData.addAttributeMetaData(samplesAttributeMeta);
				}
				entityMetaData.setIdAttribute(ID);
				entityMetaData.setLabelAttribute(ID);
			}
			catch (IOException e)
			{

			}
		}
		return entityMetaData;
	}

    void createSampleEntityMetaData(Iterable<VcfMetaFormat> formatMetaData) {
        hasFormatMetaData = formatMetaData.iterator().hasNext();
        if (hasFormatMetaData)
        {
            sampleEntityMetaData = new DefaultEntityMetaData(entityName + "_Sample");
            sampleEntityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(ID,
                    MolgenisFieldTypes.FieldTypeEnum.STRING));
            for (VcfMetaFormat meta : formatMetaData)
            {
                AttributeMetaData attributeMetaData = new DefaultAttributeMetaData(meta.getId(),
                        VcfFieldTypeToMolgenisFieldType(meta.getType()));
                sampleEntityMetaData.addAttributeMetaData(attributeMetaData);
            }
            sampleEntityMetaData.setIdAttribute(ID);
            sampleEntityMetaData.setLabelAttribute(ID);
        }
    }

    private MolgenisFieldTypes.FieldTypeEnum vcfReaderFormatToMolgenisType(VcfMetaInfo vcfMetaInfo)
	{
		String number = vcfMetaInfo.getNumber();
		boolean isListValue;
		try
		{
			isListValue = number.equals("A") || number.equals("R") || number.equals("G") || number.equals(".")
					|| Integer.valueOf(number) > 1;
		}
		catch (NumberFormatException ex)
		{
			throw new GenotypeDataException("Error parsing length of vcf info field. " + number
					+ " is not a valid int or expected preset (A, R, G, .)", ex);
		}
		switch (vcfMetaInfo.getType())
		{
			case CHARACTER:
				if (isListValue)
				{
					// TODO support list of primitives datatype
					return MolgenisFieldTypes.FieldTypeEnum.STRING;
				}
				return MolgenisFieldTypes.FieldTypeEnum.STRING;
			case FLAG:
				return MolgenisFieldTypes.FieldTypeEnum.BOOL;
			case FLOAT:
				if (isListValue)
				{
					// TODO support list of primitives datatype
					return MolgenisFieldTypes.FieldTypeEnum.STRING;
				}
				return MolgenisFieldTypes.FieldTypeEnum.DECIMAL;
			case INTEGER:
				if (isListValue)
				{
					// TODO support list of primitives datatype
					return MolgenisFieldTypes.FieldTypeEnum.STRING;
				}
				return MolgenisFieldTypes.FieldTypeEnum.INT;
			case STRING:
				if (isListValue)
				{
					// TODO support list of primitives datatype
					return MolgenisFieldTypes.FieldTypeEnum.STRING;
				}
				return MolgenisFieldTypes.FieldTypeEnum.STRING;
			default:
				throw new MolgenisDataException("unknown vcf info type [" + vcfMetaInfo.getType() + "]");
		}
	}

	private MolgenisFieldTypes.FieldTypeEnum VcfFieldTypeToMolgenisFieldType(VcfMetaFormat.Type type)
	{
		switch (type)
		{
			case CHARACTER:
				return MolgenisFieldTypes.FieldTypeEnum.STRING;
			case FLOAT:
				return MolgenisFieldTypes.FieldTypeEnum.DECIMAL;
			case INTEGER:
				return MolgenisFieldTypes.FieldTypeEnum.INT;
			case STRING:
				return MolgenisFieldTypes.FieldTypeEnum.STRING;
			default:
				throw new MolgenisDataException("unknown vcf field type [" + type + "]");
		}
	}

	private VcfReader createVcfReader() throws IOException
	{
		VcfReader reader = new VcfReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		// register reader so close() can close all readers
		if (vcfReaderRegistry == null) vcfReaderRegistry = new ArrayList<VcfReader>();
		vcfReaderRegistry.add(reader);

		return reader;
	}

	@Override
	public void close() throws IOException
	{
		if (vcfReaderRegistry != null)
		{
			for (VcfReader vcfReader : vcfReaderRegistry)
			{
				try
				{
					vcfReader.close();
				}
				catch (IOException e)
				{
					logger.warn(e);
				}
			}
		}
	}
}
