onload = () => {
    fetch(pathToRoot + "navigation.html")
    .then(response => response.text())
    .then(data => {
        document.getElementById("sideMenu").innerHTML = data;
    }).then(() => {
        document.querySelectorAll(".overview > a").forEach(link => {
            link.setAttribute("href", pathToRoot + link.getAttribute("href"))
            console.log(link.attributes["href"])
        })
    })
}