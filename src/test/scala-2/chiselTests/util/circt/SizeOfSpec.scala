// SPDX-License-Identifier: Apache-2.0

package chiselTests.util.circt

import chisel3._
import chisel3.util.circt.SizeOf
import circt.stage.ChiselStage

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

private class SizeOfBundle extends Bundle {
  val a = UInt()
  val b = SInt()
}

private class SizeOfTop extends Module {
  val w = IO(Input(UInt(65.W)))
  val x = IO(Input(new SizeOfBundle))
  val outw = IO(Output(UInt(32.W)))
  val outx = IO(Output(UInt(32.W)))
  outw := SizeOf(w)
  outx := SizeOf(x)
}

class SizeOfSpec extends AnyFlatSpec with Matchers {
  it should "work for types" in {
    val fir = ChiselStage.emitCHIRRTL(new SizeOfTop)
    (fir.split('\n').map(_.takeWhile(_ != '@').trim) should contain)
      .allOf(
        "node _outw_T = intrinsic(circt_sizeof : UInt<32>, w)",
        "node _outx_T = intrinsic(circt_sizeof : UInt<32>, x)"
      )
  }
}
