# Results 2020-03-22

## ChunkedEvalFilterMapSumBenchmark

Results:

| Library          | ops/s  | error |
| ---------------- | ------ | ----- |
| akkaStreams      | 12,598 | 0,053 |
| fs2Stream        | 50,921 | 0,414 |
| fs2StreamZio     | 46,359 | 0,353 |
| monixIterant     | 65,410 | 1,199 |
| monixObservable  | 96,423 | 1,953 |
| rxJavaFlowable   | 49,761 | 0,688 |
| rxJavaObservable | 68,869 | 0,805 |
| zioStream        | 4,015  | 0,046 |
