package com.oracle.infy.wookiee.grpc

import java.io.File
import java.nio.file.Paths

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ContextShift, Fiber, IO, Timer}
import com.oracle.infy.wookiee.grpc.impl.BearerTokenAuthenticator
import com.oracle.infy.wookiee.grpc.impl.GRPCUtils._
import com.oracle.infy.wookiee.grpc.json.HostSerde
import com.oracle.infy.wookiee.grpc.settings.{SSLServerSettings, ServerSettings}
import com.oracle.infy.wookiee.model.{Host, HostMetadata}
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import io.grpc.netty.shaded.io.grpc.netty.{GrpcSslContexts, NettyServerBuilder}
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel
import io.grpc.netty.shaded.io.netty.handler.ssl.{ClientAuth, SslContext, SslContextBuilder, SslProvider}
import io.grpc.{Server, ServerInterceptors}
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode

import scala.util.Try

final class WookieeGrpcServer(
    private val server: Server,
    private val curatorFramework: CuratorFramework,
    private val fiber: Fiber[IO, Unit],
    private val loadQueue: Queue[IO, Int],
    private val host: Host,
    private val discoveryPath: String,
    private val quarantined: Ref[IO, Boolean]
)(
    implicit cs: ContextShift[IO],
    logger: Logger[IO],
    blocker: Blocker
) {

  def shutdown(): IO[Unit] = {
    for {
      _ <- logger.info("Stopping load writing process...")
      _ <- fiber.cancel
      _ <- logger.info("Shutting down gRPC server...")
      _ <- cs.blockOn(blocker)(IO(server.shutdown()))
    } yield ()
  }

  def awaitTermination(): IO[Unit] = {
    cs.blockOn(blocker)(IO(server.awaitTermination()))
  }

  def assignLoad(load: Int): IO[Unit] = {
    loadQueue.enqueue1(load)
  }

  def enterQuarantine(): IO[Unit] = {
    quarantined
      .getAndSet(true)
      .*>(
        WookieeGrpcServer.assignQuarantine(isQuarantined = true, host, discoveryPath, curatorFramework)
      )
  }

  def exitQuarantine(): IO[Unit] = {
    quarantined
      .getAndSet(false)
      .*>(
        WookieeGrpcServer.assignQuarantine(isQuarantined = false, host, discoveryPath, curatorFramework)
      )
  }

}

object WookieeGrpcServer {

  def start(serverSettings: ServerSettings)(
      implicit cs: ContextShift[IO],
      blocker: Blocker,
      logger: Logger[IO],
      timer: Timer[IO]
  ): IO[WookieeGrpcServer] = {
    for {
      host <- serverSettings.host
      server <- cs.blockOn(blocker)(IO {
        val builder = NettyServerBuilder
          .forPort(host.port)
          .channelFactory(() => new NioServerSocketChannel())

        if (serverSettings.sslServerSettings.nonEmpty) {
          builder.sslContext(getSslContextBuilder(serverSettings.sslServerSettings.get))
        }

        builder
          .bossEventLoopGroup(eventLoopGroup(serverSettings.bossExecutionContext, serverSettings.bossThreads))
          .workerEventLoopGroup(eventLoopGroup(serverSettings.workerExecutionContext, serverSettings.workerThreads))
          .executor(scalaToJavaExecutor(serverSettings.applicationExecutionContext))

        serverSettings.serverServiceDefinitions.map { service =>
          val authSettings = serverSettings.authSettings.find(p => p.serviceName == service.getServiceDescriptor.getName)
          if (authSettings.nonEmpty) {
            logger.info("Adding gRPC service [" + authSettings.get.serviceName + "] with authentication enabled")
            builder.addService(ServerInterceptors.intercept(service, BearerTokenAuthenticator(authSettings.get)))
          } else {
            logger.info("Adding gRPC service [" + service.getServiceDescriptor.getName + "]")
            builder.addService(service)
          }
        }

        builder.build()
      })
      _ <- cs.blockOn(blocker)(IO { server.start() })
      _ <- logger.info("gRPC server started...")
      _ <- logger.info("Registering gRPC server in zookeeper...")
      queue <- serverSettings.queue
      quarantined <- serverSettings.quarantined
      // Create an object that stores whether or not the server is quarantined.
      _ <- registerInZookeeper(serverSettings.discoveryPath, serverSettings.curatorFramework, host)
      loadWriteFiber <- streamLoads(
        queue,
        host,
        serverSettings.discoveryPath,
        serverSettings.curatorFramework,
        serverSettings,
        quarantined
      ).start

    } yield new WookieeGrpcServer(
      server,
      serverSettings.curatorFramework,
      loadWriteFiber,
      queue,
      host,
      serverSettings.discoveryPath,
      quarantined
    )
  }

