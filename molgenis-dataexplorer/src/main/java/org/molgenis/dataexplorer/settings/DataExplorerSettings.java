package org.molgenis.dataexplorer.settings;

import static org.molgenis.MolgenisFieldTypes.BOOL;
import static org.molgenis.MolgenisFieldTypes.COMPOUND;
import static org.molgenis.MolgenisFieldTypes.HYPERLINK;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.MolgenisFieldTypes.TEXT;

import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.settings.DefaultSettingsEntity;
import org.molgenis.data.settings.DefaultSettingsEntityMetaData;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.dataexplorer.controller.DataExplorerController;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
public class DataExplorerSettings extends DefaultSettingsEntity
{
	private static final long serialVersionUID = 1L;

	private static final String ID = DataExplorerController.ID;

	public DataExplorerSettings()
	{
		super(ID);
	}

	public boolean getModAggregates()
	{
		Boolean value = getBoolean(Meta.MOD_AGGREGATES);
		return value != null ? value.booleanValue() : false;
	}

	public boolean getModAnnotators()
	{
		Boolean value = getBoolean(Meta.MOD_ANNOTATORS);
		return value != null ? value.booleanValue() : false;
	}

	public boolean getModCharts()
	{
		Boolean value = getBoolean(Meta.MOD_CHARTS);
		return value != null ? value.booleanValue() : false;
	}

	public boolean getModData()
	{
		Boolean value = getBoolean(Meta.MOD_CHARTS);
		return value != null ? value : false;
	}

	public boolean getModDiseaseMatcher()
	{
		Boolean value = getBoolean(Meta.MOD_DISEASE_MATCHER);
		return value != null ? value : false;
	}

	public boolean getModReports()
	{
		Boolean value = getBoolean(Meta.MOD_REPORTS);
		return value != null ? value : false;
	}

	public Boolean getGalaxyExport()
	{
		return getBoolean(Meta.DATA_GALAXY_EXPORT);
	}

	public EntityReport getEntityReport(String entityName)
	{
		return Iterables.find(getEntityReports(), new Predicate<EntityReport>()
		{
			@Override
			public boolean apply(EntityReport entityReport)
			{
				return entityReport.getEntity().equals(entityName);
			}
		}, null);
	}

	public Iterable<EntityReport> getEntityReports()
	{
		return getEntities(Meta.REPORTS_ENTITIES, EntityReport.class);
	}

	public Map<String, String> getAggregatesDistinctOverrides()
	{
		String distinctAttrOverridesStr = getString(Meta.AGGREGATES_DISTINCT_OVERRIDES);
		return new Gson().fromJson(distinctAttrOverridesStr, new TypeToken<Map<String, String>>()
		{
		}.getType());
	}

	@Component
	private static class Meta extends DefaultSettingsEntityMetaData
	{
		public static final String GENERAL = "general_";
		public static final String GENERAL_SEARCHBOX = "searchbox";
		public static final String GENERAL_ATTRIBUTE_SELECT = "attr_select";
		public static final String GENERAL_LAUNCH_WIZARD = "launch_wizard";
		public static final String GENERAL_HEADER_ABBREVIATE = "header_abbreviate";

		private static final boolean DEFAULT_GENERAL_SEARCHBOX = true;
		private static final boolean DEFAULT_GENERAL_ATTRIBUTE_SELECT = true;
		private static final boolean DEFAULT_GENERAL_LAUNCH_WIZARD = false;
		private static final int DEFAULT_GENERAL_HEADER_ABBREVIATE = 180;

		public static final String MOD = "mods";
		public static final String MOD_AGGREGATES = "mod_aggregates";
		public static final String MOD_ANNOTATORS = "mod_annotators";
		public static final String MOD_CHARTS = "mod_charts";
		public static final String MOD_DATA = "mod_data";
		public static final String MOD_DISEASE_MATCHER = "mod_diseasematcher";
		public static final String MOD_REPORTS = "mod_reports";

		private static final boolean DEFAULT_MOD_AGGREGATES = true;
		private static final boolean DEFAULT_MOD_ANNOTATORS = true;
		private static final boolean DEFAULT_MOD_CHARTS = true;
		private static final boolean DEFAULT_MOD_DATA = true;
		private static final boolean DEFAULT_MOD_DISEASE_MATCHER = true;
		private static final boolean DEFAULT_MOD_REPORT = true;

