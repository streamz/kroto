package io.streamz.kroto.impl

object XorShiftRng {
  private val RND = new ThreadLocal[XorShiftRng] {
    override def initialValue() = new XorShiftRng
  }
  def nextInt(until: Int) = RND.get().nextInt(until)
}

private class XorShiftRng {
  private var seed = System.nanoTime()

  def nextInt(n: Int) = {
    if (n <= 0) throw new IllegalArgumentException
    ((nextLong >>> 1) % n).toInt
  }

  def nextLong = {
    seed ^= seed >>> 12
    seed ^= seed << 25
    Long.MaxValue * {
      seed ^= (seed >>> 27)
      seed
    }
  }
}
