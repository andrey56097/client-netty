package com.batsandrey.demo;

import com.batsandrey.demo.decoder.RequestDataDecoder;
import com.batsandrey.demo.encoder.ResponseDataEncoder;
import com.batsandrey.demo.handler.ClientHandler;
import com.batsandrey.demo.handler.UptimeClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class NettyClient implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        run();
    }
    private static final Bootstrap b = new Bootstrap();

    public static final String HOST = System.getProperty("host", "127.0.0.1");
    public static final int PORT = Integer.parseInt(System.getProperty("port", "5002"));

    static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnectDelay", "5"));
    private static final UptimeClientHandler handler = new UptimeClientHandler();
    private static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("readTimeout", "100"));

    public void run() {
        String host = "localhost";
        int port = 5002;
        EventLoopGroup workerGroup = new NioEventLoopGroup();

            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(host, port);
            b.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new IdleStateHandler(READ_TIMEOUT, 0, 0), (ChannelHandler) handler,
                            new RequestDataDecoder(),
                            new ResponseDataEncoder(),
                            new ClientHandler());
                }
            });

            b.connect();
    }

    public static void connect() {
        b.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.cause() != null) {
                    handler.startTime = -1;
                    handler.println("Failed to connect: " + future.cause());
                }
            }
        });
    }
}
