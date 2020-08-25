window.addEventListener('load', () => {
    if(typeof modules !== 'undefined'){
        modules.forEach(module => {
            fetchNavigationForPage(module)
        })
    } else {
        fetchNavigationForPage()
    }
})

const fetchNavigationForPage = (moduleName) => {
    const url = moduleName ? `navigation-${moduleName}.html` : "navigation.html"
    fetch(pathToRoot + url)
        .then(response => response.text())
        .then(data => {
            const element = document.createElement("div")
            element.innerHTML = data
            element.querySelectorAll(".overview > a").forEach(link => {
                //Multimodule page does not have a pathToRoot
                const withModuleName = moduleName ? moduleName + '/' + link.getAttribute("href") : pathToRoot + link.getAttribute("href")
                link.setAttribute("href", withModuleName);
            })
            document.getElementById("sideMenu").appendChild(element)
        })
        .then(() => {
            document.querySelectorAll(".sideMenuPart").forEach(nav => {
                if (!nav.classList.contains("hidden")) nav.classList.add("hidden")
            })
        }).then(() => {
        revealNavigationForCurrentPage()
    })

    /* Smooth scrolling support for going to the top of the page */
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();

            document.querySelector(this.getAttribute('href')).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });
}

revealNavigationForCurrentPage = () => {
    let pageId = document.getElementById("content").attributes["pageIds"].value.toString();
    let parts = document.querySelectorAll(".sideMenuPart");
    let found = 0;
    do {
        parts.forEach(part => {
            if (part.attributes['pageId'].value.indexOf(pageId) !== -1 && found === 0) {
                found = 1;
                if (part.classList.contains("hidden")){
                 part.classList.remove("hidden");
                 part.setAttribute('data-active',"");
                }
                revealParents(part)
            }
        });
        pageId = pageId.substring(0, pageId.lastIndexOf("/"))
    } while (pageId.indexOf("/") !== -1 && found === 0)
};

revealParents = (part) => {
    if (part.classList.contains("sideMenuPart")) {
        if (part.classList.contains("hidden")) part.classList.remove("hidden");
        revealParents(part.parentNode)
    }
};