package com.nightlynexus

import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.dom.createElement
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get

fun main() {
  val tileStorage = TileStorage(localStorage)

  val allTileElements = ArrayList<TileElement>(allTiles.size)

  val grid = document.getElementById("grid-container")!!
  val inPileCountDisplay = document.getElementById("in-pile-count")!!
  val usedCountDisplay = document.getElementById("used-count")!!
  val sortUsedCheckbox = document.getElementById("sort-used") as HTMLInputElement
  val resetButton = document.getElementById("reset") as HTMLButtonElement

  val shownTilesCount = ShownTilesCount(0, 0)

  resetButton.addEventListener("click", {
    if (window.confirm("Reset all tiles?")) {
      for (tileElement in allTileElements) {
        if (tileElement.tile !== Tonga) {
          if (tileElement.used) {
            tileElement.used = false
          }
        }
      }

      resetButton.disabled = true
    }
  })

  val defaultComparator = Comparator<TileElement> { a, b ->
    a.ordinal - b.ordinal
  }
  val sortUsedComparator = Comparator<TileElement> { a, b ->
    if (a.used && !b.used) {
      1
    } else if (!a.used && b.used) {
      -1
    } else {
      a.ordinal - b.ordinal
    }
  }

  var sortUsed = tileStorage.getSortUsedStart()
  sortUsedCheckbox.addEventListener("change", {
    sortUsed = (it.target as HTMLInputElement).checked
    allTileElements.sortWith(if (sortUsed) sortUsedComparator else defaultComparator)
    val gridChildren = grid.childNodes
    for (i in gridChildren.length - 1 downTo 0) {
      grid.removeChild(gridChildren[i]!!)
    }
    for (tileElement in allTileElements) {
      grid.appendChild(tileElement.element)
    }

    tileStorage.setSortUsed(sortUsed)
  })
  val tileElementListener = object : TileElement.Listener {
    override fun onUsedChanged(tileElement: TileElement, used: Boolean) {
      if (sortUsed) {
        allTileElements.sortWith(sortUsedComparator)
        val gridChildren = grid.childNodes
        for (i in gridChildren.length - 1 downTo 0) {
          grid.removeChild(gridChildren[i]!!)
        }
        for (tileElementToAppend in allTileElements) {
          grid.appendChild(tileElementToAppend.element)
        }
      }
      if (used) {
        shownTilesCount.inPile--
        shownTilesCount.used++
      } else {
        shownTilesCount.inPile++
        shownTilesCount.used--
      }
      inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
      usedCountDisplay.textContent = shownTilesCount.used.toString()

      resetButton.disabled = shownTilesCount.used == 1

      tileStorage.setUsed(tileElement.ordinal, used)
    }
  }

  for (i in allTiles.indices) {
    val tile = allTiles[i]
    val isSource = tile === Tonga
    val used = isSource || tileStorage.getUsedStart(i)
    val baseTileElement = grid.createTile(
      tile,
      i,
      used,
      shownTilesCount,
      tileElementListener
    )
    allTileElements += baseTileElement
  }

  inPileCountDisplay.textContent = shownTilesCount.inPile.toString()
  usedCountDisplay.textContent = shownTilesCount.used.toString()

  resetButton.disabled = shownTilesCount.used == 1

  // This does not call sortUsedCheckbox's change event listener, so we have to do an initial sort.
  sortUsedCheckbox.checked = sortUsed
  allTileElements.sortWith(if (sortUsed) sortUsedComparator else defaultComparator)
  val gridChildren = grid.childNodes
  for (i in gridChildren.length - 1 downTo 0) {
    grid.removeChild(gridChildren[i]!!)
  }
  for (tileElement in allTileElements) {
    grid.appendChild(tileElement.element)
  }
}

private class ShownTilesCount(var inPile: Int, var used: Int)

private fun Element.createTile(
  tile: Tile,
  ordinal: Int,
  used: Boolean,
  shownTilesCount: ShownTilesCount,
  listener: TileElement.Listener
): TileElement {
  if (used) {
    shownTilesCount.used++
  } else {
    shownTilesCount.inPile++
  }
  return TileElement(
    tile,
    ownerDocument!!,
    ordinal,
    used,
    listener
  )
}

