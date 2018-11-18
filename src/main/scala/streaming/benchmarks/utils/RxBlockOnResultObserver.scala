package streaming.benchmarks.utils

import java.util.concurrent.CountDownLatch

class RxBlockOnResultObserver[A] extends io.reactivex.functions.BiConsumer[A, Throwable] {
  private[this] val latch = new CountDownLatch(1)
  private[this] var result: A = _
  private[this] var error: Throwable = _

  def accept(t1: A, t2: Throwable): Unit = {
    result = t1
    error = t2
    latch.countDown()
  }

  def await(): A = {
    latch.await()
    if (error != null) throw error
    result
  }
}
