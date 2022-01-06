function myFunction() {
  const x = document.getElementById("myLinks");
  if (x.style.display === "block") {
    x.style.display = "none";
  } else {
    x.style.display = "block";
  }
}

function checkLogin(loggedin) {
  let nav;
  if(loggedin) {
    nav = document.getElementById('notloggedin');
    nav.parentNode.removeChild(nav);
  } else {
    nav = document.getElementById('logged');
    nav.parentNode.removeChild(nav);
  }
}
