package com.batsandrey.demo.handler;

import com.batsandrey.demo.NettyClient;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class UptimeClientHandler extends SimpleChannelInboundHandler<Object> {

    public long startTime = -1;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
        println("Connected to: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Discard received data
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.READER_IDLE) {
            // The connection was OK but there was no traffic for last period.
            println("Disconnecting due to no inbound traffic");
            ctx.close();
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        println("Disconnected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        println("Sleeping for: " + 6 + 's');

        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                println("Reconnecting to: " + NettyClient.HOST + ':' + NettyClient.PORT);
                NettyClient.connect();
            }
        }, 6, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void println(String msg) {
        if (startTime < 0) {
            System.err.format("[SERVER IS DOWN] %s%n", msg);
        } else {
            System.err.format("[UPTIME: %5ds] %s%n", (System.currentTimeMillis() - startTime) / 1000, msg);
        }
    }
}
