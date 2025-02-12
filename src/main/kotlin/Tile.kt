package com.nightlynexus

internal sealed interface Tile {
  val path: String
}

internal data class LandTile(
  override val path: String,
  val name: String,
  val points: Int,
  val beaches: Int
) : Tile

internal data class WaterTile(
  override val path: String,
  val placementRequirement: Int
) : Tile
