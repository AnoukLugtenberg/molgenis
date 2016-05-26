package org.molgenis.migrate.version.v1_22;

import org.molgenis.auth.MolgenisUserMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.framework.MolgenisUpgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class Step32AddRowLevelSecurityMetadata extends MolgenisUpgrade
{
	public static final String UPDATE = "_UPDATE";
	private final Logger LOG = LoggerFactory.getLogger(Step32AddRowLevelSecurityMetadata.class);

	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	private List<String> entitiesToSecure = Collections.EMPTY_LIST;

	@Autowired
	public Step32AddRowLevelSecurityMetadata(DataSource dataSource, IdGenerator idGenerator)
	{
		super(31, 32);
		this.jdbcTemplate = new JdbcTemplate(requireNonNull(dataSource));
		this.idGenerator = requireNonNull(idGenerator);
	}

	@Override
	public void upgrade()
	{
		LOG.info("Updating entities table ...");

		// update existing settings table
		jdbcTemplate.execute("ALTER TABLE entities ADD COLUMN `rowLevelSecured` boolean");

		LOG.debug("Updated application settings");

		LOG.info("Updating entities to secure with row level security...");
		for (String fullname : entitiesToSecure)
		{
			LOG.info("Updating [{}] with row level security...", fullname);
			String rowLevelSecurityId = idGenerator.generateId();
			jdbcTemplate.update(
					"INSERT INTO attributes (`identifier`,`name`,`dataType`,`refEntity`,`expression`,`nillable`,`auto`,`visible`,`label`,`description`,`aggregateable`,`enumOptions`,`rangeMin`,`rangeMax`,`readOnly`,`unique`,`visibleExpression`,`validationExpression`,`defaultValue`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					rowLevelSecurityId, UPDATE, "xref", new MolgenisUserMetaData().getName(), null, true, false, true,
					"name", "desc", false, null, null, null, false, false, null, null, "");

			jdbcTemplate.update("INSERT INTO entities_attributes (`order`, `fullName`, `attributes`) VALUES (?, ?, ?)",
					0, fullname, rowLevelSecurityId);

			// update existing settings table
			jdbcTemplate.execute("ALTER TABLE " + fullname + " ADD COLUMN `_UPDATE` varchar(255)");
			jdbcTemplate.execute("ALTER TABLE " + fullname + " ADD CONSTRAINT `" + fullname + UPDATE + " FOREIGN KEY (`"
					+ UPDATE + "`) REFERENCES `MolgenisUser` (`ID`)");

		}
		LOG.info("Updated entities to secure with row level security...");
	}

	public void setEntitiesToSecure(List<String> entitiesToSecure)
	{
		this.entitiesToSecure = entitiesToSecure;
	}
}
