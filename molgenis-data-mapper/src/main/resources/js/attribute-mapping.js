(function($, molgenis) {
	"use strict";

	/**
	 * Generate an algorithm based on category selections
	 * 
	 * @param mappedCategoryIds
	 *            a list of category identifiers
	 * @param attribute
	 *            the source attribute
	 * @param defaultValue
	 *            The value used as a default value
	 * @param nullValue
	 *            The value used for missing
	 */
	function generateAlgorithm(mappedCategoryIds, attribute, defaultValue, nullValue) {
		var algorithm;
		if (nullValue !== undefined) {
			algorithm = "$('" + attribute + "').map(" + JSON.stringify(mappedCategoryIds) + ", " + JSON.stringify(defaultValue) + ", " + JSON.stringify(nullValue)
					+ ").value();";
		} else if (defaultValue !== undefined) {
			algorithm = "$('" + attribute + "').map(" + JSON.stringify(mappedCategoryIds) + ", " + JSON.stringify(defaultValue) + ").value();";
		} else {
			algorithm = "$('" + attribute + "').map(" + JSON.stringify(mappedCategoryIds) + ").value();";
		}
		return algorithm;
	}

	/**
	 * Sends an algorithm to the server for testing.
	 * 
	 * @param algorithm
	 *            the algorithm string to send to the server
	 */
	function testAlgorithm(algorithm) {
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/mappingattribute/testscript',
			data : JSON.stringify({
				targetEntityName : $('input[name="target"]').val(),
				sourceEntityName : $('input[name="source"]').val(),
				targetAttributeName : $('input[name="targetAttribute"]').val(),
				algorithm : algorithm
			}),
			contentType : 'application/json',
			success : showStatistics
		});
	}

	/**
	 * Shows statistics for the test results.
	 * 
	 * @param data
	 *            the results from the server
	 */
	function showStatistics(data) {
		if (data.results.length === 0) {
			$('#statistics-container').hide();
			molgenis.createAlert([ {
				'message' : 'No valid cases are produced by the algorithm. TIP: Maybe your data set is empty.'
			} ], 'warning');
		}

		$('#stats-total').text(data.totalCount);
		$('#stats-valid').text(data.results.length);
		$('#stats-mean').text(jStat.mean(data.results));
		$('#stats-median').text(jStat.median(data.results));
		$('#stats-stdev').text(jStat.stdev(data.results));

		$('#statistics-container').show();
		if ($('.distribution').length) {
			$('.distribution').bcgraph(data.results);
		}
	}

	/**
	 * Searches the source attributes in an algorithm string.
	 * 
	 * @param algorithm
	 *            the algorithm string to search
	 */
	function getSourceAttrs(algorithm) {
		var regex = /\$\(['"]([^\$\(\)]+)['"]\)/g, match, result = [];

		while ((match = regex.exec(algorithm))) {
			if (match) {
				result.push(match[1]);
			}
		}
		return result;
	}

	/**
	 * Load result table from view-attribute-mapping-feedback.ftl
	 * 
	 * @param algorithm
	 *            the algorithm that is send to the server to apply over the
	 *            submitted source values
	 */
	function loadAlgorithmResult(algorithm) {
		$("#result-table-container").load("attributemappingfeedback #algorithm-result-feedback-container", {
			mappingProjectId : $('input[name="mappingProjectId"]').val(),
			target : $('input[name="target"]').val(),
			source : $('input[name="source"]').val(),
			targetAttribute : $('input[name="targetAttribute"]').val(),
			algorithm : algorithm
		}, function() {
			$('.show-error-message').on('click', function() {
				// $(this).append($(this).data('message'));
				$('#algorithm-error-message-container').html($(this).data('message'));
			});
		});
	}

	/**
	 * Load mapping table from view-advanced-mapping-editor.ftl
	 * 
	 * @param algorithm
	 *            The algorithm to set presets when opening the editor a second
	 *            time
	 */
	function loadMappingEditor(algorithm) {
		$("#advanced-mapping-table").load("advancedmappingeditor #advanced-mapping-editor", {
			mappingProjectId : $('input[name="mappingProjectId"]').val(),
			target : $('input[name="target"]').val(),
			source : $('input[name="source"]').val(),
			targetAttribute : $('input[name="targetAttribute"]').val(),
			// TODO mapping editor for > 1 attribute
			sourceAttribute : getSourceAttrs(algorithm)[0],
			algorithm : algorithm
		});
	}

	/**
	 * Selects the attributes mentioned in the algorithm
	 * 
	 * @param algorithm
	 *            the algorithm string
	 */
	function checkSelectedAttributes(algorithm) {
		var sourceAttrs = getSourceAttrs(algorithm);
		$('input:checkbox').each(function(index, value) {
			var name = $(this).attr('class'), inArray = $.inArray(name, sourceAttrs);
			$(this).prop('checked', inArray >= 0);
		});
	}

	/**
	 * Clears the editor and inserts selected attributes.
	 * 
	 * @param attribute
	 *            the name of the attribute
	 * @param editor
	 *            the ace algorithm editor to insert the attribute into
	 */
	function insertSelectedAttributes(selectedAttributes, editor) {
		editor.setValue(""); // clear the editor
		$(selectedAttributes).each(function() {
			editor.insert("$('" + this + "').value();", -1);
		});
	}

	$(function() {

		var editor, searchQuery, selectedAttributes, $textarea, initialValue, algorithm, feedBackRequest, row, targetAttributeDataType;

		// create ace editor
		$textarea = $("#ace-editor-text-area");
		initialValue = $textarea.val();
		$textarea.ace({
			options : {
				enableBasicAutocompletion : true
			},
			readOnly : $textarea.data('readonly') === true,
			theme : 'eclipse',
			mode : 'javascript',
			showGutter : true,
			highlightActiveLine : false
		});
		editor = $textarea.data('ace').editor;

		// on load use algorithm to set selected attributes and editor value
		checkSelectedAttributes(initialValue);
		algorithm = editor.getSession().getValue();

		editor.getSession().on('change', function() {
			// check attributes if manually added
			checkSelectedAttributes(editor.getValue());

			// update algorithm
			algorithm = editor.getSession().getValue();

			// update result
			loadAlgorithmResult(algorithm);
		});

		// if there is an algorithm present on load, show the result table
		if (algorithm.trim()) {
			loadAlgorithmResult(algorithm);
		} else {
			// if no algorithm present hide the mapping and result containers
			$('#attribute-mapping-container').css('display', 'none');
			$('#result-container').css('display', 'none');
		}

		// save button for saving generated mapping
		$('#save-mapping-btn').on('click', function() {
			$.post(molgenis.getContextUrl() + "/saveattributemapping", {
				mappingProjectId : $('input[name="mappingProjectId"]').val(),
				target : $('input[name="target"]').val(),
				source : $('input[name="source"]').val(),
				targetAttribute : $('input[name="targetAttribute"]').val(),
				algorithm : algorithm
			}, function() {
				molgenis.createAlert([ {
					'message' : 'Succesfully saved the created mapping'
				} ], 'success');
			});
		});

		$('#attribute-mapping-table :checkbox').on('change', function() {
			var $checkedAttributes = $('#attribute-mapping-table :checkbox:checked');
			var amountChecked = $checkedAttributes.length;
			if (this.checked) {

			}

		});
		// test button for simple attribute selection
		$('#test-mapping-btn').on('click', function() {
			selectedAttributes = [];

			// for every checkbox that is checked, get the source.name
			$('#attribute-mapping-table').find('tr').each(function() {
				row = $(this);
				if (row.find('input[type="checkbox"]').is(':checked')) {
					selectedAttributes.push(row.attr('class'));
				}
			});

			// attributes into editor
			insertSelectedAttributes(selectedAttributes, editor);

			// updates algorithm
			algorithm = editor.getSession().getValue();

			// generate result table
			loadAlgorithmResult(algorithm);

			// generate mapping editor if target attribute is an xref or
			// categorical
			targetAttributeDataType = $('input[name="targetAttributeType"]').val();
			if (targetAttributeDataType === 'xref' || targetAttributeDataType === 'categorical') {
				loadMappingEditor(algorithm);
			}

			// on selection of an attribute, show all fields
			$('#attribute-mapping-container').css('display', 'inline');
			$('#result-container').css('display', 'inline');
		});

		// only show selected attributes in table when checked
		$('#selected-only-checkbox').on('click', function() {
			var checkedAttributes;

			if ($(this).is(':checked')) {
				$('#attribute-mapping-table').find('input[type="checkbox"]:checked').each(function() {
					checkedAttributes = $(this).attr('class');
				});

			} else {
				// show all attributes
			}
		});

		// look for attributes in the attribute table
		$('#attribute-search-btn').on('click', function() {
			searchQuery = $('#attribute-search-field').val();
			// use the value of attribute-search-field to apply a filter on the
			// attribute-mapping-table
		});

		// when the map tab is selected, load its contents
		// loading on page load will fail because bootstrap tab blocks it
		$('a[href=#map]').on('shown.bs.tab', function() {
			loadMappingEditor(algorithm);
		});

		$('a[href=#script]').on('shown.bs.tab', function() {
			// Clearing the editor will empty the algorithm
			var newAlgorithm = algorithm;
			editor.setValue("");
			editor.insert(newAlgorithm, -1);
		});

		$('#advanced-mapping-table').on('change', function() {
			var mappedCategoryIds = {}, defaultValue = undefined, nullValue = undefined, key, val;

			// for each source xref value, check which target xref value
			// was chosen
			$('#advanced-mapping-table > tbody > tr').each(function() {
				key = $(this).attr('id');
				val = $(this).find('option:selected').val();
				if (key === 'nullValue') {
					if (val !== 'use-default-option') {
						if (val === 'use-null-value') {
							nullValue = null;
						} else {
							nullValue = val;
						}
					}
				} else {
					if (val !== 'use-default-option') {
						if (val === 'use-null-value') {
							mappedCategoryIds[$(this).attr('id')] = null;
						} else {
							mappedCategoryIds[$(this).attr('id')] = val;
						}
					}
				}
			});

			if (nullValue !== undefined) {
				defaultValue = null;
			}

			if ($('#default-value').is(":visible")) {
				defaultValue = $('#default-value').find('option:selected').val();
				if (defaultValue === 'use-null-value') {
					defaultValue = null;
				}
			}

			algorithm = generateAlgorithm(mappedCategoryIds, $('input[name="sourceAttribute"]').val(), defaultValue, nullValue);
			loadAlgorithmResult(algorithm);
		});
	});

}($, window.top.molgenis = window.top.molgenis || {}));