		public static final String DATA = "data";
		public static final String DATA_GALAXY_EXPORT = "data_galaxy_export";
		public static final String DATA_GALAXY_URL = "data_galaxy_url";
		public static final String DATA_GALAXY_API_KEY = "data_galaxy_api_key";
		public static final String DATA_GENOME_BROWSER = "data_genome_browser";

		private static final boolean DEFAULT_DATA_GALAXY_EXPORT = true;
		private static final boolean DEFAULT_DATA_GENOME_BROWSER = true;

		public static final String GENOMEBROWSER = "genomebrowser";
		public static final String GENOMEBROWSER_INIT = "gb_init";
		public static final String GENOMEBROWSER_INIT_BROWSER_LINKS = "gb_init_browser_links";
		public static final String GENOMEBROWSER_INIT_COORD_SYSTEM = "gb_init_coord_system";
		public static final String GENOMEBROWSER_INIT_LOCATION = "gb_init_location";
		public static final String GENOMEBROWSER_INIT_SOURCES = "gb_init_sources";
		public static final String GENOMEBROWSER_INIT_HIGHLIGHT_REGION = "gb_init_highlight_region";

		private static final String DEFAULT_GENOMEBROWSER_INIT_BROWSER_LINKS = "{Ensembl: 'http://www.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'}";
		private static final String DEFAULT_GENOMEBROWSER_INIT_COORD_SYSTEM = "{speciesName: 'Human',taxon: 9606,auth: 'GRCh',version: '37',ucscName: 'hg19'}";
		private static final String DEFAULT_GENOMEBROWSER_INIT_LOCATION = "chr:'1',viewStart:10000000,viewEnd:10100000,cookieKey:'human',nopersist:true";
		private static final String DEFAULT_GENOMEBROWSER_INIT_SOURCES = "[{name:'Genome',twoBitURI:'//www.biodalliance.org/datasets/hg19.2bit',tier_type: 'sequence'},{name: 'Genes',desc: 'Gene structures from GENCODE 19',bwgURI: '//www.biodalliance.org/datasets/gencode.bb',stylesheet_uri: '//www.biodalliance.org/stylesheets/gencode.xml',collapseSuperGroups: true,trixURI:'//www.biodalliance.org/datasets/geneIndex.ix'},{name: 'Repeats',desc: 'Repeat annotation from Ensembl 59',bwgURI: '//www.biodalliance.org/datasets/repeats.bb',stylesheet_uri: '//www.biodalliance.org/stylesheets/bb-repeats.xml'},{name: 'Conservation',desc: 'Conservation',bwgURI: '//www.biodalliance.org/datasets/phastCons46way.bw',noDownsample: true}]";
		private static final boolean DEFAULT_GENOMEBROWSER_INIT_HIGHLIGHT_REGION = false;

		public static final String AGGREGATES = "aggregates";
		public static final String AGGREGATES_DISTINCT_SELECT = "agg_distinct";
		public static final String AGGREGATES_DISTINCT_OVERRIDES = "agg_distinct_overrides";
		public static final String AGGREGATES_NO_RESULTS_MESSAGE = "agg_no_results_msg";

		public static final String REPORTS = "reports";
		public static final String REPORTS_ENTITIES = "reports_entities";

		private static final boolean DEFAULT_AGGREGATES_DISTINCT_SELECT = true;
		private static final String DEFAULT_AGGREGATES_NO_RESULTS_MESSAGE = "No results found";

		public Meta()
		{
			super(ID);
			setLabel("Data explorer settings");
			setDescription("Settings for the data explorer plugin.");

			addGeneralSettings();
			addModulesSettings();
		}

