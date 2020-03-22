Benchmark                                          (chunkCount)  (chunkSize)   Mode  Cnt   Score   Error  Units
ChunkedEvalFilterMapSumBenchmark.akkaStreams               1000         1000  thrpt   10  12,598 ± 0,053  ops/s
ChunkedEvalFilterMapSumBenchmark.fs2Stream                 1000         1000  thrpt   10  50,921 ± 0,414  ops/s
ChunkedEvalFilterMapSumBenchmark.fs2StreamZio              1000         1000  thrpt   10  46,359 ± 0,353  ops/s
ChunkedEvalFilterMapSumBenchmark.monixIterant              1000         1000  thrpt   10  65,410 ± 1,199  ops/s
ChunkedEvalFilterMapSumBenchmark.monixObservable           1000         1000  thrpt   10  96,423 ± 1,953  ops/s
ChunkedEvalFilterMapSumBenchmark.rxJavaFlowable            1000         1000  thrpt   10  49,761 ± 0,688  ops/s
ChunkedEvalFilterMapSumBenchmark.rxJavaObservable          1000         1000  thrpt   10  68,869 ± 0,805  ops/s
ChunkedEvalFilterMapSumBenchmark.zioStream                 1000         1000  thrpt   10   4,015 ± 0,046  ops/s
