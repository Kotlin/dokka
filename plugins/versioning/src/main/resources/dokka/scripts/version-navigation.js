window.addEventListener('load', () => {
    document.querySelectorAll(".versions-dropdown a")
            .forEach(elem => elem.addEventListener('click', (event) => selectVersion(event, elem)))
})

const getLastAvailableVersion = () => {
    const versions = document.querySelector(".versions-dropdown-data").children
    for (let i = versions.length - 1; i >= 0; i--) {
        let element = versions[i]
        if(!element.classList.contains("unavailable-version")) {
            return element
        }
    }
}

const selectVersion = (event, elem) => {
    if(elem.classList.contains("unavailable-version")) {
        event.preventDefault()
        document.querySelector(".versions-dropdown-button").textContent = elem.textContent

        let lastVersion = getLastAvailableVersion()
        const appended = document.createElement("iframe")
        appended.src = elem.getAttribute("href")
        appended.frameBorder = 0
        appended.height="100%"
        appended.onload = function() {
            const messageBox = appended.contentWindow.document.querySelector(".sub-title")
            messageBox.textContent="The declaration is unavaibaile in " +
            elem.textContent + " version, but this exists in "
            let link = appended.contentWindow.document.createElement("a")
            link.href = lastVersion.href
            link.textContent =  lastVersion.textContent
            link.target = "_parent"
            messageBox.append(link)
        };
        const content = document.getElementById("main")
        content.textContent = ''
        content.prepend(appended)
    }
}