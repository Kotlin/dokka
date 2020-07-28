export type Page = {
    name: string;
    kind: string;
    location: string;
    searchKey: string;
    description: string;
    disabled: boolean;
}

export type Option = Page & {
    label: string;
    key: number;
    location: string;
    name: string;
}

export type IWindow = typeof window & {
    pathToRoot: string
    pages: Page[]
}

export type Props = {
    data: Option[]
};


export type State = {
    selected: any
}
