wookiee
================
* [wookiee-grpc](#wookiee-grpc)

# wookiee-grpc
## Install
wookiee-grpc is available for Scala 2.12 and 2.13. There are no plans to support scala 2.11 or lower.
```sbt
libraryDependencies += "com.oracle.infy" %% "wookiee-grpc" % "@VERSION@"
```

## Setup ScalaPB
We use [ScalaPB](https://github.com/scalapb/ScalaPB) to generate source code from a `.proto` file. You can use
other plugins/code generators if you wish. wookiee-grpc will work as long as you have `io.grpc.ServerServiceDefinition`
for the server and something that accept `io.grpc.ManagedChannel` for the client.

Declare your gRPC service using proto3 syntax and save it in `@PROTO_FILE@`
```proto
@PROTO_DEF@
```

Add ScalaPB plugin to `plugin.sbt` file
```sbt
@PLUGIN_DEF@
```

Configure the project in `build.sbt` so that ScalaPB can generate code
```sbt
@PROJECT_DEF@
```

In the sbt shell, type `protocGenerate` to generate scala code based on the `.proto` file. ScalaPB will generate
code and put it under `target/scala-2.13/src_managed/main`.

## Using wookiee-grpc
After the code has been generated by ScalaPB, you can use wookiee-grpc for service discoverability and load balancing.