  private def getSslContextBuilder(sslServerSettings: SSLServerSettings)(implicit logger: Logger[IO]): SslContext = {
    val sslClientContextBuilder: SslContextBuilder = if (sslServerSettings.sslPassphrase.nonEmpty)
      SslContextBuilder.forServer(new File(sslServerSettings.sslCertificateChainPath),
        new File(sslServerSettings.sslPrivateKeyPath),
        sslServerSettings.sslPassphrase.get)
    else SslContextBuilder.forServer(
      new File(sslServerSettings.sslCertificateChainPath),
      new File(sslServerSettings.sslPrivateKeyPath))

    val storePath = Paths.get(sslServerSettings.sslCertificateTrustPath).toFile
    if (storePath.exists()) {
      logger.info("gRPC server will require mTLS for client connections.")
      sslClientContextBuilder.trustManager(storePath)
      sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE)
    } else {
      logger.info("gRPC server has TLS enabled.")
    }
    GrpcSslContexts.configure(sslClientContextBuilder, SslProvider.OPENSSL).build()
  }

  private def streamLoads(
      queue: Queue[IO, Int],
      host: Host,
      discoveryPath: String,
      curatorFramework: CuratorFramework,
      serverSettings: ServerSettings,
      quarantined: Ref[IO, Boolean]
  )(implicit timer: Timer[IO], cs: ContextShift[IO], blocker: Blocker, logger: Logger[IO]) = {
    val stream = queue.dequeue
    stream
      .debounce(serverSettings.loadUpdateInterval)
      .evalTap { load: Int =>
        for {
          isQuarantined <- quarantined.get
          _ <- if (isQuarantined) {
            logger
              .info(s"In quarantine. Not updating load...")
          } else {
            assignLoad(load, host, discoveryPath, curatorFramework)
              .*>(
                logger
                  .info(s"Wrote load to zookeeper: load = $load")
              )
          }
        } yield ()
      }
      .compile
      .drain
  }

  private def assignLoad(
      load: Int,
      host: Host,
      discoveryPath: String,
      curatorFramework: CuratorFramework
  )(implicit cs: ContextShift[IO], blocker: Blocker): IO[Unit] = {
    cs.blockOn(blocker) {
      IO {
        val newHost = Host(host.version, host.address, host.port, HostMetadata(load, host.metadata.quarantined))
        curatorFramework
          .setData()
          .forPath(s"$discoveryPath/${host.address}:${host.port}", HostSerde.serialize(newHost))
        ()
      }
    }
  }

  private def assignQuarantine(
      isQuarantined: Boolean,
      host: Host,
      discoveryPath: String,
      curatorFramework: CuratorFramework
  )(implicit cs: ContextShift[IO], blocker: Blocker): IO[Unit] = {
    cs.blockOn(blocker) {
      IO {
        val newHost = Host(host.version, host.address, host.port, HostMetadata(host.metadata.load, isQuarantined))
        curatorFramework
          .setData()
          .forPath(s"$discoveryPath/${host.address}:${host.port}", HostSerde.serialize(newHost))
        ()
      }
    }
  }

  private def registerInZookeeper(
      discoveryPath: String,
      curator: CuratorFramework,
      host: Host
  )(implicit cs: ContextShift[IO], blocker: Blocker): IO[Unit] = {
    cs.blockOn(blocker)(
      IO {
        if (Option(curator.checkExists().forPath(discoveryPath)).isEmpty) {
          curator
            .create()
            .orSetData()
            .creatingParentsIfNeeded()
            .forPath(discoveryPath)
        }

        val path = s"$discoveryPath/${host.address}:${host.port}"

        // Remove any nodes attached to old sessions first (if they exists)
        Try {
          curator
            .delete()
            .forPath(path)
        }

        curator
          .create
          .orSetData()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(path, HostSerde.serialize(host))
        ()
      }
    )
  }
}
