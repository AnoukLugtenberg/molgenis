package org.molgenis.rdconnect;

import static java.util.Objects.requireNonNull;
import static org.molgenis.rdconnect.BiobankMetadataController.URI;

import org.molgenis.data.DataService;
import org.molgenis.ui.MolgenisPluginController;
import org.molgenis.util.ErrorMessageResponse;
import org.molgenis.util.ErrorMessageResponse.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(URI)
public class BiobankMetadataController extends MolgenisPluginController
{
	private static final Logger LOG = LoggerFactory.getLogger(BiobankMetadataController.class);

	public static final String ID = "biobankmeta";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private final BiobankMetadataService biobankMetadataService;
	private final DataService dataService;

	@Autowired
	public BiobankMetadataController(DataService dataService, BiobankMetadataService biobankMetadataService)
	{
		super(URI);
		this.biobankMetadataService = requireNonNull(biobankMetadataService);
		this.dataService = requireNonNull(dataService);
	}

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAnyRole('ROLE_SU')")
	public String init(Model model) throws Exception
	{
		return "view-biobankrefresh";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/refresh")
	@PreAuthorize("hasAnyRole('ROLE_SU')")
	public String refreshMetadata(Model model) throws Exception
	{
		biobankMetadataService.getIdCardBiobanks().forEach(e -> dataService.add("regbb", e));
		return init(model);
	}

	@ExceptionHandler(value = Throwable.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessageResponse handleThrowable(Throwable t)
	{
		LOG.error("", t);
		return new ErrorMessageResponse(new ErrorMessage(t.getMessage()));
	}
}
