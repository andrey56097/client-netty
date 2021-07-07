package com.batsandrey.demo.handler;

import com.batsandrey.demo.NettyClient;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
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
            println("Disconnecting due to no inbound traffic" + " remote address" + ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        println("Disconnected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        println("Sleeping for: " + 3 + 's' + " remote address " + ctx.channel().remoteAddress());

        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                NettyClient.reconnect(ctx.channel().remoteAddress());
            }
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void println(String msg) {
        if (startTime < 0) {
            log.error("[SERVER IS DOWN] {}", msg);
        } else {
            log.info("[UPTIME: {}s] {}", (System.currentTimeMillis() - startTime) / 1000, msg);
        }
    }
}
