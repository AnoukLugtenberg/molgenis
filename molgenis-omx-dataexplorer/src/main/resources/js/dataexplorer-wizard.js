(function($, molgenis) {
	"use strict";
	
	molgenis.dataexplorer = molgenis.dataexplorer || {};
	var self = molgenis.dataexplorer.wizard = molgenis.dataexplorer.wizard || {};

	var restApi = new molgenis.RestClient();
	
	self.openFilterWizardModal = function(entityMetaData, attributeFilters) {
		var modal = createFilterWizardModal();
		createFilterWizardContent(entityMetaData, attributeFilters, modal);
		modal.modal('show');
	};
	
	function createFilterWizardModal() {
		var modal = $('#filter-wizard-modal');
		if(!modal.length) {
			var items = [];
			items.push('<div class="modal large hide" id="filter-wizard-modal" tabindex="-1">');
			items.push('<div class="modal-header">');
			items.push('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>');
			items.push('<h3>Filter Wizard</h3>');
			items.push('</div>');
			items.push('<div class="modal-body">');
			items.push('<div class="filter-wizard">');
			items.push('<form class="form-horizontal">');
			items.push('<ul class="wizard-steps"></ul>');
			items.push('<div class="tab-content wizard-page"></div>');
			items.push('<ul class="pager wizard">');
			items.push('<li class="previous"><a href="#">Previous</a></li>');
			items.push('<li class="next"><a href="#">Next</a></li>');
			items.push('</ul>');
			items.push('</form>');
			items.push('</div>');
			items.push('</div>');
			items.push('<div class="modal-footer">');
			items.push('<a href="#" class="btn" data-dismiss="modal">Cancel</a>');
			items.push('<a href="#" class="btn btn-primary filter-wizard-apply-btn" data-dismiss="modal">Apply</a>');
			items.push('</div>');
			items.push('</div>');
			
			modal = $(items.join(''));
			
			// workaround for "Uncaught RangeError: Maximum call stack size exceeded"
			// http://stackoverflow.com/a/19190216
			var enforceModalFocusFn = $.fn.modal.Constructor.prototype.enforceFocus;
			$.fn.modal.Constructor.prototype.enforceFocus = function() {};
			modal.on('hidden', function() {
			    $.fn.modal.Constructor.prototype.enforceFocus = enforceModalFocusFn;
			});
			
			modal.modal({'show': false});
			
			createFilterModalControls(modal);
		}
		return modal;
	}
	
	function createFilterModalControls(modal) {
		$('.filter-wizard-apply-btn', modal).click(function() {
			var filters = molgenis.dataexplorer.createFilters($('form', modal));
			if (filters.length > 0) {
				$(document).trigger('updateAttributeFilters', {
					'filters' : filters
				});
			}
		});
		
		modal.on('shown', function () {
			$('form input:visible:first', modal).focus();
		});
		
		modal.keypress(function(e) {
		    if(e.which == 13) {
		    	e.preventDefault();
		    	$('.filter-wizard-apply-btn', modal).click();
		    }
		});
	}
	
	function createFilterWizardContent(entityMetaData, attributeFilters, modal) {
		var wizard = $('.filter-wizard', modal);
		
		var listItems = [];
		var paneItems = [];
		
		var compoundAttributes = molgenis.getCompoundAttributes(entityMetaData.attributes, restApi);
		compoundAttributes.unshift(entityMetaData);
		
		$.each(compoundAttributes, function(i, compoundAttribute) {
			var tabId = compoundAttribute.name + '-tab';
			var label = compoundAttribute.label || compoundAttribute.name;
			listItems.push('<li' + (i === 0 ? ' class="active"' : '') + '><a href="#' + tabId + '" data-toggle="tab">' + label + '</a></li>');
			
			var pane = $('<div class="tab-pane' + (i === 0 ? ' active"' : '') + '" id="' + tabId + '">');
			var paneContainer = $('<div class="well"></div>');
			pane.append(paneContainer);
			$.each(compoundAttribute.attributes, function(i, attribute) {
				if(attribute.fieldType !== 'COMPOUND') {
					paneContainer.append(molgenis.dataexplorer.createFilterControls(attribute, attributeFilters[attribute.href]));
				}
			});
			paneItems.push(pane);
		});
		
		$('.wizard-steps', wizard).html(listItems.join(''));
		$('.tab-content', wizard).html(paneItems);
		
		wizard.bootstrapWizard({
	   		tabClass: 'bwizard-steps',
	   		onTabShow: function(tab, navigation, index) {
	   			var $total = navigation.find('li').length;
	   			var $current = index+1;
	   			
	   			// If it's the last tab then hide the last button and show the finish instead
	   			if($total == 1) {
	   				wizard.find('.pager').hide();
	   			} else if($current === 1) {
	   				wizard.find('.pager .previous').hide();
	   				wizard.find('.pager .next').show();
	   			} else if($current > 1 && $current < $total) {
	   				wizard.find('.pager .previous').show();
	   				wizard.find('.pager .next').show();
	   			} else {
	   				wizard.find('.pager .previous').show();
	   				wizard.find('.pager .next').hide();
	   			}
	   		}
		});
	}
})($, window.top.molgenis = window.top.molgenis || {});	