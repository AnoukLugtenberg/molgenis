package org.molgenis.ontology.ic;

import static org.molgenis.MolgenisFieldTypes.DECIMAL;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.ontology.core.model.OntologyPackage.PACKAGE_ONTOLOGY;

import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.molgenis.ontology.core.model.OntologyPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TermFrequencyEntityMetaData extends SystemEntityMetaDataImpl
{
	public final static String SIMPLE_NAME = "TermFrequency";
	public static final String TERM_FREQUENCY = PACKAGE_ONTOLOGY + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public final static String ID = "id";
	public final static String TERM = "term";
	public final static String FREQUENCY = "frequency";
	public final static String OCCURRENCE = "occurrence";

	private final OntologyPackage ontologyPackage;

	@Autowired
	TermFrequencyEntityMetaData(OntologyPackage ontologyPackage)
	{
		super(SIMPLE_NAME, PACKAGE_ONTOLOGY);
		this.ontologyPackage = ontologyPackage;
	}

	@Override
	public void init()
	{
		setPackage(ontologyPackage);

		addAttribute(ID, ROLE_ID).setAuto(true);
		addAttribute(TERM).setNillable(false);
		addAttribute(FREQUENCY).setDataType(INT).setNillable(false);
		addAttribute(OCCURRENCE).setDataType(DECIMAL).setNillable(false);
	}
}