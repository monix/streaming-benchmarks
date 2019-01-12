# Results 2018-11-18

## ChunkedEvalFilterMapSumBenchmark

Results:

| Library            | Stream type + IO type          | ops/s  | error |
|--------------------|--------------------------------|--------|-------|
| monix  3.0.0-RC2   | Observable + MonixTask         | 73.640 | 1.172 |
| rxjava 2.2.5       | Observable + Single + Callable | 63.492 | 0.877 |
| monix 3.0.0-RC2    | Iterant + MonixTask            | 56.860 | 1.029 |
| rxjava 2.2.5       | Flowable + Single + Callable   | 55.451 | 0.392 | 
| fs2-core 1.0.2     | Stream + MonixTask             | 34.748 | 0.339 |
| akka-stream 2.5.19 | Source + scala Future          | 11.725 | 0.028 |
