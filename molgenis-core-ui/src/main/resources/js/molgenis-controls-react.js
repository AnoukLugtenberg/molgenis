/** @jsx React.DOM */
/**
 * MOLGENIS attribute controls for all data types
 * 
 * Dependencies: TODO
 * 
 * @param $
 * @param molgenis
 */
(function($, molgenis) {
	"use strict";
	
	molgenis.controls = molgenis.controls || {};

	var api = new molgenis.RestClient();
	
	/**
	 * Only render components if their state or props changed
	 */
	var DeepPureRenderMixin = {
		shouldComponentUpdate: function(nextProps, nextState) {
			return !_.isEqual(this.props, nextProps) || !_.isEqual(this.state, nextState);
		}
	};
	
	/**
	 * React component for Select2
	 */
	var Select2Component = React.createClass({
		mixins: [DeepPureRenderMixin],
		getInitialState: function() {
			return {value: this.props.value};
		},
		componentWillReceiveProps : function(nextProps) {
			this.setState({value: nextProps.value});
		},
		componentDidMount: function() {console.log('componentDidMount Select2Component');
			var $container = $(this.refs.select2.getDOMNode());
			
			// create select2
			$container.select2($.extend({
					containerCssClass: 'form-control',
					placeholder : ' ', // cannot be an empty string
					minimumResultsForSearch: -1, // permanently hide the search field
					width: '100%'
				}, this.props.options));

			// create event select2 handlers
			var self = this;
			$container.on('change', function() {
				self._handleChange($container.select2('data'));
			});

			// initialize select2
			this._updateSelect2();
		},
		componentWillUnmount: function() {console.log('componentWillUnmount Select2Component');
			var $container = $(this.refs.select2.getDOMNode());
			$container.off();
			$container.select2('destroy');
		},
		render: function() {console.log('render Select2Component', this.state, this.props);
			if (this.isMounted()) {
				this._updateSelect2();
			}
			return(<input type="hidden" ref="select2" />);
		},
		_handleChange: function(value) {console.log('_handleChange Select2Component', value);
			this.setState({value: value});
			this.props.onChange(value);
		},
		_updateSelect2: function() {
			var $container = $(this.refs.select2.getDOMNode());
			console.log('_updateSelect2', this.state);
			$container.select2('data', this.state.value);
			$container.select2('enable', !this.props.disabled);
		}
	});
	
	/**
	 * React component for jQRangeSlider
	 */
	var JQRangeSliderComponent = React.createClass({
		mixins: [DeepPureRenderMixin],
		componentDidMount: function() {console.log('componentDidMount JQRangeSliderComponent');
			var $container = $(this.refs.rangeslider.getDOMNode());
			$container.editRangeSlider(this.props.options);

			if(this.props.disabled) {
				$container.editRangeSlider('disable');
			}

			var props = this.props;
			$container.on('userValuesChanged', function(e, data) {
				props.onChange({value: [data.values.min, data.values.max]});
			});
		},
		componentWillUnmount: function() {console.log('componentWillUnmount JQRangeSliderComponent');
			var $container = $(this.refs.rangeslider.getDOMNode());
			$container.off();
			$container.editRangeSlider('destroy');
		},
		render: function() {console.log('render JQRangeSliderComponent', this.state, this.props);
			if(this.isMounted()) {
				var $container = $(this.refs.rangeslider.getDOMNode());
				$container.editRangeSlider(this.props.disabled ? 'disable' : 'enable');
				$container.editRangeSlider('values', this.props.value[0], this.props.value[1]);
			}
			return(
				<div ref="rangeslider"></div>		
			);
		}
	});
	
	/**
	 * React component for datetimepicker
	 */
	var DateTimePickerComponent = React.createClass({
		mixins: [DeepPureRenderMixin],
		componentDidMount: function() {console.log('componentDidMount DateTimePickerComponent');
			var props = this.props;

			var format = props.time ? 'YYYY-MM-DDTHH:mm:ssZZ' : 'YYYY-MM-DD';

			var $container = $(this.refs.datepicker.getDOMNode());
			$container.datetimepicker({
				format: format
			});

			$container.on('dp.change', function(event) {console.log('event.date', event.date.format(props.time));
				props.onChange({value: event.date.format(format)});
			});

			var $clearBtn = $(this.refs.clearbtn.getDOMNode());
			$clearBtn.on('click', function() {
				props.onChange({value: undefined});
			});
		},
		componentWillUnmount: function() {console.log('componentWillUnmount DateTimePickerComponent');
			var $container = $(this.refs.datepicker.getDOMNode());
			$container.datetimepicker('destroy');
		},
		render: function() {console.log('render DateTimePickerComponent', this.state, this.props);
			var placeholder = this.props.placeholder;
			var required = this.props.required;
			var disabled = this.props.disabled;

			return(
				<div className="input-group date group-append" ref="datepicker">
					<input type="text" className="form-control" value={this.props.value}
		    			placeholder={placeholder} required={required} disabled={disabled} />
		    		<span className="input-group-addon">
						<span className="glyphicon glyphicon-remove empty-date-input" ref="clearbtn"></span>
					</span>
					<span className="input-group-addon datepickerbutton">
						<span className="glyphicon glyphicon-calendar"></span>
					</span>
				</div>
			);
		},
	});

	/**
	 * Range slider control for number types 
	 */
	var RangeSliderControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render RangeSliderControl', this.state, this.props);
			var range = this.props.range;
			var value = this.props.value;
			
			var fromValue = value && value[0] ? value[0] : range.min;
			var toValue = value && value[1] ? value[1] : range.max;
			var options = {
				symmetricPositionning: true,
				bounds: {min: range.min, max: range.max},
				defaultValues: {min: fromValue, max: toValue},
				step: this.props.step,
				type: 'number'
			}
			return(
				<JQRangeSliderComponent options={options} disabled={this.props.disabled} value={[fromValue, toValue]} onChange={this._handleChange} />
			);
		},
		_handleChange: function(event) {console.log('_handleChange RangeSliderControl', event);
			this.props.onValueChange({value: event.value});
		}
	});
	
	/**
	 * Input control for string and number types
	 */
	var InputControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		getInitialState: function() {
			return {value: this.props.value};
		},
		componentWillReceiveProps : function(nextProps) {
			this.setState({
				value: nextProps.value
			});
		},
		getDefaultProps: function() {
			return {
				disabled: false,
				required: true
			};
		},
		render: function() {
			var type = this.props.type;
			var placeholder = this.props.placeholder;
			var required = this.props.required;
			var disabled = this.props.disabled;
			var step = this.props.step;
			var min = this.props.min;
			var max = this.props.max;
			var maxlength = this.props.maxlength;
			
			var value = this.state.value;
		    return (
	    		<input type={type} className="form-control"
	    			placeholder={placeholder} required={required} disabled={disabled} step={step} min={min} max={max}
	    			value={value} onChange={this._handleChange} />
		    );
		},
		_handleChange: function(event) {console.log('_handleChange InputControl', event);
			var value = event.target.value;
			if(this.props.type === 'number') {console.log('value', value);
				if(value.length > 0 && value.charAt(value.length - 1) === '.') {
					return;
				} else if(value !== '') {
					value = parseFloat(value);
				}
			}
			this.props.onValueChange({value: value});
		}
	});
	
	/**
	 * Input control for BOOL type with checkbox or radio buttons
	 */
	var BoolControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render BoolControl', this.state, this.props);
			if(this.props.nillable || this.props.layout === 'radio') {
				return <BoolRadioControl {...this.props} />
			} else {
				return <BoolCheckboxControl {...this.props} />
			}
		}
	});
	
	/**
	 * Input control for BOOL type with radio buttons for different states
	 */
	var BoolRadioControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		getInitialState: function() {
			return {
				value: this._valueToInputValue(this.props.value)
			};
		},
		componentWillReceiveProps : function(nextProps) {
			this.setState({
				value: this._valueToInputValue(nextProps.value)
			});
		},
		render: function() {console.log('render BoolRadioControl', this.state, this.props);
			var states = [{value: 'true', label: 'True'}, {value: 'false', label: 'False'}];
			if(this.props.nillable) {
				states.push({value: 'null', label: 'N/A'});
			}

			var self = this;
			var inputs = $.map(states, function(state) {
				return (
					<label className="radio-inline">
						<input type="radio" name="bool-radio" checked={state.value === self.state.value} disabled={self.props.disabled} value={state.value} onChange={self._handleChange} /> {state.label}
					</label>
				);	
			});
			return (<div>{inputs}</div>);
		},
		_handleChange: function(event) {console.log('_handleChange BoolRadioControl', event);
			var value = this._inputToValue(event.target.value);
			this.setState({value: value});
			this.props.onValueChange({value: value});
		},
		_valueToInputValue: function(value) {
			if(value === 'true' || value === true)
				return 'true';
			else if(value === 'false' || value === false)
				return 'false';
			else if(value === 'null' || value === null)
				return 'null';
			else
				return undefined;
		},
		_inputToValue: function(value) {
			return value === 'true' ? true : (value === 'false' ? false : (value === 'null' ? null : undefined));
		}
	});

	/**
	 * Input control for BOOL type with radio buttons for different states
	 */
	var BoolCheckboxControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render BoolCheckboxControl', this.state, this.props);
			var label = this.props.label || '\u00A0'; // workaround for https://github.com/facebook/react/issues/183
		    return (
		    		<div className="checkbox">
		    			<label>
		    				<input type="checkbox" disabled={this.props.disabled} checked={this.props.value === true} onChange={this._handleChange} /> {label}   
	    				</label>
		    		</div>
		    );
		},
		_handleChange: function(event) {console.log('_handleChange BoolCheckboxControl', event);
			var checked = event.target.checked;
			this.setState({value: checked});
			if(this.props.onValueChange) {
				this.props.onValueChange({value: checked});
			}
		}
	});

	/**
	 * Input control for DATE and DATE_TIME types
	 */
	var DateControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		getInitialState: function() {
			return {value: this.props.value};
		},
		componentWillReceiveProps : function(nextProps) {
			this.setState({
				value: nextProps.value
			});
		},
		render: function() {console.log('render DateControl', this.state, this.props);
			return <DateTimePickerComponent {...this.props} value={this.state.value} onChange={this._handleChange} />
		},
		_handleChange: function(value) {console.log('_handleChange DateControl', value);
			this.setState(value);
			this.props.onValueChange(value);
		}
	});

	var EnumControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render EnumControl', this.state, this.props);
			var data = this.props.options.map(function(option) {
				return {id: option, text: option};
			});
			
			var options = {
				enable: !this.props.disabled,
				multiple: this.props.multiple,
				placeholder : this.props.placeholder || ' ', // cannot be an empty string
				initSelection : function (element, callback) {
					if(value) {
						callback(value);
					}
				},
				query: function(query) {
					query.callback({results: data});
		        }
			};

			var value;
			if(this.props.value) {
				if(this.props.multiple) {
					value = $.map(this.props.value, function(option) {
						return {id: option, text: option};
					});
				} else {
					value = {id: this.props.value, text: this.props.value}
				}
			}

			return (
				<Select2Component options={options} disabled={this.props.disabled} value={value} onChange={this._handleChange} />
			);
		},
		_handleChange: function(value) {console.log('_handleChange EnumControl', value);
			var value = value !== '' ? value : null;
			this.props.onValueChange({value: value});
		}
	});
	
	var CategoricalControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		getInitialState: function() {
			// initialize with entity meta if exists
			var entity = this.props.entity;
			return {
				entity: entity.idAttribute !== undefined ? entity : null
			};
		},
		componentDidMount: function() {console.log('componentDidMount CategoricalControl');
			// fetch entity meta if not exists
			var entity = this.props.entity;
			if(entity.idAttribute === undefined) {
				var self = this;
				api.getAsync(entity.href).done(function(entity) {
					if (self.isMounted()) {
						self.setState({entity: entity});
					}
				});	
			}
		},
		render: function() {console.log('render CategoricalControl', this.state, this.props);
			if(this.state.entity === null) {
				// entity meta data not fetched yet
				return (<div></div>);
			}
			
			var props = this.props;
			var entity = this.state.entity;
			
			var format = function(item) {
				if (item)
					return item[entity.labelAttribute];
			};
			
			var self = this;
			var options = {
				enable: !this.props.disabled,
				id: entity.idAttribute,
				multiple: props.multiple,
				allowClear : props.nillable ? true : false,
				placeholder : props.placeholder || ' ', // cannot be an empty string
				initSelection: function(element, callback) {
					if(self.props.value)
						callback([self.props.value]);
				},
			    query: function (query) {
			    	var num = 25;
				    var q = {
						q : {
							start : (query.page - 1) * num, 
							num : num
						}
					};
			    	
			    	api.getAsync(entity.hrefCollection, q).done(function(data) {
			    		query.callback({results: data.items, more: data.nextHref ? true : false});
			    	});
			    },
			    formatResult: format,
			    formatSelection: format
			};
			return (
				<Select2Component options={options} disabled={this.props.disabled} value={this.props.value} onChange={this._handleChange} />
			);
		},
		_handleChange: function(value) {console.log('_handleChange CategoricalControl', value);
			var value = this.props.multiple && value.length === 0 ? undefined : value;
			this.props.onValueChange({value: value});
		}
	});
	
	var TextControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render TextControl', this.state, this.props);
			return (<textarea className="form-control" placeholder={this.props.placeholder} required={this.props.required} disabled={this.props.disabled} onChange={this._handleChange}>{this.props.value}</textarea>);
		},
		_handleChange: function(value) {console.log('_handleChange TextControl', value);
			var value = value !== '' ? value : null;
			this.props.onValueChange({value: value});
		}
	});
		
	var AttributeControl = React.createClass({
		mixins: [DeepPureRenderMixin],
		render: function() {console.log('render AttributeControl', this.state, this.props);
			var props = this.props;
			var attr = props.attr;
			
			switch(attr.fieldType) {
				case 'BOOL':
					var layout = props.layout || 'checkbox';
					return <BoolControl label={props.label} nillable={attr.nillable} disabled={props.disabled} layout={layout} value={props.value} onValueChange={this._handleValueChange} />
				case 'CATEGORICAL':
					var placeholder = props.placeholder || 'Select a Category';
					var multiple = props.multiple || false;
					return this._createEntityControl(multiple, placeholder);
				case 'DATE':
					var placeholder = props.placeholder || 'Date';
					return this._createDateControl(false, placeholder);
				case 'DATE_TIME':
					var placeholder = props.placeholder || 'Date';
					return this._createDateControl(true, placeholder);
				case 'DECIMAL':
					return this._createNumberControl('any');
				case 'EMAIL':
					var placeholder = props.placeholder || 'Email';
					return this._createStringControl('email', placeholder);
				case 'ENUM':
					var placeholder = props.placeholder || 'Select an Option';
					var multiple = props.multiple || false;
					return <EnumControl multiple={multiple} placeholder={placeholder} required={props.required} disabled={props.disabled} options={attr.enumOptions} value={props.value} onValueChange={this._handleValueChange} />
				case 'HTML':
					return this._createTextControl();
				case 'HYPERLINK':
					var placeholder = props.placeholder || 'URL';
					return this._createStringControl('url', placeholder);
				case 'INT':
				case 'LONG':
					return this._createNumberControl('1');
				case 'XREF':
					var placeholder = props.placeholder || 'Search for a Value';
					var multiple = props.multiple || false;
					return this._createEntityControl(multiple, placeholder);
				case 'MREF':
					var placeholder = props.placeholder || 'Search for Values';
					var multiple = props.multiple || true;
					return this._createEntityControl(multiple, placeholder);
				case 'SCRIPT':
					return this._createTextControl();
				case 'STRING':
					var placeholder = props.placeholder || '';
					return this._createStringControl('text', placeholder);
				case 'TEXT':
					return this._createTextControl();
				case 'COMPOUND' :
				case 'FILE':
				case 'IMAGE':
					throw 'Unsupported data type: ' + attr.fieldType;
				default:
					throw 'Unknown data type: ' + attr.fieldType;
			}
		},
		_handleValueChange: function(event) {console.log('_handleChange AttributeControl', event);
			this.props.onValueChange({attr: this.props.attr.name, value: event.value});
		},
		_createNumberControl: function(step) {
			var min = this.props.range ? this.props.range.min : undefined;
			var max = this.props.range ? this.props.range.max : undefined;
			var placeholder = this.props.placeholder || 'Number';
			return <InputControl type="number" placeholder={placeholder} required={this.props.required} disabled={this.props.disabled} step={step} min={min} max={max} value={this.props.value} onValueChange={this._handleValueChange} />
		},
		_createStringControl: function(type, placeholder) {
			return <InputControl type={type} placeholder={placeholder} required={this.props.required} disabled={this.props.disabled} maxlength="255" value={this.props.value} onValueChange={this._handleValueChange} />
		},
		_createDateControl: function(time, placeholder) {
			return <DateControl placeholder={placeholder} required={this.props.required} disabled={this.props.disabled} time={time} value={this.props.value} onValueChange={this._handleValueChange} />
		},
		_createTextControl: function() {
			return <TextControl placeholder={this.props.placeholder} required={this.props.required} disabled={this.props.disabled} value={this.props.value} onValueChange={this._handleValueChange} />
		},
		_createEntityControl: function(multiple, placeholder) {
			var props = this.props;
			return <CategoricalControl placeholder={placeholder} nillable={props.attr.nillable} multiple={multiple} disabled={this.props.disabled} entity={props.attr.refEntity} value={props.value} onValueChange={this._handleValueChange} />
		}
	});
	
	molgenis.controls = {
			BoolControl: BoolControl,
			AttributeControl: AttributeControl,
			RangeSliderControl: RangeSliderControl,
			CategoricalControl: CategoricalControl,
			EnumControl: EnumControl
	}
}($, window.top.molgenis = window.top.molgenis || {}));