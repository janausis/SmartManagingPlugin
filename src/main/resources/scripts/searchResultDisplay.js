function searchResultDisplay(playerList, dataList) {
    outList = document.getElementById("result-list");

    for (let i = 0; i < playerList.length; i++) {
      outList.innerHTML += getTemplate(dataList[i], playerList[i]);
    }
}

function getTemplate(dataList, playername) {
    template =
    '<li class="result-list-item">' +
    '  <h1 class="item-bigtitle item-center player-top-margin results-bigtitle">' + playername + '</h1>' +
    '  <div class="stat-left">'

    for (let i = 0; i < dataList.length; i+=2) {
      template +=
      '    <h3 class="item-ip item-left white inline size-search">' + dataList[i][0] + '</h3>' +
      '    <h3 class="item-ip item-left green inline size-search">' + dataList[i][1] + '</h3>' +
      '    <br><br>'
    }

    template +=
    '  </div>' +
    '  <div class="stat-right">';

    for (let i = 1; i < dataList.length; i+=2) {
      template +=
      '    <h3 class="item-ip item-left white inline size-search">' + dataList[i][0] + '</h3>' +
      '    <h3 class="item-ip item-left green inline size-search">' + dataList[i][1] + '</h3>' +
      '    <br><br>'
    }

    template +=
    '  </div>' +
    '</li>';

    return template;

}
