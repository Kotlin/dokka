onload = () => {
    fetch(pathToRoot + "navigation.html")
        .then(response => response.text())
        .then(data => {
            document.getElementById("sideMenu").innerHTML = data;
        }).then(() => {
        document.querySelectorAll(".overview > a").forEach(link => {
            link.setAttribute("href", pathToRoot + link.getAttribute("href"));
            console.log(link.attributes["href"])
        })
    }).then(() => {
        document.querySelectorAll(".sideMenuPart").forEach(nav => {
            if (!nav.classList.contains("hidden")) nav.classList.add("hidden")
        })
    }).then(() => {
        revealNavigationForCurrentPage()
    })
};

revealNavigationForCurrentPage = () => {
    let pageId = document.getElementById("content").attributes["pageIds"].value.toString();
    let parts = document.querySelectorAll(".sideMenuPart");
    let found = 0;
    do {
        parts.forEach(part => {
            if (part.attributes['pageId'].value.indexOf(pageId) !== -1 && found === 0) {
                found = 1;
                if (part.classList.contains("hidden")) part.classList.remove("hidden");
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