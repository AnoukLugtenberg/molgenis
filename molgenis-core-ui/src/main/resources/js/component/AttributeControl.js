/* global _: false, React: false, molgenis: true */
(function(_, React, molgenis) {
	"use strict";
	
	var api = new molgenis.RestClient();
	
	/**
	 * REST entity attribute control
	 * 
	 * @memberOf component
	 */
	var AttributeControl = React.createClass({
		mixins: [molgenis.ui.mixin.DeepPureRenderMixin, molgenis.ui.mixin.AttributeLoaderMixin],
		displayName: 'AttributeControl',
		propTypes: {
			attr: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]),
			required: React.PropTypes.bool, // optional overwrite for attr.required
			readOnly: React.PropTypes.bool, // optional overwrite for attr.readOnly
			visible: React.PropTypes.bool,  // optional overwrite for attr.visible
			disabled: React.PropTypes.bool, // optional overwrite for attr.disabled
			focus: React.PropTypes.bool,
			onValueChange: React.PropTypes.func.isRequired
		},
		getInitialState: function() {
			return {
				attr: null
			};
		},
		render: function() {	// FIXME apply focus to all controls
			if(this.state.attr === null) {
				// attribute not available yet
				return molgenis.ui.Spinner();
			}
			
			var props = this.props;
			var attr = this.state.attr;
			
			if(attr.expression === undefined) {
				// default props for all controls
				var controlProps = {
					id: props.id,
					name: attr.name,
					required : this._isRequired(),
					disabled : this._isDisabled(),
					readOnly : this._isReadOnly(),
					focus: this.props.focus,
					value : props.value,
					onValueChange : this._handleValueChange,
					onBlur : this.props.onBlur
				};
				
				switch(attr.fieldType) {
					case 'BOOL':
						return molgenis.ui.BoolControl(_.extend({}, controlProps, {
							label : props.label,
							layout : props.layout || 'horizontal',
						}));
					case 'CATEGORICAL':
						if(this.state.options === undefined) {
							// options not yet available
							return molgenis.ui.Spinner();
						}
						
						var CategoricalControl = props.multiple === true ? molgenis.ui.CheckboxGroup : molgenis.ui.RadioGroup;
						return CategoricalControl(_.extend({}, controlProps, {
							options : this.state.options,
							layout : props.layout || 'vertical',
							value : props.value !== undefined ? props.value[attr.refEntity.idAttribute] : undefined,
							onValueChange : this._handleValueChange // FIXME go from id to entity
						}));
					case 'CATEGORICAL_MREF':
						if(this.state.options === undefined) {
							// options not yet available
							return molgenis.ui.Spinner();
						}
	
						var values = props.value ? _.map(props.value.items, function(item) {
							return item[attr.refEntity.idAttribute];
						}) : [];
						return molgenis.ui.CheckboxGroup(_.extend({}, controlProps, {
							options : this.state.options,
							layout : 'vertical', // FIXME make configurable
							value : values
						}));
					case 'DATE':
						return this._createDateControl(controlProps, false, props.placeholder || 'Date');
					case 'DATE_TIME':
						return this._createDateControl(controlProps, true, props.placeholder || 'Date');
					case 'DECIMAL':
						return this._createNumberControl(controlProps, 'any');
					case 'EMAIL':
						return this._createStringControl(controlProps, 'email', props.placeholder || 'Email');
					case 'ENUM':
						if(this.state.options === undefined) {
							// options not yet available
							return molgenis.ui.Spinner();
						}
						
						var EnumControl = props.multiple === true ? molgenis.ui.CheckboxGroup : molgenis.ui.RadioGroup;
						return EnumControl(_.extend({}, controlProps, {
							options : this.state.options,
							layout : props.layout,
						}));
					case 'HTML':
						return molgenis.ui.CodeEditor({
							placeholder : this.props.placeholder,
							language: 'html'
						});
					case 'HYPERLINK':
						return this._createStringControl(controlProps, 'url', props.placeholder || 'URL');
					case 'INT':
					case 'LONG':
						return this._createNumberControl(controlProps, '1');
					case 'XREF':
						return this._createEntitySelectBox(controlProps, props.multiple || false, props.placeholder || 'Search for a Value', props.value);
					case 'MREF':
						return this._createEntitySelectBox(controlProps, props.multiple || true, props.placeholder || 'Search for Values', props.value ? props.value.items : undefined);
					case 'SCRIPT':
						return molgenis.ui.CodeEditor({
							placeholder : this.props.placeholder,
						});
					case 'STRING':
						return this._createStringControl(controlProps, 'text', props.placeholder || '');
					case 'TEXT':
						return molgenis.ui.TextArea({
							placeholder : this.props.placeholder,
						});
					case 'COMPOUND' :
					case 'FILE':
					case 'IMAGE':
						throw 'Unsupported data type: ' + attr.fieldType;
					default:
						throw 'Unknown data type: ' + attr.fieldType;
				}
			} else {
				return molgenis.ui.Input({
					type: 'text',
					disabled: true,
					placeholder: 'This value is computed automatically',
					onValueChange : function() {}
				});
			}
		},
		_handleValueChange: function(event) {
			this.props.onValueChange(_.extend({}, event, {attr: this.state.attr.name}));
		},
		_isRequired: function() {
			return this.props.required !== undefined ? this.props.required : !this.state.attr.nillable; 
		},
		_isVisible: function() {
			return this.props.visible !== undefined ? this.props.visible : this.state.attr.visible;
		},
		_isDisabled: function() {
			return this.props.disabled !== undefined ? this.props.disabled : this.state.attr.disabled;
		},
		_isReadOnly: function() {
			return this.props.readOnly !== undefined ? this.props.readOnly : this.state.attr.readOnly;
		},
		_createNumberControl: function(controlProps, step) {
			var range = this.props.attr.range;
			var min = range ? range.min : undefined;
			var max = range ? range.max : undefined;
			var placeholder = this.props.placeholder || 'Number';

			return molgenis.ui.Input(_.extend({}, controlProps, {
				type : 'number',
				placeholder : placeholder,
				step : step,
				min : min,
				max : max,
			}));
		},
		_createStringControl: function(controlProps, type, placeholder) {
			return molgenis.ui.Input(_.extend({}, controlProps, {
				type : type,
				placeholder : placeholder,
				maxlength : '255',
			}));
		},
		_createDateControl: function(controlProps, time, placeholder) {
			return molgenis.ui.DateControl(_.extend({}, controlProps, {
				placeholder : placeholder,
				time : time,
			}));
		},
		_createEntitySelectBox: function(controlProps, multiple, placeholder, value) {
			return molgenis.ui.EntitySelectBox(_.extend({}, controlProps, {
				mode: 'create',
				placeholder : placeholder,
				multiple : multiple,
				entity : this.state.attr.refEntity,
			}));
		},
		_onAttrInit: function() {
			var attr = this.state.attr;
			if(attr.fieldType === 'CATEGORICAL' || attr.fieldType === 'CATEGORICAL_MREF') {
				// retrieve all categories
				api.getAsync(attr.refEntity.href).done(function(meta) {
					var idAttr = meta.idAttribute;
					var lblAttr = meta.labelAttribute;
					
					if (this.isMounted()) {
						api.getAsync(attr.refEntity.hrefCollection, {'attributes' : [idAttr, lblAttr]}).done(function(data) { // FIXME problems in case of large number of categories
							if (this.isMounted()) {
								var options = _.map(data.items, function(entity) {
									return {value: entity[idAttr], label: entity[lblAttr]};
								});
								this.setState({options: options});
							}
						}.bind(this));	
					}
				}.bind(this));
			}
			else if(attr.fieldType === 'ENUM') {
				var options = _.map(attr.enumOptions, function(option) {
					return {value : option, label : option};
				});
				this.setState({options: options});
			}
		}
	});
	
	// export component
	molgenis.ui = molgenis.ui || {};
	_.extend(molgenis.ui, {
		AttributeControl: React.createFactory(AttributeControl)
	});
}(_, React, molgenis));