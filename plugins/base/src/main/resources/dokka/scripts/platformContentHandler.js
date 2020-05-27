filteringContext = {
    dependencies: {},
    restrictedDependencies: [],
    activeFilters: []
}
window.addEventListener('load', () => {
    initializeFiltering()
    document.querySelectorAll("div[data-platform-hinted]")
        .forEach(elem => elem.addEventListener('click', (event) => togglePlatformDependent(event,elem)))
    document.querySelectorAll("div[tabs-section]")
        .forEach(elem => elem.addEventListener('click', (event) => toggleSections(event)))
    document.getElementById('filter-section').addEventListener('click', (event) => filterButtonHandler(event))
    initTabs()
})

function initTabs(){
    document.querySelectorAll("div[tabs-section]")
        .forEach(element => {
            showCorrespondingTabBody(element)
            element.addEventListener('click', (event) => toggleSections(event))
        })
}

function showCorrespondingTabBody(element){
    const key = element.querySelector("button[data-active]").getAttribute("data-togglable")
    document.querySelector(".tabs-section-body")
        .querySelector("div[data-togglable='" + key + "']")
        .setAttribute("data-active", "")
}

function filterButtonHandler(event) {
        if(event.target.tagName == "BUTTON" && event.target.hasAttribute("data-filter")) {
            let sourceset = event.target.getAttribute("data-filter")
            if(filteringContext.activeFilters.indexOf(sourceset) != -1) {
                filterSourceset(sourceset)
            } else {
                unfilterSourceset(sourceset)
            }
        }
        refreshFilterButtons();
        refreshPlatformTabs()
}

function initializeFiltering() {
    filteringContext.dependencies = JSON.parse(sourceset_dependencies)
    document.querySelectorAll("#filter-section > button")
        .forEach(p => filteringContext.restrictedDependencies.push(p.getAttribute("data-filter")))
    Object.keys(filteringContext.dependencies).forEach(p => {
        filteringContext.dependencies[p] = filteringContext.dependencies[p]
            .filter(q => -1 !== filteringContext.restrictedDependencies.indexOf(q))
    })
    filteringContext.activeFilters = filteringContext.restrictedDependencies
}

function refreshFilterButtons() {
    document.querySelectorAll("#filter-section > button")
        .forEach(f => {
            if(filteringContext.activeFilters.indexOf(f.getAttribute("data-filter")) != -1){
                f.setAttribute("data-active","")
            } else {
                f.removeAttribute("data-active")
            }
        })
}

function filterSourceset(sourceset) {
    filteringContext.activeFilters = filteringContext.activeFilters.filter(p => p != sourceset)
    refreshFiltering()
}

function unfilterSourceset(sourceset) {
    if(filteringContext.activeFilters.length == 0) {
        filteringContext.activeFilters = filteringContext.dependencies[sourceset].concat([sourceset])
        refreshFiltering()
    } else {
        filteringContext.activeFilters.push(sourceset)
        refreshFiltering()
    }
}


function toggleSections(evt){
    if(!evt.target.getAttribute("data-togglable")) return

    const activateTabs = (containerClass) => {
        for(const element of document.getElementsByClassName(containerClass)){
            for(const child of element.children){
                if(child.getAttribute("data-togglable") === evt.target.getAttribute("data-togglable")){
                    child.setAttribute("data-active", "")
                } else {
                    child.removeAttribute("data-active")
                }
            }
        }
    }

    activateTabs("tabs-section")
    activateTabs("tabs-section-body")
}

function togglePlatformDependent(e, container) {
    let target = e.target
    if (target.tagName != 'BUTTON') return;
    let index = target.getAttribute('data-toggle')

    for(let child of container.children){
        if(child.hasAttribute('data-toggle-list')){
            for(let bm of child.children){
                if(bm == target){
                    bm.setAttribute('data-active',"")
                } else if(bm != target) {
                    bm.removeAttribute('data-active')
                }
            }
        }
        else if(child.getAttribute('data-togglable') == index) {
           child.setAttribute('data-active',"")
        }
        else {
            child.removeAttribute('data-active')
        }
    }
}

function refreshFiltering() {
    let sourcesetList = filteringContext.activeFilters
    document.querySelectorAll("[data-filterable-set]")
        .forEach(
            elem => {
                let platformList = elem.getAttribute("data-filterable-set").split(' ').filter(v => -1 !== sourcesetList.indexOf(v))
                elem.setAttribute("data-filterable-current", platformList.join(' '))
            }
        )
}

function refreshPlatformTabs() {
    document.querySelectorAll(".platform-hinted > .platform-bookmarks-row").forEach(
        p => {
            let active = false;
            let firstAvailable = null
            p.childNodes.forEach(
                element => {
                    if(element.getAttribute("data-filterable-current") != ''){
                        if( firstAvailable == null) {
                            firstAvailable = element
                        }
                        if(element.hasAttribute("data-active")) {
                            active = true;
                        }
                    }
                }
            )
            if( active == false && firstAvailable !== null ) {
                firstAvailable.click()
            }
        }
    )
}
