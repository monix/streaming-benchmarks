package streaming

import streaming.benchmarks.utils.RxBlockOnResultObserver

package object benchmarks {

  def awaitRxSingle[A](single: io.reactivex.Single[A]): A = {
    val c = new RxBlockOnResultObserver[A]
    single.subscribe(c)
    c.await()
  }
}