		private void addGeneralSettings()
		{
			DefaultAttributeMetaData generalAttr = addAttribute(GENERAL).setDataType(COMPOUND).setLabel("General");
			AttributeMetaData generalSearchboxAttr = new DefaultAttributeMetaData(GENERAL_SEARCHBOX).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_GENERAL_SEARCHBOX).setLabel("Show search box");
			AttributeMetaData generalAttrSelectAttr = new DefaultAttributeMetaData(GENERAL_ATTRIBUTE_SELECT)
					.setDataType(BOOL).setNillable(false).setDefaultValue(DEFAULT_GENERAL_ATTRIBUTE_SELECT)
					.setLabel("Show data item selection");
			AttributeMetaData generalLaunchWizardAttr = new DefaultAttributeMetaData(GENERAL_LAUNCH_WIZARD)
					.setDataType(BOOL).setNillable(false).setDefaultValue(DEFAULT_GENERAL_LAUNCH_WIZARD)
					.setLabel("Launch data item filter wizard");
			AttributeMetaData generalHeaderAbbreviateAttr = new DefaultAttributeMetaData(GENERAL_HEADER_ABBREVIATE)
					.setDataType(INT).setNillable(false).setDefaultValue(DEFAULT_GENERAL_HEADER_ABBREVIATE)
					.setLabel("Entity description abbreviation length");
			generalAttr.addAttributePart(generalSearchboxAttr);
			generalAttr.addAttributePart(generalAttrSelectAttr);
			generalAttr.addAttributePart(generalLaunchWizardAttr);
			generalAttr.addAttributePart(generalHeaderAbbreviateAttr);
		}

