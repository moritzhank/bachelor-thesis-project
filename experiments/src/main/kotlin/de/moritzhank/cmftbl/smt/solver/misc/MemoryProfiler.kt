@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import kotlin.math.max
import kotlin.math.min
import oshi.SystemInfo
import oshi.software.os.OSProcess
import kotlin.math.pow

/**
 * Profile the memory used by the system and a particular process.
 * NOTE: Memory in swap is not recorded! (See [oshi.software.os.OSProcess.getResidentSetSize])
 */
class MemoryProfiler private constructor(private val pid: Int, val sampleRate: Int) {

  val maxSysMemUsagePercent: Double
  val maxProcMemUsageBytes: Long
  val numSamples: Int
  val elapsedMillis: Long
  val delayMillis: Long

  init {
    var minSysMemAvailable: Long = -1
    var maxProcMemUsageBytes: Long = -1
    var numSamples = 0
    val startMillis = System.currentTimeMillis()
    var sysInfo = SystemInfo()
    val totalMem = sysInfo.hardware.memory.total
    var proc: OSProcess? = sysInfo.operatingSystem.getProcess(pid)
    delayMillis = System.currentTimeMillis() - startMillis
    var t1 = System.currentTimeMillis()
    while (proc != null) {
      if ((System.currentTimeMillis() - t1) < sampleRate) {
        continue
      }
      t1 = System.currentTimeMillis()
      minSysMemAvailable = if (minSysMemAvailable == -1L) {
        sysInfo.hardware.memory.available
      } else  {
        min(sysInfo.hardware.memory.available, minSysMemAvailable)
      }
      maxProcMemUsageBytes = max(proc.residentSetSize, maxProcMemUsageBytes)
      numSamples++
      try {
        require(proc.updateAttributes())
        sysInfo = SystemInfo()
      } catch (_: Exception) {
        break
      }
    }
    this.maxSysMemUsagePercent = if(minSysMemAvailable == -1L) {
      -1.0
    } else {
      (totalMem - minSysMemAvailable).toDouble() / totalMem
    }
    this.numSamples = numSamples
    this.maxProcMemUsageBytes = maxProcMemUsageBytes
    elapsedMillis = System.currentTimeMillis() - startMillis
  }

  companion object {
    fun start(pid: Int, sampleRate: Int = 100) = MemoryProfiler(pid, sampleRate)
    fun bytesToGB(bytes: Long) = bytes * 10.0.pow(-9)
  }
}
