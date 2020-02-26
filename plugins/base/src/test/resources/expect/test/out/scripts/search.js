var query = new URLSearchParams(window.location.search).get("query");
    document.getElementById("searchTitle").innerHTML += '"' + query + '":';
    document.getElementById("searchTable").innerHTML = pages.filter(el => el.name.startsWith(query)).reduce((acc, element) => { return acc +
        '<tr><td><a href="' + element.location + '">' + element.name + '</a></td></tr>'
    }, "");