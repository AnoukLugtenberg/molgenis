package org.molgenis.data.jobs;

import static com.google.common.collect.Lists.newArrayList;
import static org.molgenis.MolgenisFieldTypes.DATETIME;
import static org.molgenis.MolgenisFieldTypes.HYPERLINK;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.MolgenisFieldTypes.STRING;
import static org.molgenis.MolgenisFieldTypes.TEXT;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.meta.RootSystemPackage.PACKAGE_SYSTEM;

import java.util.List;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.molgenis.fieldtypes.EnumField;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionMetaData extends SystemEntityMetaDataImpl
{
	private static final String SIMPLE_NAME = "JobExecution";
	public static final String JOB_EXECUTION = PACKAGE_SYSTEM + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier"; // Job ID
	public static final String USER = "user"; // Owner of the job
	public static final String STATUS = "status"; // Job status like running or failed
	public static final String TYPE = "type"; // Job type like ImportJob
	public static final String SUBMISSION_DATE = "submissionDate";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String PROGRESS_INT = "progressInt"; // Number of processed entities
	public static final String PROGRESS_MESSAGE = "progressMessage";
	public static final String PROGRESS_MAX = "progressMax"; // Max number of entities to process
	public static final String LOG = "log";
	public static final String RESULT_URL = "resultUrl";
	public static final String SUCCESS_EMAIL = "successEmail";
	public static final String FAILURE_EMAIL = "failureEmail";

	private final List<String> jobStatusOptions = newArrayList("PENDING", "RUNNING", "SUCCESS", "FAILED", "CANCELED");

	JobExecutionMetaData()
	{
		super(SIMPLE_NAME, PACKAGE_SYSTEM);
	}

	@Override
	public void init()
	{
		setAbstract(true);
		addAttribute(IDENTIFIER, ROLE_ID).setLabel("Job ID").setAuto(true).setNillable(false);
		addAttribute(USER).setDataType(MolgenisFieldTypes.STRING).setLabel("Job owner").setNillable(false);
		addAttribute(STATUS).setDataType(new EnumField()).setEnumOptions(jobStatusOptions).setLabel("Job status")
				.setNillable(false);
		addAttribute(TYPE).setDataType(STRING).setLabel("Job type").setNillable(false);
		addAttribute(SUBMISSION_DATE).setDataType(DATETIME).setLabel("Job submission date").setNillable(false);
		addAttribute(START_DATE).setDataType(DATETIME).setLabel("Job start date").setNillable(true);
		addAttribute(END_DATE).setDataType(DATETIME).setLabel("Job end date").setNillable(true);
		addAttribute(PROGRESS_INT).setDataType(INT).setLabel("Progress").setNillable(true);
		addAttribute(PROGRESS_MAX).setDataType(INT).setLabel("Maximum progress").setNillable(true);
		addAttribute(PROGRESS_MESSAGE).setDataType(STRING).setLabel("Progress message").setNillable(true);
		addAttribute(LOG).setDataType(TEXT).setLabel("Log").setNillable(true);
		addAttribute(RESULT_URL).setDataType(HYPERLINK).setLabel("Result URL").setNillable(true);
		addAttribute(FAILURE_EMAIL).setDataType(STRING).setLabel("Failure email")
				.setDescription("Comma-separated email addresses to send email to if execution fails or is canceled")
				.setNillable(true);
		addAttribute(SUCCESS_EMAIL).setDataType(STRING).setLabel("Success email")
				.setDescription("Comma-separated email addresses to send email to if execution succeeds")
				.setNillable(true);
	}
}