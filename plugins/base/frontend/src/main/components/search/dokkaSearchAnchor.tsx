import React from "react";
import SearchIcon from 'react-svg-loader!../assets/searchIcon.svg';

export const DokkaSearchAnchor = ({wrapperProps, buttonProps, popup}: any) => {
    return (
        <span {...wrapperProps}>
            <button type="button" {...buttonProps}>
                <SearchIcon />
            </button>
            {popup}
        </span>
    )
}