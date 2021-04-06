package org.example.netty.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;

public class HttpServer {
    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        //使用默认的ssl上下文
        SSLContext sslContext = SSLContext.
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpPipelineInitializer(sslContext,false));


        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("绑定成功");
                } else {
                    System.out.println("绑定失败");
                    future.cause().printStackTrace();
                }
            }
        });
        future.sync();

    }
}
