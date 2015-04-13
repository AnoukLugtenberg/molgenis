/* global _: false, React: false, molgenis: true */
(function(_, React, molgenis) {
	"use strict";
	
	var textarea = React.DOM.textarea;
	
	/**
	 * @memberOf component
	 */
	var TextArea = React.createClass({
		mixins: [molgenis.ui.mixin.DeepPureRenderMixin],
		displayName: 'TextArea',
		propTypes: {
			id: React.PropTypes.string,
			name: React.PropTypes.string,
			placeholder: React.PropTypes.string,
			required: React.PropTypes.bool,
			disabled: React.PropTypes.bool,
			readOnly: React.PropTypes.bool,
			value: React.PropTypes.string,
			onValueChange: React.PropTypes.func.isRequired
		},
		getInitialState: function() {
			return {value: this.props.value};
		},
		componentWillReceiveProps : function(nextProps) {
			this.setState({
				value: nextProps.value
			});
		},
		render: function() {
			return textarea({
				className: 'form-control',
				id: this.props.id,
				name: this.props.name,
				placeholder: this.props.placeholder,
				required: this.props.required,
				disabled: this.props.disabled,
				readOnly: this.props.readOnly,
				value: this.state.value,
				onChange: this._handleChange});
		},
		_handleChange: function(event) {
			this.setState({value: event.target.value});
			
			var value = event.target.value !== '' ? event.target.value : null;
			this.props.onValueChange({value: value});
		}
	});
	
	// export component
	molgenis.ui = molgenis.ui || {};
	_.extend(molgenis.ui, {
		TextArea: React.createFactory(TextArea)
	});
}(_, React, molgenis));