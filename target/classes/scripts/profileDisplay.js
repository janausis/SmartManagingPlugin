function profile(modeList, scoreList, valueList) {
  list = document.getElementById('gamelist');
  outList = document.getElementById('showlist');

  //scoreList = [["1", "1", "1", "1", "1"], ["1", "1", "1", "1", "1"]]
  //valueList = [["a", "a", "a", "a", "a"], ["a", "a", "a", "a", "a"]]
  //modeList = ["Skywars", "Bedwars"];

  for(let i = 0; i < modeList.length; i++) {
    let scoreTemplate = '' +
            '<div class="wrapper">' +
            '  <div class="stat-left">';


    for (let v = 1; v <= scoreList[i].length; v+=2) {
      if (v <= scoreList[i].length) {
          t = '    <h3 class="item-ip item-left white inline size-score">' + scoreList[i][v-1] + '</h3>' +
          '    <h3 class="item-ip item-left green inline size-score">' + valueList[i][v-1] + '</h3>' +
          '    <br>' +
          '    <br>';
          scoreTemplate += t;
      }
    }
    scoreTemplate += '  </div>' +
    '  <div class="stat-right">';

    for (let v = 2; v <= scoreList[i].length; v+=2) {
      if (v <= scoreList[i].length) {
          t = '    <h3 class="item-ip item-left white inline size-score">' + scoreList[i][v-1] + '</h3>' +
          '    <h3 class="item-ip item-left green inline size-score">' + valueList[i][v-1] + '</h3>' +
          '    <br>' +
          '    <br>';
          scoreTemplate += t;
      }
    }
    scoreTemplate += '  </div>' +
    '</div>' +
    '';

    const style = document.createElement('style');
    style.type = 'text/css';
      style.innerHTML = '.selectable-' + modeList[i].replace(/[^a-zA-Z]/g, "") + ' { background-image: linear-gradient(rgba(0, 0, 0, 0.8), rgba(0, 0, 0, 0.6)), url(../images/modes/' + modeList[i].replace(/[^a-zA-Z]/g, "") + '.png);}';
      style.innerHTML += '.selectable-' + modeList[i].replace(/[^a-zA-Z]/g, "") + ':hover { background-image: linear-gradient(rgba(125, 125, 125, 0.6), rgba(125, 125, 125, 0.4)), url(../images/modes/' + modeList[i].replace(/[^a-zA-Z]/g, "") + '.png);}';

      document.getElementsByTagName('head')[0].appendChild(style);

      if (modeList[i] === modeList[0]) {
        list.innerHTML += '<li class="selected selectable-' + modeList[i].replace(/[^a-zA-Z]/g, "") + '">' + modeList[i] + '</li>'
      } else {
          list.innerHTML += '<li class="selectable selectable-' + modeList[i].replace(/[^a-zA-Z]/g, "") + '">' + modeList[i] + '</li>'
      }

      outList.innerHTML += '<li class="showitem" id="out-' + modeList[i].replace(/[^a-zA-Z]/g, "") + '">' + modeList[i] + '</li>'
      scores = document.getElementById('out-' + modeList[i].replace(/[^a-zA-Z]/g, ""));

      scores.innerHTML += scoreTemplate;
  }

  // Selector
  document.querySelector('ul').addEventListener('click', function(e) {
    let selected;

    if(e.target.tagName === 'LI') {
      selected = document.querySelector('li.selected');
      if(selected) {
        selected.classList.add('selectable');
        selected.classList.remove('selected');
      }

      e.target.classList.remove('selectable');
      e.target.classList.add('selected');

      //Scroll target
      const tar = document.getElementById('out-' + e.target.textContent.replace(/[^a-zA-Z]/g, ""));
      tar.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  });
}
