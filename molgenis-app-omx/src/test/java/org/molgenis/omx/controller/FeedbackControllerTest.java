package org.molgenis.omx.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;
import java.util.List;

import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.controller.FeedbackControllerTest.Config;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.util.GsonHttpMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@WebAppConfiguration
@ContextConfiguration(classes = Config.class)
public class FeedbackControllerTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private FeedbackController feedbackController;

	@Autowired
	private MolgenisUserService molgenisUserService;

	@Autowired
	private JavaMailSender javaMailSender;

	private MockMvc mockMvcFeedback;

	private Authentication authentication;
	
	@BeforeMethod
	public void beforeMethod()
	{
		mockMvcFeedback = MockMvcBuilders.standaloneSetup(feedbackController)
				.setMessageConverters(new GsonHttpMessageConverter()).build();
		authentication = new TestingAuthenticationToken("userName", null);
		authentication.setAuthenticated(true);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Test
	public void initFeedbackAnonymous() throws Exception
	{
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymous", null));

		List<String> adminEmails = Collections.singletonList("molgenis@molgenis.org");
		when(this.molgenisUserService.getSuEmailAddresses()).thenReturn(adminEmails);
		verify(molgenisUserService, never()).getUser("anonymous");

		mockMvcFeedback.perform(MockMvcRequestBuilders.get(FeedbackController.URI))
				.andExpect(MockMvcResultMatchers.status().isOk()).andExpect(view().name("view-feedback"))
				.andExpect(model().attribute("adminEmails", adminEmails))
				.andExpect(model().attributeDoesNotExist("userName"))
				.andExpect(model().attributeDoesNotExist("userEmail"));

	}

	@Test
	public void initFeedbackLoggedInNameKnown() throws Exception
	{
		List<String> adminEmails = Collections.singletonList("molgenis@molgenis.org");
		MolgenisUser user = new MolgenisUser();
		user.setFirstName("First");
		user.setLastName("Last");
		user.setEmail("user@blah.org");
		when(this.molgenisUserService.getUser("userName")).thenReturn(user);
		when(this.molgenisUserService.getSuEmailAddresses()).thenReturn(adminEmails);
		mockMvcFeedback.perform(MockMvcRequestBuilders.get(FeedbackController.URI))
				.andExpect(MockMvcResultMatchers.status().isOk()).andExpect(view().name("view-feedback"))
				.andExpect(model().attribute("adminEmails", adminEmails))
				.andExpect(model().attribute("userName", "First Last"))
				.andExpect(model().attribute("userEmail", "user@blah.org"));
	}

	@Test
	public void initFeedbackLoggedInDetailsNotSpecified() throws Exception
	{
		List<String> adminEmails = Collections.singletonList("molgenis@molgenis.org");
		MolgenisUser user = new MolgenisUser();
		when(this.molgenisUserService.getUser("userName")).thenReturn(user);
		when(this.molgenisUserService.getSuEmailAddresses()).thenReturn(adminEmails);
		mockMvcFeedback.perform(MockMvcRequestBuilders.get(FeedbackController.URI))
				.andExpect(MockMvcResultMatchers.status().isOk()).andExpect(view().name("view-feedback"))
				.andExpect(model().attribute("adminEmails", adminEmails))
				.andExpect(model().attributeDoesNotExist("userName"))
				.andExpect(model().attributeDoesNotExist("userEmail"));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public FeedbackController feedbackController()
		{
			return new FeedbackController();
		}

		@Bean
		public MolgenisUserService molgenisUserService()
		{
			return mock(MolgenisUserService.class);
		}

		@Bean
		public JavaMailSender mailSender()
		{
			return mock(JavaMailSender.class);
		}
	}
}
