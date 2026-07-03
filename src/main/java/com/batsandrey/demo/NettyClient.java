package com.batsandrey.demo;

import com.batsandrey.demo.decoder.RequestDataDecoder;
import com.batsandrey.demo.encoder.ResponseDataEncoder;
import com.batsandrey.demo.handler.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NettyClient implements CommandLineRunner {

    private static final String HOST = "localhost";
    private static final int PORT = 5002;
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 2000;

    @Override
    public void run(String... args) throws Exception {
        connectWithRetry();
    }

    private void connectWithRetry() throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(
                            new RequestDataDecoder(),
                            new ResponseDataEncoder(),
                            new ClientHandler());
                }
            });

            Channel channel = null;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    channel = b.connect(HOST, PORT).sync().channel();
                    log.info("Connected to {}:{} on attempt {}", HOST, PORT, attempt);
                    break;
                } catch (Exception e) {
                    if (attempt == MAX_RETRIES) {
                        log.error("Failed to connect to {}:{} after {} attempts. Giving up.", HOST, PORT, MAX_RETRIES, e);
                        return;
                    }
                    long delay = BASE_DELAY_MS * attempt;
                    log.warn("Connection to {}:{} failed (attempt {}/{}). Retrying in {}ms...",
                            HOST, PORT, attempt, MAX_RETRIES, delay);
                    Thread.sleep(delay);
                }
            }

            if (channel != null) {
                channel.closeFuture().sync();
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
