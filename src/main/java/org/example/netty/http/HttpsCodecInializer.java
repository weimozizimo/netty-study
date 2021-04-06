package org.example.netty.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * 用于启用https
 */
public class HttpsCodecInializer  extends ChannelInitializer<Channel> {

    private final SSLContext context;

    private final boolean isClient;

    public HttpsCodecInializer(SSLContext context, boolean isClient) {
        this.context = context;
        this.isClient = isClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //将SSLHandler添加到ChannelPipeline中以使用Https
        SSLEngine engine = context.createSSLEngine();
        pipeline.addLast("ssl",new SslHandler(engine));
    }
}
