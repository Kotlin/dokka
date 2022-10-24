navigationPageText = fetch(pathToRoot + "navigation.html").then(response => response.text())

displayNavigationFromPage = () => {
    navigationPageText.then(data => {
        document.getElementById("sideMenu").innerHTML = data;
    }).then(() => {
        document.querySelectorAll(".overview > a").forEach(link => {
            link.setAttribute("href", pathToRoot + link.getAttribute("href"));
        })
    }).then(() => {
        document.querySelectorAll(".sideMenuPart").forEach(nav => {
            if (!nav.classList.contains("hidden"))
                nav.classList.add("hidden")
        })
    }).then(() => {
        revealNavigationForCurrentPage()
        document.querySelectorAll("#sideMenu a")
            .forEach(elem => elem.addEventListener('click', (event) => selectPage(event, elem)))
        document.querySelectorAll("#main a")
            .forEach(elem => elem.addEventListener('click', (event) => selectPage(event, elem)))
    }).then(() => {
        scrollNavigationToSelectedElement()
    })
    document.querySelectorAll('.footer a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelector(this.getAttribute('href')).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });
    window.addEventListener('popstate', (event) => {
      loadPage(document.location, false)
    });
}

const selectPage = (event, elem) => {
    const link = elem.getAttribute("href")
    let pattern = /^#|^(?:[a-z]+:)?\/\//gi;
    if(link.match(pattern))
        return;
     event.preventDefault();
     event.stopPropagation();
     event.stopImmediatePropagation();
     loadPage(link, true)
}

const loadPage = (link, isPushState) => {
    navigationPageText = fetch(link).then(response => response.text()).then(data => {
        var parser = new DOMParser();
        var doc = parser.parseFromString(data, "text/html");

        const oldPathToRoot = pathToRoot
                // update pathToRoot
        const newScript = document.createElement("script");
        newScript.appendChild(document.createTextNode(doc.getElementsByTagName("script")[0].innerHTML));
        const oldScript = document.getElementsByTagName("script")[0]
        oldScript.parentNode.replaceChild(newScript, oldScript);

        replacePathToRoot(oldPathToRoot)

        document.getElementById("main").innerHTML = doc.getElementById("main").innerHTML;
        document.getElementById("navigation-wrapper").innerHTML = doc.getElementById("navigation-wrapper").innerHTML;
        document.getElementsByTagName("title")[0].innerHTML = doc.getElementsByTagName("title")[0].innerHTML;

        if(isPushState) window.history.pushState({}, doc.getElementsByTagName("title")[0].innerHTML, link);

        document.querySelectorAll("#main a")
          .forEach(elem => elem.addEventListener('click', (event) => selectPage(event, elem)))

     //    console.log(pathToRoot + "\n" + oldPathToRoot)
    }).then(() => {
             revealNavigationForCurrentPage()
             window.Prism = window.Prism || {};
             window.Prism.highlightAllUnder(document.getElementById("main"))
          // scrollNavigationToSelectedElement()
            document.dispatchEvent(new Event('updateContentPage'))
    })
}

// TODO: replace with absolute link
replacePathToRoot = (oldPathToRoot) => {
    document.querySelectorAll(".overview > a").forEach(link => {
        const  oldLink = link.getAttribute("href")
        if(oldLink.startsWith(oldPathToRoot)) {
            const originLink = oldLink.substring(oldPathToRoot.length)
              ///console.log(oldLink + "\n" + pathToRoot + originLink)
            link.setAttribute("href", pathToRoot + originLink);
        }
    })
}

revealNavigationForCurrentPage = () => {
    let pageId = document.getElementById("content").attributes["pageIds"].value.toString();
    let parts = document.querySelectorAll(".sideMenuPart");
    let found = 0;
    do {
        parts.forEach(part => {
            if (part.attributes['pageId'].value.indexOf(pageId) !== -1 && found === 0) {
                found = 1;
                 part.classList.remove("hidden");
                 part.setAttribute('data-active', "");
                revealParents(part)
            } else if(part.hasAttribute("data-active")) {
                part.classList.add("hidden");
                part.removeAttribute("data-active")
            }
        });
        pageId = pageId.substring(0, pageId.lastIndexOf("/"))
    } while (pageId.indexOf("/") !== -1 && found === 0)
};
revealParents = (part) => {
    if (part.classList.contains("sideMenuPart")) {
        if (part.classList.contains("hidden"))
            part.classList.remove("hidden");
        revealParents(part.parentNode)
    }
};

scrollNavigationToSelectedElement = () => {
    let selectedElement = document.querySelector('div.sideMenuPart[data-active]')
    if (selectedElement == null) { // nothing selected, probably just the main page opened
        return
    }

    let hasIcon = selectedElement.querySelectorAll(":scope > div.overview span.nav-icon").length > 0

    // for instance enums also have children and are expandable, but are not package/module elements
    let isPackageElement = selectedElement.children.length > 1 && !hasIcon
    if (isPackageElement) {
        // if package is selected or linked, it makes sense to align it to top
        // so that you can see all the members it contains
        selectedElement.scrollIntoView(true)
    } else {
        // if a member within a package is linked, it makes sense to center it since it,
        // this should make it easier to look at surrounding members
        selectedElement.scrollIntoView({
            behavior: 'auto',
            block: 'center',
            inline: 'center'
        })
    }
}

/*
    This is a work-around for safari being IE of our times.
    It doesn't fire a DOMContentLoaded, presumabely because eventListener is added after it wants to do it
*/
if (document.readyState == 'loading') {
    window.addEventListener('DOMContentLoaded', () => {
        displayNavigationFromPage()
    })
} else {
    displayNavigationFromPage()
}
