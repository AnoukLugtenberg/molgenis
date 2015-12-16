package org.molgenis.data.annotator.websettings;

import org.molgenis.data.annotation.entity.impl.GeneNetworksAnnotator;
import org.molgenis.data.settings.DefaultSettingsEntity;
import org.molgenis.data.settings.DefaultSettingsEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class GeneNetworksAnnotatorSettings extends DefaultSettingsEntity
{
	private static final long serialVersionUID = 1L;
	private static final String ID = GeneNetworksAnnotator.NAME;

	public GeneNetworksAnnotatorSettings()
	{
		super(ID);
	}

	@Component
	public static class Meta extends DefaultSettingsEntityMetaData
	{
		public static final String PROJECT_ENTITY = "examEntity";
		public static final String VARIANT_ENTITY_ATTRIBUTE = "entityName";
		public static final String GENE_NETWORK_URL = "url";

		public Meta()
		{
			super(ID);
			setLabel("Gene Network annotator settings");
			String defaultEntity = "Project";
			String defaultEntityAttribute = "ID";
			String defaultUrlAttribute = "http://molgenis27.target.rug.nl";
			addAttribute(PROJECT_ENTITY).setLabel("Examination Entity Name").setDefaultValue(defaultEntity);
			addAttribute(VARIANT_ENTITY_ATTRIBUTE).setLabel("Variant Entity Attribute Name").setDefaultValue(defaultEntityAttribute);
			addAttribute(GENE_NETWORK_URL).setLabel("Adress of the Gene network server").setDefaultValue(defaultUrlAttribute);
		}
	}

}
