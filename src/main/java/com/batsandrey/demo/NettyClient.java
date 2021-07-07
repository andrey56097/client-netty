package com.batsandrey.demo;

import com.batsandrey.demo.decoder.RequestDataDecoder;
import com.batsandrey.demo.encoder.ResponseDataEncoder;
import com.batsandrey.demo.handler.UptimeClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class NettyClient implements CommandLineRunner {

    public static List<Bootstrap> bootstrapList = new ArrayList<>();
    private static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("readTimeout", "100"));

    public static Map<String, Integer> hosts = new HashMap<>();

    static {
        hosts.put("127.0.0.2", 5003);
        hosts.put("127.0.0.3", 5004);
        hosts.put("127.0.0.4", 5005);
        hosts.put("127.0.0.5", 5006);
        hosts.put("127.0.0.6", 5007);
    }

    @Override
    public void run(String... args) throws Exception {
        run();
    }

    public void run() {

        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new IdleStateHandler(READ_TIMEOUT, 0, 0), new UptimeClientHandler(),
                        new RequestDataDecoder(),
                        new ResponseDataEncoder());
            }
        });

        ChannelFutureListener listener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // connection complete start to read first data
//                    System.out.println("connection established + channel: " + future.channel());
                    log.info("[Connection established] + channel: {}, ip - {}", future.channel(), future.channel().remoteAddress());
                } else {
                    // Close the connection if the connection attempt has failed.
//                    System.out.println("connection failed " + future.channel());
                    log.error("[Connection failed] + channel: {}, ip - {}", future.channel(), future.channel().remoteAddress());
                }
            }
        };

        for (Map.Entry<String, Integer> entry : hosts.entrySet()) {
            b = b.clone();
            b.remoteAddress(entry.getKey(), entry.getValue());
            b.connect().addListener(listener);
            bootstrapList.add(b);
        }

    }

    public static void reconnect(SocketAddress socketAddress) {

         String uri = getURI(socketAddress);

        Bootstrap bootstrap = null;
        for (Bootstrap b: bootstrapList) {
            String s = getURI(b.config().remoteAddress());
            if (s.equals(uri)) {
                bootstrap = b;
            }
        }

        if (bootstrap != null) {
            bootstrap.connect().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.cause() != null) {
                        log.error("Failed to reconnect: {} channel {}", future.cause(),  future.channel());
                    }
                }
            });
        }
    }

    public static String getURI(SocketAddress socketAddress) {
        return ((InetSocketAddress) socketAddress).getHostName() + ":" + ((InetSocketAddress) socketAddress).getPort();
    }
}
