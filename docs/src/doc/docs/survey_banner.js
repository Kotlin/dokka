window.addEventListener('load', () => {
    const appended = document.createElement("a")
    appended.style = "display: block;text-decoration: none !important;color: #E8F0FE !important;font-family: Inter, Arial, sans-serif !important;font-size: 18px;font-weight: 500;line-height: 24px;padding: 6px 0;position: relative;text-align: center;background-color: #7F52FF;z-index: 5000000;"
    appended.href = "https://surveys.jetbrains.com/s3/dokka-survey"
    appended.innerText = "Take part in Dokka devX survey. It helps us a lot, and gives you a chance to win a prize! -->"
    document.body.prepend(appended)
    window.scrollTo(0, 0);
})