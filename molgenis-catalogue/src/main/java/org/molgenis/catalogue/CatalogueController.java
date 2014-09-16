package org.molgenis.catalogue;

import static org.molgenis.catalogue.CatalogueController.URI;
import static org.molgenis.security.core.utils.SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX;
import static org.molgenis.security.core.utils.SecurityUtils.AUTHORITY_SU;
import static org.molgenis.security.core.utils.SecurityUtils.currentUserHasRole;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.catalogue.model.ShoppingCart;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Controller
@RequestMapping(URI)
public class CatalogueController extends MolgenisPluginController
{
	private static final String SHOPPINGCART_URI = "/shoppingcart";
	public static final String ID = "catalogue";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_NAME = "view-catalogue";
	private static final String CART_VIEW_NAME = "view-shoppingcart";
	private final DataService dataService;

	@Autowired
	public CatalogueController(DataService dataService)
	{
		super(URI);
		this.dataService = dataService;
	}

	@RequestMapping
	public String showView(@RequestParam(value = "entity", required = false) String selectedEntityName, Model model)
	{
		// TODO check permission and existence of selectedEntityName
		List<EntityMetaData> emds = Lists.newArrayList();
		for (String entityName : dataService.getEntityNames())
		{
			if (currentUserHasRole(AUTHORITY_SU, AUTHORITY_ENTITY_READ_PREFIX + entityName))
			{
				emds.add(dataService.getEntityMetaData(entityName));
			}
		}

		model.addAttribute("showEntitySelect", StringUtils.isBlank(selectedEntityName));

		if ((selectedEntityName == null) && !emds.isEmpty())
		{
			selectedEntityName = emds.get(0).getName();
		}

		model.addAttribute("entitiesMeta", emds);
		model.addAttribute("selectedEntityName", selectedEntityName);

		return VIEW_NAME;
	}

	/* CART */
	@RequestMapping(value = SHOPPINGCART_URI, method = RequestMethod.POST)
	@ResponseStatus(OK)
	public void refreshShoppingCart(@Valid @RequestBody RefreshShoppingCartRequest request, HttpSession session)
	{
		ShoppingCart cart = getShoppingCart(session);
		cart.clear();
		String entityName = request.getEntityName();
		for (String attributeName : request.getAttributeNames())
		{
			cart.addAttribute(entityName, attributeName);
		}
	}

	/**
	 * Adds an attribute to the shopping cart. e.g.
	 * http://localhost:8080/menu/main/catalogue/shoppingcart/add?entityName=MolgenisUser&attributeName=userName
	 * 
	 * @param session
	 *            the user's session
	 * @param entityName
	 *            the name of the entity
	 * @param attributeName
	 *            the name of the attribute
	 * @return ShoppingCart with the current contents of the cart
	 * 
	 */
	@RequestMapping(SHOPPINGCART_URI + "/add")
	public @ResponseBody ShoppingCart addToShoppingCart(HttpSession session,
			@RequestParam(required = true) String entityName, @RequestParam(required = true) String attributeName)
	{
		return getShoppingCart(session).addAttribute(entityName, attributeName);
	}

	@RequestMapping(SHOPPINGCART_URI + "/remove")
	public @ResponseBody ShoppingCart removeFromShoppingCart(HttpSession session,
			@RequestParam(required = true) String attributeName)
	{
		return getShoppingCart(session).removeAttribute(attributeName);
	}

	@RequestMapping(SHOPPINGCART_URI + "/clear")
	public @ResponseBody ShoppingCart clearShoppingCart(HttpSession session)
	{
		return getShoppingCart(session).clear();
	}

	@RequestMapping(SHOPPINGCART_URI + "/show")
	public String showShoppingCart(@RequestParam(required = false) String entityName, Model model, HttpSession session)
	{
		final ShoppingCart cart = getShoppingCart(session);
		if (entityName != null)
		{
			cart.setEntityName(entityName);
		}
		if (cart.isEmpty())
		{
			model.addAttribute("attributes", Collections.emptyList());
		}
		else
		{
			EntityMetaData metaData = dataService.getEntityMetaData(cart.getEntityName());
			System.out.println(metaData);
			Iterable<AttributeMetaData> selectedAttributes = Iterables.filter(metaData.getAtomicAttributes(),
					new Predicate<AttributeMetaData>()
					{
						@Override
						public boolean apply(AttributeMetaData input)
						{
							return cart.getAttributes().contains(input.getName());
						}
					});
			model.addAttribute("attributes", Lists.newArrayList(selectedAttributes));

		}
		return CART_VIEW_NAME;
	}

	/**
	 * Retrieves the {@link ShoppingCart} from the session or creates one if none present.
	 * 
	 * @param session
	 *            the {@link HttpSession}
	 * @return the {@link ShoppingCart}
	 */
	private static ShoppingCart getShoppingCart(HttpSession session)
	{
		if (session.getAttribute("shoppingCart") == null)
		{
			session.setAttribute("shoppingCart", new ShoppingCart());
		}
		ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
		return cart;
	}
}
