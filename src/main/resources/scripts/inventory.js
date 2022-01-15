function minecraftInventory(originalData) {

  /* Drag and drop heavily inspired by
     https://codepen.io/lavininhamelo/pen/NWxPKLJ*/

  // Init variables
  let stackSizeDict = {};
  let current_slot = null;
  let status_click = false;
  let current_itemId = null;
  let current_itemType = null;
  let current_itemValue = null;
  let parent_slot = null;

  populate(originalData);

  // Every item that was placed in the inventory
  const items = document.querySelectorAll('.item');

  // Update slot padding for when an item is in a slot or not
  checkSlots();

  items.forEach((item) => {
    // On left Click
    item.addEventListener('click', function(){
      moveItem(false, this);
    }, false);

    // On right click
    item.addEventListener('contextmenu', function(){
      moveItem(true, this);
    }, false);

    // Set all item to not follow the cursor
    item.setAttribute('draggable', false);
  });

  function moveItem(halfItem, item) {
    // Prevent what normally happens on left click
    event.preventDefault();

    // Set currently used item
    let waitItem = null;

    // Clone item and make it attach to mouse
    let ghostItem = item.cloneNode(true);
    ghostItem.setAttribute('class', 'ghostItem');
    ghostItem.classList.add("tooltip");

    if (halfItem) {
      value = parseInt(ghostItem.lastElementChild.innerHTML) / parseFloat(2);
      if (value !== 0.5) {
        ghostItem.lastElementChild.innerHTML -= Math.floor(value);
        item.lastElementChild.innerHTML -= Math.floor(value);
      } else {
        item.lastElementChild.innerHTML -= 1;
        item.classList.add("invisible")
      }
    } else {

      value = parseInt(ghostItem.lastElementChild.innerHTML);
      item.lastElementChild.innerHTML -= Math.floor(value);

      // hide item in slot because it will be 0 anyway
      item.classList.add("invisible")
    }


    parent_slot = item.parentNode.id;

    // Create a collision box to check for the nearest slot when needed
    let shiftX = ghostItem.getBoundingClientRect().left + 20;
    let shiftY = ghostItem.getBoundingClientRect().top + 20;

    // Show draggable item above everything else in the page
    ghostItem.style.position = 'absolute';
    ghostItem.style.zIndex = 1000;

    // Show draggable item in document
    document.body.append(ghostItem);

    // On left click when already lifted
    ghostItem.onclick = function (event) {

      // Current item id is set on mouse move and is the id of the nearest slot
      if (current_itemId && current_itemId != item.parentNode.id) {
        let area = document.getElementById(current_itemId);
        let free_space = !!!area.firstElementChild;

        // If slot is free
        if (free_space) {
          // Create new item with same id
          let newItem = item.cloneNode(true);
          // register listeners for new item
          newItem.addEventListener('click', function(){
            moveItem(false, this);
          }, false);
          newItem.addEventListener('contextmenu', function(){
            moveItem(true, this);
          }, false);
          newItem.setAttribute('draggable', false);

          ghostItemVal = parseInt(ghostItem.lastElementChild.innerHTML);
          // if origin had only one item, remove
          newItem.lastElementChild.innerHTML = ghostItemVal;
          if (parseInt(item.lastElementChild.innerHTML) <= 0) {
            item.remove();

            // if not subtract item from origin and draggable and add to destination
          } else {
            item.classList.remove('invisible');
          }
          ghostItem.remove();

          // Set new item value to one because the space was free


          // add new item and remove invisibility from item copy
          area.append(newItem);
          newItem.classList.remove('invisible');


          // If slot has item of same id
        } else if (current_itemType == item.id) {
          if (current_slot && item.dataset.nbt == current_slot.firstElementChild.dataset.nbt) {
            // get count from slot
            let destiny = area.firstElementChild.lastElementChild.innerHTML;
            // get count from draggable
            let origin = ghostItem.lastElementChild.innerHTML;
            let total = parseInt(destiny) + parseInt(origin);

            // Stack size is more than the allowed stacksize for this item
            maxStack = getStackSize(item.id);
            if (total > maxStack) {
              // subtract from origin
              item.lastElementChild.innerHTML = parseInt(item.lastElementChild.innerHTML) + (parseInt(origin) - (maxStack - destiny));
              // Set destiny to maxstack
              area.firstElementChild.lastElementChild.innerHTML = maxStack;
            } else {
              // If smaller add to destiny and delete origin
              area.firstElementChild.lastElementChild.innerHTML = total;

              if (parseInt(item.lastElementChild.innerHTML) <= 0) {
                item.remove();
              }
            }
          } else {
            item.lastElementChild.innerHTML = parseInt(item.lastElementChild.innerHTML) + parseInt(ghostItem.lastElementChild.innerHTML);
          }
        } else {
          // If not free and not same id, switch places
          item.parentNode.append(area.firstElementChild);
          item.lastElementChild.innerHTML = parseInt(item.lastElementChild.innerHTML) + parseInt(ghostItem.lastElementChild.innerHTML);
          area.append(item);
        }
      } else {
        item.lastElementChild.innerHTML = parseInt(item.lastElementChild.innerHTML) + parseInt(ghostItem.lastElementChild.innerHTML);
      }
      // show item and remove draggable after click
      item && item.classList.remove('invisible');
      ghostItem.remove();
      status_click = !status_click;
      checkSlots();
    };

    // On right click whilst dragging
    ghostItem.oncontextmenu = function (event) {
      // Prevent contextmenu from opening
      event.preventDefault();
      if (current_itemId && current_itemId != item.parentNode.id) {
        let area = document.getElementById(current_itemId);
        let free_space = !!!area.firstElementChild;

        // Slot is free
        if (free_space) {
          // Create new item with same id
          let newItem = item.cloneNode(true);
          // register listeners for new item
          newItem.addEventListener('click', function(){
            moveItem(false, this);
          }, false);
          newItem.addEventListener('contextmenu', function(){
            moveItem(true, this);
          }, true);
          newItem.setAttribute('draggable', false);

          // if origin had only one item, remove
          if (parseInt(ghostItem.lastElementChild.innerHTML) == 1) {
            ghostItem.remove();
            status_click = !status_click;

            // if not subtract one item from draggable and add to destination
          } else {

            // Remove from draggable
            ghostItem.lastElementChild.innerHTML =
                parseInt(ghostItem.lastElementChild.innerHTML) - 1;
          }
          // Set new item value to one because the space was free
          newItem.lastElementChild.innerHTML = 1;

          // add new item and remove invisibility from item copy
          area.append(newItem);
          newItem.classList.remove('invisible');

          // If not empty but has the same id
        } else if (current_itemType == item.id) {
          if (current_slot && item.dataset.nbt == current_slot.firstElementChild.dataset.nbt) {
            // remove origin if only 1 left
            if (parseInt(ghostItem.lastElementChild.innerHTML) == 1) {
              ghostItem.remove();
              status_click = !status_click;
            }
            let destiny = parseInt(
                area.firstElementChild.lastElementChild.innerHTML
            );
            let origin = parseInt(ghostItem.lastElementChild.innerHTML);
            let total = destiny + 1;

            // Stack size is more than the allowed stacksize for this item
            maxStack = getStackSize(item.id);
            if (total > maxStack) {

            } else {
              // add to 1 destination and remove 1 from draggable and origin
              area.firstElementChild.lastElementChild.innerHTML = destiny + 1;
              ghostItem.lastElementChild.innerHTML = origin - 1;
            }
          }
        }
      }
      checkSlots();
    };


    status_click = !status_click;

    if (status_click) {
      // If currently dragging, move ghost item to cursor
      moveAt(event.pageX, event.pageY);
    }

    // Move draggable to pixel coordinate in page
    function moveAt(pageX, pageY) {
      ghostItem.style.left = pageX - shiftX + 'px';
      ghostItem.style.top = pageY - shiftY + 'px';
    }

    function onMouseMove(event) {
      if (status_click) {
        // move draggable
        moveAt(event.pageX, event.pageY);
      }
      // hide draggable, apparently for actual recalculation of position
      ghostItem.hidden = true;
      // Get element below draggable
      let elemBelow = document.elementFromPoint(event.clientX, event.clientY);
      ghostItem.hidden = false;

      // If there is no element below, stop
      if (!elemBelow) return;

      // Get closest slot in below element radius
      let droppableBelow = elemBelow.closest('.slot');

      // if slot and item below dont match
      if (current_slot != droppableBelow) {
        // leave draggable if current_slot is set, meaning the draggable has been picked up
        if (current_slot) {
          leaveDroppable(current_slot);
        }

        // Set current_slot and other variables on slot change
        current_slot = droppableBelow;
        if (current_slot) {
          enterDroppable(current_slot);
        }
      }
    }

    document.addEventListener('mousemove', onMouseMove);

  }

  function checkSlots() {
    // add padding to slots if empty and remove if containing item
    slots = document.querySelectorAll('.slot');
    slots.forEach((item) => {
      if (item.firstChild) {
        item.classList.add('no-padding');
      } else {
        item.classList.remove('no-padding');
      }
    });
  }

  function enterDroppable(elem) {
    current_itemId = elem.id;
    if (elem.firstElementChild) {
      current_itemType = elem.firstElementChild.id;
      child = elem.firstElementChild;
      current_itemValue = child.lastElementChild.innerHTML;
    }
    checkSlots();
  }

  function leaveDroppable(elem) {
    current_itemId = null;
    current_itemType = null;
    current_itemValue = null;
    free_space = false;
    parent_slot = null;
    checkSlots();
  }


  function getUpperCaseFirst(str) {
    const arr = str.split(" ");
    for (var i = 0; i < arr.length; i++) {
      arr[i] = arr[i].charAt(0).toUpperCase() + arr[i].slice(1);

    }
    const str2 = arr.join(" ");
    return str2;
  }


  function getSlotTemplate(count, id, tag, parent_id) {
    tmp = '<div data-nbt="' + tag + '" class="item tooltip" id="' + id +'">' +
        '  <img class="iconImage" src="../images/renders/' + id.replace("minecraft:", "") +'.png">' +
        '  <span id="tooltip-span">' +
        '       ' + getUpperCaseFirst(id.replace("minecraft:", "").replace("_", " ").trim()) +
        '  </span>' +
        '  <div class="number">' + count + '</div>' +
        '</div>';

    return tmp;
  }

  function populate(originalData) {
    slotspace = document.getElementById("slotspace");
    hotbar = document.getElementById("slotspace2");
    armor = document.getElementById("slotspace3");

    dataArray = originalData.data;

    for (var i = 9; i < 36; i++) {
      if (dataArray.hasOwnProperty(i)) {
        addStackSize(dataArray[i]["id"], dataArray[i]["maxStack"]);
        elem = "<div class='slot' id='" + i + "'>" + getSlotTemplate(dataArray[i]["count"], dataArray[i]["id"], dataArray[i]["tag"], i) + "</div>"
      } else {
        elem = "<div class='slot' id='" + i + "'></div>"
      }
      slotspace.innerHTML += elem;
    }


    for (var i = 0; i < 9; i++) {
      if (dataArray.hasOwnProperty(i)) {
        addStackSize(dataArray[i]["id"], dataArray[i]["maxStack"]);
        elem = "<div class='slot' id='" + i + "'>" + getSlotTemplate(dataArray[i]["count"], dataArray[i]["id"], dataArray[i]["tag"], i) + "</div>"
      } else {
        elem = "<div class='slot' id='" + i + "'></div>"
      }
      hotbar.innerHTML += elem;
    }


    for (var i = 103; i >= 100 ; i--) {
      if (dataArray.hasOwnProperty(i)) {
        addStackSize(dataArray[i]["id"], dataArray[i]["maxStack"]);
        elem = "<div class='slot' id='" + i + "'>" + getSlotTemplate(dataArray[i]["count"], dataArray[i]["id"], dataArray[i]["tag"], i) + "</div>"
      } else {
        elem = "<div class='slot' id='" + i + "'></div>"
      }
      armor.innerHTML += elem;
    }
    if (dataArray.hasOwnProperty(-106)) {
      addStackSize(dataArray[-106]["id"], dataArray[-106]["maxStack"]);
      elem = "<div class='slot' id='-106'>" + getSlotTemplate(dataArray[-106]["count"], dataArray[-106]["id"], dataArray[-106]["tag"], -106) + "</div>"
    } else {
      elem = "<div class='slot' id='-106'></div>"
    }
    armor.innerHTML += elem;
    btn = "<button class='minecraft-button minecraft-button-right'>Save</button>"
    armor.innerHTML += btn;
  }
  function addStackSize(id, size) {
    stackSizeDict[id] = parseInt(size);
  }

  function getStackSize(id) {
    return stackSizeDict[id];
  }
}
