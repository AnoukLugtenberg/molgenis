<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=['molgenis-form.css']>
<#assign js=['jquery.validate.min.js']>

<@header css/>

<#if submitted??>
	<div class="hero-unit">
		<h1>Thanks!</h1>
		<p>Thank you for your feedback.</p>
	</div>
<#elseif adminEmails?has_content>
	<div class="page-header">
		<h3>We&rsquo;re here to help with any questions or comments.<br/>
		<small>If you just want to say hi, that&rsquo;s cool too.</small>
	</div>
	<div class="container">
		<form accept-charset="UTF-8" method="post" action="feedback" id="feedbackForm">
			<fieldset>
				<#assign adminEmailsString = "" />
				<#list adminEmails as adminEmail>
					<#assign adminEmailsString = adminEmailsString + adminEmail/>
   					<#if adminEmail_has_next>
   						<#assign adminEmailsString = adminEmailsString + ';'/>
   					</#if>
				</#list>
				<p>
					Feel free to email us at <a href='mailto:${adminEmailsString}'>${adminEmailsString}</a>
				</p>
				<label class="control-label" for="form_name">Name</label>
				<#if userName??>
					<span class="input-xlarge uneditable-input">${userName}</span>
					<input type="hidden" name="form[name]" value="${userName}"/>
				<#else>
					<input
						class="input-xlarge" name="form[name]" size="30"
						type="text" />
				</#if>
				<label class="control-label" for="form_email">Email</label>
				<#if userEmail??>
					<span class="input-xlarge uneditable-input">${userEmail}</span>
					<input type="hidden" name="form[email]" value="${userEmail}"/>
				<#else>
					<input class="input-xlarge" name="form[email]"
						size="30" type="email" />
				</#if>
				<label class="control-label"
					for="form_subject">Subject</label>
				<input class="input-xxlarge"
					maxlength="72" name="form[subject]" size="72"
					type="text" />
				<label class="control-label" for="form_body">Body</label>
				<textarea class="input-xxlarge" name="form[comments]"
					placeholder="If you use 140 characters or fewer, we&rsquo;ll give you a gold star."
					required="true" rows="10"></textarea>
			</fieldset>
			<button type="submit" class="btn btn-success">Send</button>
		</form>
		<script>
			$("#feedbackForm").validate();
		</script>
	</div>
<#else>
	<p>Admin email addresses not known.</p>
</#if>
<@footer/>
