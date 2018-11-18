package streaming.benchmarks

import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink => AkkaSink, Source => AkkaSource}
import cats.effect.{IO => CatsIO}
import fs2.{Chunk => FS2Chunk, Stream => FS2Stream}
import io.reactivex.{Flowable => RxFlowable, Observable => RxObservable, Single => RxSingle}
import monix.execution.Scheduler
import monix.reactive.{Observable => MonixObservable}
import monix.tail.Iterant
import monix.tail.batches.Batch
import org.openjdk.jmh.annotations._
import scala.collection.immutable.IndexedSeq
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Benchmark designed to execute these operations:
  *
  *  1. iteration over an iterable
  *  2. if data type has chunks, do chunks, by whatever method is necessary
  *  3. filter
  *  4. map
  *  5. foldLeft
  *
  * If the benchmark does not execute these and exactly these operations,
  * then the measurement is not measuring the same thing across the board.
  *
  * To run the benchmarks and record results:
  *
  *     ./run-benchmark ChunkedFilterMapSumBenchmark
  *
  * This will generate results in `./results`.
  *
  * Or to run the benchmark from within SBT:
  *
  *     jmh:run -i 10 -wi 10 -f 2 -t 1 streaming.benchmarks.ChunkedFilterMapSumBenchmark
  *
  * Which means "10 iterations", "10 warm-up iterations", "2 forks", "1 thread".
  * Please note that benchmarks should be usually executed at least in
  * 10 iterations (as a rule of thumb), but more is better.
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ChunkedFilterMapSumBenchmark {
  @Param(Array("1000"))
  var chunkCount: Int = _

  @Param(Array("1000"))
  var chunkSize: Int = _

  implicit val ec     = Scheduler.computation(name="benchmarks-ec", daemonic = false)
  implicit val system = ActorSystem("benchmarks", defaultExecutionContext = Some(ec))
  implicit val mat    = ActorMaterializer()

  // Benchmarks need to work with the same chunks, otherwise they
  // aren't measuring the same thing
  var chunks: IndexedSeq[Array[Int]] = _
  // For the streaming data types that don't do chunks
  var flatChunks: IndexedSeq[Int] = _
  // For ensuring we get the result we expect
  var wholeSum: Long = _

  @Setup
  def setup(): Unit = {
    chunks = (1 to chunkCount).map(i => Array.fill(chunkSize)(i))
    flatChunks = chunks.flatten
    wholeSum = flatChunks.foldLeft(0L) { (acc, e) => if (e % 2 == 0) acc + e else acc }
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
      .fromIterator(() => flatChunks.iterator)
      // 3: filter
      .filter(_ % 2 == 0)
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
      .apply(chunks: _*)
      // 2: chunks
      .flatMap(chunk => FS2Stream.chunk(FS2Chunk.array(chunk)))
      // 3: filter
      .filter(_ % 2 == 0)
      // 4: map
      .map(_.toLong)
      .covary[CatsIO]
      .compile
      // 5: foldLeft
      .fold(0L)(_ + _)

    testResult(stream.unsafeRunSync)
  }

  @Benchmark
  def monixIterant: Long = {
    val stream = Iterant[CatsIO]
      // 1: iteration
      .fromSeq(chunks)
      // 2: chunk
      .mapBatch(Batch.fromArray(_))
      // 3: filter
      .filter(_ % 2 == 0)
      // 4: map
      .map(_.toLong)
      // 5: foldLeft
      .foldLeftL(0L)(_ + _)

    testResult(stream.unsafeRunSync())
  }

  @Benchmark
  def monixObservable: Long = {
    // N.B. chunks aren't needed for Monix's Observable ;-)
    val stream = MonixObservable
      // 1: iteration
      .fromIterable(flatChunks)
      // 3: filter
      .filter(_ % 2 == 0)
      // 4: map
      .map(_.toLong)
      // 5: foldLeft
      .foldLeftL(0L)(_ + _)

    testResult(stream.runSyncUnsafe())
  }

  @Benchmark
  def rxJavaFlowable: Long = {
    import scala.collection.JavaConverters._
    // N.B. chunks aren't needed for Rx's Flowable ;-)
    val stream: RxSingle[Long] = RxFlowable
      // 1: iteration
      .fromIterable(flatChunks.asJava)
      // 3: filter
      .filter(x => x % 2 == 0)
      // 4: map
      .map[Long](x => x.toLong)
      // 5: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    testResult(stream.blockingGet())
  }

  @Benchmark
  def rxJavaObservable: Long = {
    import scala.collection.JavaConverters._
    // N.B. chunks aren't needed for Rx's Observable ;-)
    val stream: RxSingle[Long] = RxObservable
      // 1: iteration
      .fromIterable(flatChunks.asJava)
      // 3: filter
      .filter(x => x % 2 == 0)
      // 4: map
      .map[Long](x => x.toLong)
      // 5: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    testResult(awaitRxSingle(stream))
  }

  def testResult(r: Long): Long = {
    if (r == 0 || r != wholeSum) {
      throw new RuntimeException(s"received: $r != expected: $wholeSum")
    }
    r
  }
}
