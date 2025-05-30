javascript:(function(){
  if(window.scrollLoop) { clearInterval(window.scrollLoop); window.scrollLoop = null; alert("Auto-scroll stopped"); }
  else {
    window.scrollLoop = setInterval(function(){ window.scrollBy(0, 50); }, 3000);
    alert("Auto-scroll started");
  }
})();
