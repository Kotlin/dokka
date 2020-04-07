import React, {Component} from 'react';
import Select from '@jetbrains/ring-ui/components/select/select';
import '@jetbrains/ring-ui/components/input-size/input-size.scss';

class WithFuzzySearchFilterComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {selected: props.data[0]};
  }

  clearSelection = () => {
    this.setState({selected: null});
  };

  onSelect = option => {
    window.location.href = `${window.pathToRoot}${option.location}?query${option.name}`;
    this.setState({selected: option});
    debugger
  };

  render() {
    return (
      <div className="search-container">
        <div className="search">
          <Select
            selectedLabel="Search"
            label="Please type page name"
            filter={{fuzzy: true}}
            clear
            selected={this.state.selected}
            data={this.props.data}
            onSelect={this.onSelect}
          />
        </div>
      </div>
    );
  }
}

export const WithFuzzySearchFilter = () => {
  let data = [];
  if (window.pages) {
    data = window.pages.map((page, i) => ({
      ...page,
      label: page.name,
      key: i + 1,
      type: page.kind
    }));
  }

  return <WithFuzzySearchFilterComponent data={data}/>;
};
