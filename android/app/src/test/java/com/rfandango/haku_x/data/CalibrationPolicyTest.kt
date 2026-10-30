package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class CalibrationPolicyTest {
  private val low=GraphicsOptions(1,false,"bilinear",120)
  private val high=GraphicsOptions(2,false,"bilinear",120)
  private fun metric(fps:Double,p95:Double=20.0)=CalibrationMetrics(30.0,(fps*30).toLong(),800,12.0,p95,100.0,50000.0,600.0)
  private fun run(o:GraphicsOptions,fps:Double,p95:Double=20.0)=CalibrationRun(o,metric(fps,p95))
  @Test fun comparesSameCeilingAndRepeatsReference() {
    val options=CalibrationPolicy.candidates(high.copy(fpsLimit=30),120)
    assertEquals(options.first(),options.last())
    assertEquals(options.size-1,options.dropLast(1).distinct().size)
    assertTrue(options.all { it.fpsLimit==120 })
  }
  @Test fun measuredFasterHigherScaleCanWin() {
    val result=CalibrationPolicy.winner(listOf(run(low,40.0),run(high,55.0),run(low,40.5)))
    assertEquals(high,result?.options)
  }
  @Test fun nearTieChoosesLowerLoad() {
    assertEquals(low,CalibrationPolicy.winner(listOf(run(low,59.0),run(high,60.0),run(low,59.0)))?.options)
  }
  @Test fun worseFramePacingLosesWithinFpsTolerance() {
    assertEquals(high,CalibrationPolicy.winner(listOf(run(low,60.0,35.0),run(high,59.0,21.0),run(low,60.0,35.0)))?.options)
  }
  @Test fun referenceDriftPreventsPublishing() {
    assertNull(CalibrationPolicy.winner(listOf(run(low,60.0),run(high,65.0),run(low,50.0))))
    assertNull(CalibrationPolicy.winner(listOf(run(low,60.0),run(high,65.0),run(low,60.0,30.0))))
  }
  @Test fun failedReferenceAndShortMeasurementCannotWin() {
    assertNull(CalibrationPolicy.winner(listOf(CalibrationRun(low,null,"failed"),run(high,65.0),run(low,60.0))))
    assertFalse(metric(60.0).copy(seconds=5.0).valid)
    assertFalse(metric(60.0).copy(cpuMs=Double.NaN).valid)
    assertFalse(metric(60.0).copy(peakRssMb=-1.0).valid)
    assertFalse(metric(60.0).copy(samples=2).valid)
  }
  @Test fun failedCandidateIsExcludedAndGpuTimeIsOptional() {
    val valid=run(low,60.0)
    assertTrue(valid.metrics!!.valid)
    assertEquals(-1.0,valid.metrics.gpuMs,0.0)
    assertEquals(low,CalibrationPolicy.winner(listOf(valid,CalibrationRun(high,null,"crash"),valid))?.options)
  }
}
