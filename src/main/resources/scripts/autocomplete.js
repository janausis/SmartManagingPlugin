function loadJSON(path, success, error, th, inp, ip, closeAllLists, typedValue) {
  var xhr = new XMLHttpRequest();
  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      if (xhr.status === 200) {
        success(JSON.parse(xhr.responseText), th, inp, ip, closeAllLists, typedValue);
      }
      else {
        console.log(xhr.responseText);
      }
    }
  };
  xhr.open('GET', path, true);
  xhr.send();
}



function myData(Data, th, inp, ip, closeAllLists, typedValue)
{
  closeAllLists();


  var a, b, i, val = th.value;

  arr = Data.data;

  if (arr.length <= 0) {
    return;
  }

  currentFocus = -1;
  /*create a DIV element that will contain the items (values):*/
  a = document.createElement("DIV");
  a.setAttribute("id", th.id + "autocomplete-list");
  a.setAttribute("class", "autocomplete-items");

  if (inp.getAttribute("id") == "playername2") {
    a.setAttribute("class", "autocomplete-items autocomplete-results");
  }

  /*append the DIV element as a child of the autocomplete container:*/
  th.parentNode.appendChild(a);
  /*for each item in the array...*/
  var tmp = 0;
  var last = null;
  for (i = 0; i < arr.length; i++) {
    /*check if the item starts with the same letters as the text field value:*/
    if (arr[i].substr(0, val.length).toUpperCase() == val.toUpperCase()) {
      /*create a DIV element for each matching element:*/
      b = document.createElement("DIV");
      /*make the matching letters bold:*/
      b.innerHTML = "<strong class='autocomplete-correct'>" + arr[i].substr(0, val.length) + "</strong>";
      b.innerHTML += arr[i].substr(val.length);
      /*insert a input field that will hold the current array item's value:*/
      b.innerHTML += "<input id='" + arr[i] + "' type='hidden' value='" + arr[i] + "'>";
      /*execute a function when someone clicks on the item value (DIV element):*/

      b.addEventListener("mouseover", function(evt) {
        inp.value = this.getElementsByTagName("input")[0].value;
      });
      b.addEventListener("mouseout", function(evt) {
        inp.value = typedValue;
      });

      b.addEventListener("click", function(e) {
          /*insert the value for the autocomplete text field:*/

          form = document.getElementById("searchForm");

          var input = document.createElement('input');
          input.setAttribute("name", 'exactMatch');
          input.setAttribute('value', "true");
          input.setAttribute('type', "text")
          form.appendChild(input);
          form.submit();
          /*close the list of autocompleted values,
          (or any other open lists of autocompleted values:*/
          closeAllLists();
      });
      a.appendChild(b);
      tmp ++;
      last = b;
    }
  }
  if (tmp > 0) {
    inp.classList.add('found');
    if (last != null) {
      last.classList.add("found-bottom")
    }
  } else {
    inp.classList.remove('found');
  }

}

var arr = [];

function autocomplete(inp, ip) {
  var typedValue = "";
  /*the autocomplete function takes two arguments,
  the text field element and an array of possible autocompleted values:*/
  var currentFocus;
  /*execute a function when someone writes in the text field:*/
  inp.addEventListener("input", function(e) {
      var a, b, i, val = this.value;
      typedValue = val;
      /*close any already open lists of autocompleted values*/

      if (!val) {closeAllLists(); return false;}

      currentFocus = -1;
      loadJSON(ip + '/players/search?search=' + val, myData,'jsonp', this, inp, ip, closeAllLists, typedValue);

  });
  /*execute a function presses a key on the keyboard:*/
  inp.addEventListener("keydown", function(e) {
      var x = document.getElementById(this.id + "autocomplete-list");
      if (x) x = x.getElementsByTagName("div");
      if (e.keyCode == 40) {
        /*If the arrow DOWN key is pressed,
        increase the currentFocus variable:*/
        currentFocus++;
        /*and and make the current item more visible:*/
        addActive(x);
      } else if (e.keyCode == 38) { //up
        /*If the arrow UP key is pressed,
        decrease the currentFocus variable:*/
        currentFocus--;
        /*and and make the current item more visible:*/
        addActive(x);
      } else if (e.keyCode == 13) {
        /*If the ENTER key is pressed, prevent the form from being submitted,*/
        if (currentFocus > -1) {
          e.preventDefault();
          /*and simulate a click on the "active" item:*/
          if (x) x[currentFocus].click();
        }
      }
  });
  function addActive(x) {
    /*a function to classify an item as "active":*/
    if (!x) return false;
    /*start by removing the "active" class on all items:*/
    removeActive(x);

    if (currentFocus >= x.length) currentFocus = -1;
    if (currentFocus < -1) currentFocus = (x.length - 1);

    if (currentFocus == -1) {return;}
    inp.value = x[currentFocus].querySelector('input').value;
    x[currentFocus].classList.add("autocomplete-active");
  }
  function removeActive(x) {
    inp.value = typedValue;
    /*a function to remove the "active" class from all autocomplete items:*/
    for (var i = 0; i < x.length; i++) {
      x[i].classList.remove("autocomplete-active");
    }
  }
  function closeAllLists(elmnt) {
    inp.classList.remove('found');
    /*close all autocomplete lists in the document,
    except the one passed as an argument:*/
    var x = document.getElementsByClassName("autocomplete-items");
    for (var i = 0; i < x.length; i++) {
      if (elmnt != x[i] && elmnt != inp) {
        x[i].parentNode.removeChild(x[i]);
      }
    }
  }
  /*execute a function when someone clicks in the document:*/
  document.addEventListener("click", function (e) {
      closeAllLists(e.target);
  });
}
