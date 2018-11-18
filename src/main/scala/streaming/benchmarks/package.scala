package streaming

import streaming.benchmarks.utils.RxBlockOnResultObserver

package object benchmarks {

  def awaitRxSingle[A](single: io.reactivex.Single[A]): A = {
    val c = new RxBlockOnResultObserver[A]
    single.subscribe(c)
    c.await()
  }

  def sumIntScala(seq: Iterable[Int]): Int = {
    val cursor = seq.iterator
    var sum = 0
    while (cursor.hasNext) {
      sum += cursor.next()
    }
    sum
  }

  def sumIntJava(seq: java.lang.Iterable[Int]): Int = {
    val cursor = seq.iterator
    var sum = 0
    while (cursor.hasNext) {
      sum += cursor.next()
    }
    sum
  }
}
