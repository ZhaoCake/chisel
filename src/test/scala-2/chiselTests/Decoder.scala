// SPDX-License-Identifier: Apache-2.0

package chiselTests

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.simulator.stimulus.RunUntilFinished
import chisel3.util.{BitPat, Counter}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.propspec.AnyPropSpec

class Decoder(bitpats: List[String]) extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val matched = Output(Bool())
  })
  io.matched := VecInit(bitpats.map(BitPat(_) === io.inst)).reduce(_ || _)
}

class DecoderTester(pairs: List[(String, String)]) extends Module {
  val (insts, bitpats) = pairs.unzip
  val (cnt, wrap) = Counter(true.B, pairs.size)
  val dut = Module(new Decoder(bitpats))
  dut.io.inst := VecInit(insts.map(_.asUInt))(cnt)
  when(!dut.io.matched) {
    assert(cnt === 0.U)
    stop()
  }
  when(wrap) {
    stop()
  }
}

class DecoderSpec extends AnyPropSpec with PropertyUtils with ChiselSim {

  // Use a single Int to make both a specific instruction and a BitPat that will match it
  val bitpatPair = for (seed <- Arbitrary.arbitrary[Int]) yield {
    val rnd = new scala.util.Random(seed)
    val bs = seed.toBinaryString
    val bp = bs.map(if (rnd.nextBoolean()) _ else "?")

    // The following randomly throws in white space and underscores which are legal and ignored.
    val bpp = bp.map { a =>
      if (rnd.nextBoolean()) {
        a.toString
      } else {
        a.toString + (if (rnd.nextBoolean()) "_" else " ")
      }
    }.mkString

    ("b" + bs, "b" + bpp)
  }
  private def nPairs(n: Int) = Gen.containerOfN[List, (String, String)](n, bitpatPair)

  property("BitPat wildcards should be usable in decoding") {
    forAll(nPairs(4)) { (pairs: List[(String, String)]) =>
      simulate { new DecoderTester(pairs) }(RunUntilFinished(pairs.size + 1))
    }
  }
}