		private void addModulesSettings()
		{
			AttributeMetaData modAggregatesAttr = new DefaultAttributeMetaData(MOD_AGGREGATES).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_MOD_AGGREGATES).setLabel("Aggregates");
			AttributeMetaData modAnnotatorsAttr = new DefaultAttributeMetaData(MOD_ANNOTATORS).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_MOD_ANNOTATORS).setLabel("Annotators");
			AttributeMetaData modChartsAttr = new DefaultAttributeMetaData(MOD_CHARTS).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_MOD_CHARTS).setLabel("Charts");
			AttributeMetaData modDataAttr = new DefaultAttributeMetaData(MOD_DATA).setDataType(BOOL).setNillable(false)
					.setDefaultValue(DEFAULT_MOD_DATA).setLabel("Data");
			AttributeMetaData modDiseaseMatcherAttr = new DefaultAttributeMetaData(MOD_DISEASE_MATCHER)
					.setDataType(BOOL).setNillable(false).setDefaultValue(DEFAULT_MOD_DISEASE_MATCHER)
					.setLabel("Disease Matcher");
			AttributeMetaData modReportAttr = new DefaultAttributeMetaData(MOD_REPORTS).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_MOD_REPORT).setLabel("Reports");

			DefaultAttributeMetaData modAttr = addAttribute(MOD).setDataType(COMPOUND).setLabel("Modules");
			modAttr.addAttributePart(modAggregatesAttr);
			modAttr.addAttributePart(createModAggregatesSettings());
			modAttr.addAttributePart(modAnnotatorsAttr);
			modAttr.addAttributePart(modChartsAttr);
			modAttr.addAttributePart(modDataAttr);
			modAttr.addAttributePart(createModDataSettings());
			modAttr.addAttributePart(modDiseaseMatcherAttr);
			modAttr.addAttributePart(modReportAttr);
			modAttr.addAttributePart(createModReportSettings());
		}

		private AttributeMetaData createModDataSettings()
		{
			DefaultAttributeMetaData dataAttr = new DefaultAttributeMetaData(DATA).setDataType(COMPOUND)
					.setLabel("Data").setVisibleExpression("$('" + MOD_DATA + "').eq(true).value()");

			AttributeMetaData dataGalaxyExportAttr = new DefaultAttributeMetaData(DATA_GALAXY_EXPORT).setDataType(BOOL)
					.setNillable(false).setDefaultValue(DEFAULT_DATA_GALAXY_EXPORT).setLabel("Galaxy export");
			AttributeMetaData dataGalaxyUrlAttr = new DefaultAttributeMetaData(DATA_GALAXY_URL).setDataType(HYPERLINK)
					.setNillable(true).setLabel("Galaxy URL")
					.setVisibleExpression("$('" + DATA_GALAXY_EXPORT + "').eq(true).value()");
			AttributeMetaData dataGalaxyApiKeyAttr = new DefaultAttributeMetaData(DATA_GALAXY_API_KEY).setNillable(true)
					.setLabel("Galaxy API key")
					.setVisibleExpression("$('" + DATA_GALAXY_EXPORT + "').eq(true).value()");
			dataAttr.addAttributePart(dataGalaxyExportAttr);
			dataAttr.addAttributePart(dataGalaxyUrlAttr);
			dataAttr.addAttributePart(dataGalaxyApiKeyAttr);

			// genome browser
			DefaultAttributeMetaData genomeBrowserInitAttr = new DefaultAttributeMetaData(GENOMEBROWSER_INIT)
					.setDataType(COMPOUND).setLabel("Initialization");
			AttributeMetaData genomeBrowserInitBrowserLinksAttr = new DefaultAttributeMetaData(
					GENOMEBROWSER_INIT_BROWSER_LINKS).setNillable(false).setDataType(TEXT)
							.setDefaultValue(DEFAULT_GENOMEBROWSER_INIT_BROWSER_LINKS).setLabel("Browser links");
			AttributeMetaData genomeBrowserInitCoordSystemAttr = new DefaultAttributeMetaData(
					GENOMEBROWSER_INIT_COORD_SYSTEM).setNillable(false).setDataType(TEXT)
							.setDefaultValue(DEFAULT_GENOMEBROWSER_INIT_COORD_SYSTEM).setLabel("Coordinate system");
			AttributeMetaData genomeBrowserInitLocationAttr = new DefaultAttributeMetaData(GENOMEBROWSER_INIT_LOCATION)
					.setNillable(false).setDataType(TEXT).setDefaultValue(DEFAULT_GENOMEBROWSER_INIT_LOCATION)
					.setLabel("Location");
			AttributeMetaData genomeBrowserInitSourcesAttr = new DefaultAttributeMetaData(GENOMEBROWSER_INIT_SOURCES)
					.setNillable(false).setDataType(TEXT).setDefaultValue(DEFAULT_GENOMEBROWSER_INIT_SOURCES)
					.setLabel("Sources");
			AttributeMetaData genomeBrowserInitHighlightRegionAttr = new DefaultAttributeMetaData(
					GENOMEBROWSER_INIT_HIGHLIGHT_REGION).setNillable(false).setDataType(BOOL)
							.setDefaultValue(DEFAULT_GENOMEBROWSER_INIT_HIGHLIGHT_REGION).setLabel("Highlight region");

			genomeBrowserInitAttr.addAttributePart(genomeBrowserInitBrowserLinksAttr);
			genomeBrowserInitAttr.addAttributePart(genomeBrowserInitCoordSystemAttr);
			genomeBrowserInitAttr.addAttributePart(genomeBrowserInitLocationAttr);
			genomeBrowserInitAttr.addAttributePart(genomeBrowserInitSourcesAttr);
			genomeBrowserInitAttr.addAttributePart(genomeBrowserInitHighlightRegionAttr);

			DefaultAttributeMetaData dataGenomeBrowserAttr = new DefaultAttributeMetaData(DATA_GENOME_BROWSER)
					.setDataType(BOOL).setNillable(false).setDefaultValue(DEFAULT_DATA_GENOME_BROWSER)
					.setLabel("Genome Browser");

			DefaultAttributeMetaData genomeBrowserAttr = new DefaultAttributeMetaData(GENOMEBROWSER)
					.setDataType(COMPOUND).setLabel("Genome Browser")
					.setVisibleExpression("$('" + DATA_GENOME_BROWSER + "').eq(true).value()");
			genomeBrowserAttr.addAttributePart(genomeBrowserInitAttr);

			dataAttr.addAttributePart(dataGenomeBrowserAttr);
			dataAttr.addAttributePart(genomeBrowserAttr);

			return dataAttr;
		}

		private AttributeMetaData createModAggregatesSettings()
		{
			DefaultAttributeMetaData aggregatesAttr = new DefaultAttributeMetaData(AGGREGATES).setDataType(COMPOUND)
					.setLabel("Aggregates").setVisibleExpression("$('" + MOD_AGGREGATES + "').eq(true).value()");
			AttributeMetaData aggregatesDistinctSelectAttr = new DefaultAttributeMetaData(AGGREGATES_DISTINCT_SELECT)
					.setNillable(false).setDataType(BOOL).setDefaultValue(DEFAULT_AGGREGATES_DISTINCT_SELECT)
					.setLabel("Distinct aggregates");
			AttributeMetaData aggregatesDistinctOverrideAttr = new DefaultAttributeMetaData(
					AGGREGATES_DISTINCT_OVERRIDES).setDataType(TEXT).setLabel("Distinct attribute overrides")
							.setDescription("JSON object that maps entity names to attribute names")
							.setVisibleExpression("$('" + AGGREGATES_DISTINCT_SELECT + "').eq(true).value()");
			aggregatesAttr.addAttributePart(aggregatesDistinctSelectAttr);
			aggregatesAttr.addAttributePart(aggregatesDistinctOverrideAttr);
			return aggregatesAttr;
		}

		private AttributeMetaData createModReportSettings()
		{
			DefaultAttributeMetaData reportsAttr = new DefaultAttributeMetaData(REPORTS).setDataType(COMPOUND)
					.setLabel("Reports").setVisibleExpression("$('" + MOD_REPORTS + "').eq(true).value()");
			AttributeMetaData reportsEntitiesAttr = new DefaultAttributeMetaData(REPORTS_ENTITIES).setNillable(true)
					.setDataType(MREF).setRefEntity(EntityReport.META_DATA).setLabel("Reports");
			reportsAttr.addAttributePart(reportsEntitiesAttr);
			return reportsAttr;
		}

		@Override
		protected Entity getDefaultSettings()
		{
			// FIXME workaround for https://github.com/molgenis/molgenis/issues/1810
			MapEntity defaultSettings = new MapEntity(this);
			defaultSettings.set(GENERAL_SEARCHBOX, DEFAULT_GENERAL_SEARCHBOX);
			defaultSettings.set(GENERAL_ATTRIBUTE_SELECT, DEFAULT_GENERAL_ATTRIBUTE_SELECT);
			defaultSettings.set(GENERAL_LAUNCH_WIZARD, DEFAULT_GENERAL_LAUNCH_WIZARD);
			defaultSettings.set(GENERAL_HEADER_ABBREVIATE, DEFAULT_GENERAL_HEADER_ABBREVIATE);
			defaultSettings.set(MOD_AGGREGATES, DEFAULT_MOD_AGGREGATES);
			defaultSettings.set(MOD_ANNOTATORS, DEFAULT_MOD_ANNOTATORS);
			defaultSettings.set(MOD_CHARTS, DEFAULT_MOD_CHARTS);
			defaultSettings.set(MOD_DATA, DEFAULT_MOD_DATA);
			defaultSettings.set(MOD_DISEASE_MATCHER, DEFAULT_MOD_DISEASE_MATCHER);
			defaultSettings.set(MOD_REPORTS, DEFAULT_MOD_REPORT);
			defaultSettings.set(DATA_GALAXY_EXPORT, DEFAULT_DATA_GALAXY_EXPORT);
			defaultSettings.set(DATA_GENOME_BROWSER, DEFAULT_DATA_GENOME_BROWSER);
			defaultSettings.set(GENOMEBROWSER_INIT_BROWSER_LINKS, DEFAULT_GENOMEBROWSER_INIT_BROWSER_LINKS);
			defaultSettings.set(GENOMEBROWSER_INIT_COORD_SYSTEM, DEFAULT_GENOMEBROWSER_INIT_COORD_SYSTEM);
			defaultSettings.set(GENOMEBROWSER_INIT_LOCATION, DEFAULT_GENOMEBROWSER_INIT_LOCATION);
			defaultSettings.set(GENOMEBROWSER_INIT_SOURCES, DEFAULT_GENOMEBROWSER_INIT_SOURCES);
			defaultSettings.set(GENOMEBROWSER_INIT_HIGHLIGHT_REGION, DEFAULT_GENOMEBROWSER_INIT_HIGHLIGHT_REGION);
			defaultSettings.set(AGGREGATES_DISTINCT_SELECT, DEFAULT_AGGREGATES_DISTINCT_SELECT);
			defaultSettings.set(AGGREGATES_NO_RESULTS_MESSAGE, DEFAULT_AGGREGATES_NO_RESULTS_MESSAGE);
			return defaultSettings;
		}
	}
}
