# Streaming Benchmarks

Benchmarks for streaming data types relevant to the Scala ecosystem.

The purpose of this repository isn't to judge the implementations based on these results — as a matter of fact, all included implementations are fast enough for most purposes.

The purpose of this repository is for highlighting implementation problems and possible improvements.

## Benchmarks

### ChunkedEvalFilterMapSumBenchmark

[See source](./src/main/scala/streaming/benchmarks/ChunkedEvalFilterMapSumBenchmark.scala).

Sample:

```scala
Iterant[Task]
  // 1: iteration
  .fromSeq(allElements)
  // 2: collect buffers
  .bufferTumbling(chunkSize)
  // 3: eval map
  .mapEval(seq => Task(seq.sum))
  // 4: filter
  .filter(_ > 0)
  // 5: map
  .map(_.toLong)
  // 6: foldLeft
  .foldLeftL(0L)(_ + _)
```

The purpose of this benchmark is the evaluation of a lazy task in chunks, coupled with the usual `filter`, `map` and a simple fold to compute a final result.

This is not very unlike real world use-cases. The test is designed to take advantage of chunking, if the implementation supports chunking, however by forcing lazy boundaries via lazy tasks we are forcing the participating implementations to actually break that work in smaller units, because aggressive fusion on pure streams is not very relevant for real world workloads.

## History of Results

- [2018-11-18](./results/2018-11-18/index.md)
