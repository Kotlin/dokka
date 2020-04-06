window.addEventListener('load', () => {
    document.querySelectorAll("div[data-platform-hinted]")
        .forEach(elem => elem.addEventListener('click', (event) => togglePlatformDependent(event,elem)))
    }
)

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
