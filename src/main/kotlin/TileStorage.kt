package com.nightlynexus

import org.w3c.dom.Storage
import org.w3c.dom.get
import org.w3c.dom.set

internal class TileStorage(private val storage: Storage) {
  private val currentVersion = "1"
  private val isCurrentVersion = run {
    val version = storage["version"]
    if (version == null || version != currentVersion) {
      storage.clear()
      storage["version"] = currentVersion
      false
    } else {
      true
    }
  }

  fun getUsedStart(ordinal: Int): Boolean {
    if (!isCurrentVersion) return false
    val used = storage["used_$ordinal"] ?: return false
    return used.toBoolean()
  }

  fun getSortUsedStart(): Boolean {
    if (!isCurrentVersion) return false
    val sortUsed = storage["sort_used"] ?: return false
    return sortUsed.toBoolean()
  }

  fun setUsed(ordinal: Int, used: Boolean) {
    storage["used_$ordinal"] = used.toString()
  }

  fun setSortUsed(sortUsed: Boolean) {
    storage["sort_used"] = sortUsed.toString()
  }
}
