function myFunction() {
  const x = document.getElementById("myLinks");
  if (x.style.display === "block") {
    x.style.display = "none";
  } else {
    x.style.display = "block";
  }
}

function myFunction() {
  const x = document.getElementById("myLinks");
  if (x.style.display === "block") {
    x.style.display = "none";
  } else {
    x.style.display = "block";
  }
}

function checkLogin(loggedin) {
  window.onload = (event) => {
    init(loggedin);
  };
}

function init(loggedin) {
  if(loggedin) {
    let nav = document.getElementById('notloggedin');
    nav.parentNode.removeChild(nav);
  } else {
    let nav = document.getElementById('logged');
    nav.parentNode.removeChild(nav);
  }
}
