declare module '@jetbrains/ring-ui' {
    export const Select: any;
    export const List: any;
}

declare module '@jetbrains/ring-ui/components/global/fuzzy-highlight.js' {
    import {Option} from "../../components/search/types";

    export const fuzzyHighlight: (string, string, boolean) => Option[]
}
