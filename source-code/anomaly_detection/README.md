# Anomaly Detection — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Anomaly Detection Machine Learning Example](https://leanpub.com/read/clojureai/leanpub-auto-anomaly-detection-machine-learning-example) — free to read online.

This example demonstrates statistical anomaly detection using Clojure. It reads CSV data, transforms values into approximate Gaussian distributions, and uses a custom Java `AnomalyDetection` class (via Java interop) to flag outliers. The [Incanter](https://github.com/incanter/incanter) library is used for statistics and optional histogram plotting.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |

## Run

    lein run

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