private class TileElement(
  val tile: Tile,
  ownerDocument: Document,
  val ordinal: Int,
  used: Boolean,
  private val listener: Listener,
) {
  interface Listener {
    fun onUsedChanged(tileElement: TileElement, used: Boolean)
  }

  var used = used
    set(value) {
      check(value != field)
      field = value
      val grayscale = if (value) "100%" else "0%"
      imageElement.setAttribute("style", "filter: grayscale($grayscale);")
      listener.onUsedChanged(this, value)
    }

  // This is a val, but createElement doesn't have a Kotlin contract
  // to promise the compiler the capture is run once.
  private var imageElement: Element

  val element = ownerDocument.createElement("figure") {
    imageElement = appendElement("img") {
      setAttribute("src", tile.path)

      val grayscale = if (this@TileElement.used) "100%" else "0%"
      setAttribute("style", "filter: grayscale($grayscale);")

      if (tile !== Tonga) {
        addEventListener("click", {
          this@TileElement.used = !this@TileElement.used
        })
      }
    }
    appendElement("figcaption") {
      if (tile === Tonga) {
        appendElement("b") {
          appendText(tile.name)
        }
      } else if (tile is LandTile) {
        appendElement("b") {
          appendText("(")
          appendText(tile.points.toString())
          appendText(")")
        }
        appendText(" ")
        appendText(tile.name)
      }
    }
  }
}

private val Tonga = LandTile(
  "tiles/land/tonga.png",
  "Tonga",
  0,
  6
)

private val allTiles = listOf(
  Tonga,
  LandTile(
    "tiles/land/fidschi.png",
    "Fidschi",
    5,
    3
  ),
  LandTile(
    "tiles/land/samoa.png",
    "Samoa",
    5,
    3
  ),
  LandTile(
    "tiles/land/hawaii.png",
    "Hawaii",
    5,
    3
  ),
  LandTile(
    "tiles/land/oahu.png",
    "Oahu",
    4,
    3
  ),
  LandTile(
    "tiles/land/hiva_oa.png",
    "Hiva Oa",
    4,
    3
  ),
  LandTile(
    "tiles/land/tuvalu.png",
    "Tuvalu",
    4,
    3
  ),
  LandTile(
    "tiles/land/mangareva.png",
    "Mangareva",
    4,
    3
  ),
  LandTile(
    "tiles/land/tahiti.png",
    "Tahiti",
    4,
    3
  ),
  LandTile(
    "tiles/land/tokelau.png",
    "Tokelau",
    3,
    3
  ),
  LandTile(
    "tiles/land/rapa_nui.png",
    "Rapa Nui",
    3,
    2
  ),
  LandTile(
    "tiles/land/tuamotu.png",
    "Tuamotu",
    3,
    2
  ),
  LandTile(
    "tiles/land/rarotonga.png",
    "Rarotonga",
    3,
    2
  ),
  LandTile(
    "tiles/land/muroroa.png",
    "Muroroa",
    2,
    3
  ),
  LandTile(
    "tiles/land/nauru.png",
    "Nauru",
    2,
    3
  ),
  LandTile(
    "tiles/land/tubuai.png",
    "Tubuai",
    2,
    2
  ),
  WaterTile(
    "tiles/water/water_a.png",
    4
  ),
  WaterTile(
    "tiles/water/water_b.png",
    4
  ),
  WaterTile(
    "tiles/water/water_c.png",
    4
  ),
  WaterTile(
    "tiles/water/water_d.png",
    3
  ),
  WaterTile(
    "tiles/water/water_e.png",
    3
  ),
  WaterTile(
    "tiles/water/water_f.png",
    3
  ),
  WaterTile(
    "tiles/water/water_g.png",
    3
  ),
  WaterTile(
    "tiles/water/water_h.png",
    3
  ),
  WaterTile(
    "tiles/water/water_i.png",
    2
  ),
  WaterTile(
    "tiles/water/water_j.png",
    2
  ),
  WaterTile(
    "tiles/water/water_k.png",
    2
  ),
  WaterTile(
    "tiles/water/water_l.png",
    0
  ),
  WaterTile(
    "tiles/water/water_m.png",
    0
  ),
  WaterTile(
    "tiles/water/water_n.png",
    0
  ),
  WaterTile(
    "tiles/water/water_o.png",
    0
  ),
  WaterTile(
    "tiles/water/water_p.png",
    0
  )
)
