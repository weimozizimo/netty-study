package org.example.netty.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
*@Description 用于对Http请求响应编解码channelHandler
*@Author weiyifei
*@date 2021/4/3
*/
public class HttpPipelineInitializer extends ChannelInitializer<Channel> {

    private final SSLContext context;

    private  final boolean client;

    public HttpPipelineInitializer(SSLContext context, boolean client) {
        this.context = context;
        this.client = client;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //将SSLHandler添加到ChannelPipeline中以使用Https
        SSLEngine engine = context.createSSLEngine();
        pipeline.addFirst("ssl",new SslHandler(engine));
        if(client){

            //如果是客户端则需要对服务器响应解码与自己发送请求的编码
            pipeline.addLast("decoder1",new HttpResponseDecoder());
            pipeline.addLast("encoder1",new HttpRequestEncoder());
            //如果客户端则添加HttpClientCodec
            pipeline.addLast("codec",new HttpClientCodec());
        }else {
            //如果是服务器，那么需要对客户端请求解码与自己发送响应的编码
            pipeline.addLast("decoder1",new HttpRequestDecoder());
            pipeline.addLast("encoder1",new HttpResponseEncoder());
            //如果是服务器则添加HttpServerCodec
            pipeline.addLast("codec",new HttpServerCodec());
            pipeline.addLast("service",new ServiceHandler());
        }
    }
}
