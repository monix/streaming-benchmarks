package streaming.benchmarks

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink => AkkaSink, Source => AkkaSource}
import cats.effect.{IO => CatsIO}
import fs2.{Chunk => FS2Chunk, Stream => FS2Stream}
import io.reactivex.{Flowable => RxFlowable, Single => RxSingle, Observable => RxObservable}
import monix.eval.{Coeval, Task}
import monix.execution.Scheduler
import monix.reactive.{Observable => MonixObservable}
import monix.tail.Iterant
import org.openjdk.jmh.annotations._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/** To run the benchmarks and record results:
  *
  *     ./run-benchmark ChunkFlatMapFilterMapSumBenchmark
  *
  * This will generate results in `./results`.
  *
  * Or to run the benchmark from within SBT:
  *
  *     jmh:run -i 10 -wi 10 -f 2 -t 1 streaming.benchmarks.ChunkFlatMapFilterMapSumBenchmark
  *
  * Which means "10 iterations", "10 warm-up iterations", "2 forks", "1 thread".
  * Please note that benchmarks should be usually executed at least in
  * 10 iterations (as a rule of thumb), but more is better.
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ChunkFlatMapFilterMapSumBenchmark {
  @Param(Array("10000"))
  var chunkCount: Int = _

  @Param(Array("5000"))
  var chunkSize: Int = _

  implicit val ec     = Scheduler.computation(name="benchmarks-ec")
  implicit val system = ActorSystem("benchmarks", defaultExecutionContext = Some(ec))
  implicit val mat    = ActorMaterializer()

  // Benchmarks need to work with the same chunks function, otherwise they
  // aren't measuring the same thing
  def buildChunks = {
    (1 to chunkCount).map(i => Array.fill(chunkSize)(i))
  }

  @TearDown
  def shutdown(): Unit = {
    ec.shutdown()
    system.terminate()
    ()
  }

  @Benchmark
  def akkaStreams = {
    val chunks = buildChunks

    val program = AkkaSource
      // 0: iteration
      .fromIterator(() => chunks.iterator)
      // 1: flatMap
      .flatMapConcat(array => AkkaSource.fromIterator(() => array.iterator))
      // 2: filter
      .filter(_ % 2 == 0)
      // 3: map
      .map(_.toLong)
      // 4: fold
      .toMat(AkkaSink.fold(0L)(_ + _))(Keep.right)
    Await.result(program.run, Duration.Inf)
  }

  @Benchmark
  def fs2Stream = {
    val chunks = buildChunks

    val stream = FS2Stream
      // 0: iteration
      .apply(chunks: _*)
      // 1: flatMap
      .flatMap(chunk => FS2Stream.chunk(FS2Chunk.array(chunk)))
      // 2: filter
      .filter(_ % 2 == 0)
      // 3: map
      .map(_.toLong)
      .covary[CatsIO]
      .compile
      // 4: foldLeft
      .fold(0L)(_ + _)
    stream.unsafeRunSync
  }

  @Benchmark
  def monixIterantTask: Long = {
    val chunks = buildChunks
    val stream = Iterant[Task]
      // 0: iteration
      .fromIndexedSeq(chunks)
      // 1: flatMap
      .flatMap(Iterant[Task].fromArray)
      // 2: filter
      .filter(_ % 2 == 0)
      // 3: map
      .map(_.toLong)
      // 4: foldLeft
      .foldLeftL(0L)(_ + _)

    stream.runSyncUnsafe()
  }

  @Benchmark
  def monixIterantCoeval: Long = {
    val chunks = buildChunks
    val stream = Iterant[Coeval]
      // 0: iteration
      .fromIndexedSeq(chunks)
      // 1: flatMap
      .flatMap(Iterant[Coeval].fromArray(_))
      // 2: filter
      .filter(_ % 2 == 0)
      // 3: map
      .map(_.toLong)
      // 4: foldLeft
      .foldLeftL(0L)(_ + _)

    stream.value()
  }

  @Benchmark
  def monixObservable: Long = {
    val chunks = buildChunks
    val stream = MonixObservable
      // 0: iteration
      .fromIterable(chunks)
      // 1: flatMap
      .flatMap(MonixObservable.fromIterable(_))
      // 2: filter
      .filter(_ % 2 == 0)
      // 3: map
      .map(_.toLong)
      // 4: foldLeft
      .foldLeftL(0L)(_ + _)

    stream.runSyncUnsafe()
  }

  @Benchmark
  def rxJavaFlowable: Long = {
    import scala.collection.JavaConverters._

    val chunks = buildChunks
    val stream: RxSingle[Long] = RxFlowable
      // 0: iteration
      .fromIterable(chunks.asJava)
      // 1: flatMap
      .concatMap((chunk: Array[Int]) => RxFlowable.fromArray(chunk:_*))
      // 2: filter
      .filter(x => x % 2 == 0)
      // 3: map
      .map[Long](x => x.toLong)
      // 4: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    stream.blockingGet()
  }

  @Benchmark
  def rxJavaObservable: Long = {
    import scala.collection.JavaConverters._

    val chunks = buildChunks
    val stream: RxSingle[Long] = RxObservable
      // 0: iteration
      .fromIterable(chunks.asJava)
      // 1: flatMap
      .concatMap((chunk: Array[Int]) => RxObservable.fromArray(chunk:_*))
      // 2: filter
      .filter(x => x % 2 == 0)
      // 3: map
      .map[Long](x => x.toLong)
      // 4: foldLeft
      .reduce[Long](0L, (a, b) => a + b)

    stream.blockingGet()
  }
}
