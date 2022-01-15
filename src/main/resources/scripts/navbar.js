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

function checkLogin(routes, names) {
  var template = "";
    for (var i = 0; i < routes.length; i++) {
      template += '<a href="'+ routes[i] +'"><button class="navbar-button b'+ routes.length +' b'+ routes.length +'-'+ (i+1) +'">'+ names[i] +'</button></a>';
    }

    template += "" +
    '<div class="topnav">' +
      '<a class="active"></a>' +
        '<div id="myLinks">';
          for (var i = 0; i < routes.length; i++) {
            template += '<a class="tab" href="'+ routes[i] +'">'+ names[i] +'</a>';
          }
        template += "" +
        '</div>' +

      '<div href="javascript:void(0);" class="icon" onclick="myFunction()">' +
        '<div class="burger"></div>' +
        '<div class="burger"></div>' +
        '<div class="burger"></div>' +
      '</div>' +
    '</div>';


  window.onload = (event) => {
    init(template);
  };
}

function init(template) {
  let nav = document.getElementById('mainNav');
  nav.innerHTML = template;
}
