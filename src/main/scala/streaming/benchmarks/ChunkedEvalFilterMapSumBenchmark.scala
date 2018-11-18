package streaming.benchmarks

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink => AkkaSink, Source => AkkaSource}
import cats.effect.{IO => CatsIO}
import fs2.{Chunk => FS2Chunk, Stream => FS2Stream}
import io.reactivex.{Flowable => RxFlowable, Observable => RxObservable, Single => RxSingle}
import monix.eval.{Task => MonixTask}
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.Scheduler
import monix.execution.schedulers.TrampolineExecutionContext.immediate
import monix.reactive.{Observable => MonixObservable}
import monix.tail.Iterant
import monix.tail.batches.Batch
import org.openjdk.jmh.annotations._

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Benchmark designed to execute these operations:
  *
  *  1. iteration over an iterable
  *  2. sliding window of fixed size
  *  3. mapEval / flatMap
  *  4. filter
  *  5. map
  *  6. foldLeft
  *
  * If the benchmark does not execute these and exactly these operations,
  * then the measurement is not measuring the same thing across the board.
  *
  * To run the benchmarks and record results:
  *
  *     ./run-benchmark ChunkedEvalFilterMapSumBenchmark
  *
  * This will generate results in `./results`.
  *
  * Or to run the benchmark from within SBT:
  *
  *     jmh:run -i 10 -wi 10 -f 2 -t 1 streaming.benchmarks.ChunkedEvalFilterMapSumBenchmark
  *
  * Which means "10 iterations", "10 warm-up iterations", "2 forks", "1 thread".
  * Please note that benchmarks should be usually executed at least in
  * 10 iterations (as a rule of thumb), but more is better.
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ChunkedEvalFilterMapSumBenchmark {
  @Param(Array("1000"))
  var chunkCount: Int = _

  @Param(Array("1000"))
  var chunkSize: Int = _

  implicit val ec = Scheduler.computation(
    name = "benchmarks-ec",
    daemonic = false,
    executionModel = SynchronousExecution
  )

  implicit val system = ActorSystem("benchmarks", defaultExecutionContext = Some(ec))
  implicit val mat = ActorMaterializer()

  // All events that need to be streamed
  var allElements: IndexedSeq[Int] = _
  // For ensuring we get the result we expect
  var expectedSum: Long = _

  @Setup
  def setup(): Unit = {
    val chunks = (1 to chunkCount).map(i => Array.fill(chunkSize)(i))
    allElements = chunks.flatten
    expectedSum = allElements.sum
  }

  @TearDown
  def shutdown(): Unit = {
    system.terminate()
    ec.shutdown()
    ec.awaitTermination(10.seconds)
    ()
  }

  @Benchmark
  def akkaStreams = {
    // N.B. Akka Streams don't do chunks
    val program = AkkaSource
      // 1: iteration
      .fromIterator(() => allElements.iterator)
      // 2: collect buffers
      .sliding(chunkSize, chunkSize)
      // 3: async processing
      .mapAsync(1)(seq => Future(seq.sum)(immediate))
      // 3: filter
      .filter(_ > 0)
      // 4: map
      .map(_.toLong)
      // 5: fold
      .toMat(AkkaSink.fold(0L)(_ + _))(Keep.right)

    testResult(Await.result(program.run, Duration.Inf))
  }

  @Benchmark
  def fs2Stream = {
    val stream = FS2Stream
      // 1: iteration
      .apply(allElements:_*)
      // 2: collect buffers
      .chunkN(chunkSize)
      // 3: eval map
      .evalMap[MonixTask, Int](chunk => MonixTask(chunk.foldLeft(0)(_ + _)))
      // 4: filter
      .filter(_ > 0)
      // 5: map
      .map(_.toLong)
      .compile
      // 6: foldLeft
      .fold(0L)(_ + _)

    testResult(stream.runSyncUnsafe())
  }

  @Benchmark
  def monixIterant: Long = {
    val stream = Iterant[MonixTask]
      // 1: iteration
      .fromSeq(allElements)
      // 2: collect buffers
      .bufferTumbling(chunkSize)
      // 3: eval map
      .mapEval(seq => MonixTask(sumIntScala(seq)))
      // 4: filter
      .filter(_ > 0)
      // 5: map
      .map(_.toLong)
      // 6: foldLeft
      .foldLeftL(0L)(_ + _)

    testResult(stream.runSyncUnsafe())
  }

  @Benchmark
  def monixObservable: Long = {
    // N.B. chunks aren't needed for Monix's Observable ;-)
    val stream = MonixObservable
      // 1: iteration
      .fromIterable(allElements)
      // 2: collect buffers
      .bufferTumbling(chunkSize)
      // 3: eval map
      .mapEval[Int](seq => MonixTask(sumIntScala(seq)))
      // 4: filter
      .filter(_ > 0)
      // 5: map
      .map(_.toLong)
      // 6: foldLeft
      .foldLeftL(0L)(_ + _)

    testResult(stream.runSyncUnsafe())
  }

  @Benchmark
  def rxJavaFlowable: Long = {
    import scala.collection.JavaConverters._
    // N.B. chunks aren't needed for Rx's Flowable ;-)
    val stream: RxSingle[Long] = RxFlowable
      // 1: iteration
      .fromIterable(allElements.asJava)
      // 2: collect buffers
      .buffer(chunkSize)
      // 3: eval map
      .concatMapSingle(x => RxSingle.fromCallable(() => sumIntJava(x)))
      // 4: filter
      .filter(_ > 0)
      // 5: map
      .map[Long](x => x.toLong)
      // 6: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    testResult(stream.blockingGet())
  }

  @Benchmark
  def rxJavaObservable: Long = {
    import scala.collection.JavaConverters._
    // N.B. chunks aren't needed for Rx's Observable ;-)
    val stream: RxSingle[Long] = RxObservable
      // 1: iteration
      .fromIterable(allElements.asJava)
      // 2: collect buffers
      .buffer(chunkSize)
      // 3: eval map
      .concatMapSingle(x => RxSingle.fromCallable(() => sumIntJava(x)))
      // 4: filter
      .filter(_ > 0)
      // 5: map
      .map[Long](x => x.toLong)
      // 6: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    testResult(awaitRxSingle(stream))
  }

  def testResult(r: Long): Long = {
    if (r == 0 || r != expectedSum) {
      throw new RuntimeException(s"received: $r != expected: $expectedSum")
    }
    r
  }
}
