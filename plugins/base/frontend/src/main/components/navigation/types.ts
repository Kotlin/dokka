export type NavigationRecord = {
    id: string;
    name: string;
    location: string;
    children: NavigationRecord[];
}