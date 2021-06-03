document.addEventListener("DOMContentLoaded", function() {
    let nav = document.getElementsByClassName("md-nav");
    for(let i = 0; i < nav.length; i++) {
        if (nav.item(i).getAttribute("data-md-level")) {
            nav.item(i).style.display = 'block';
            nav.item(i).style.overflow = 'visible';
        }
    }

    nav = document.getElementsByClassName("md-nav__toggle");
    for(let i = 0; i < nav.length; i++) {
        nav.item(i).checked = true;
    }
});
