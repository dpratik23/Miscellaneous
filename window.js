let goingDown = true;
let interval = setInterval(() => {
    if (goingDown) {
        window.scrollBy(0, 50);
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight) goingDown = false;
    } else {
        window.scrollBy(0, -50);
        if (window.scrollY <= 0) goingDown = true;
    }
}, 3000);
