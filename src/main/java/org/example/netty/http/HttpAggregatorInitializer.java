package org.example.netty.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;

/**
*@Description 用于聚合Http消息，因为Http的请求和响应可能会由许多部分组成因此，我们需要将其聚合形成完整的消息
*@Author weiyifei
*@return
*/
public class HttpAggregatorInitializer extends ChannelInitializer<Channel> {
    private final boolean client;

    public HttpAggregatorInitializer(boolean client) {
        this.client = client;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if(client){
            //如果客户端则添加HttpClientCodec
            pipeline.addLast("codec",new HttpClientCodec());
            //客户端则添加解压handler
            pipeline.addLast("decompressor",new HttpContentDecompressor());
        }else {
            //如果是服务器则添加HttpServerCodec
            pipeline.addLast("codec",new HttpServerCodec());
            pipeline.addLast("service",new ServiceHandler());

        }
        //将最大消息为512KB的消息的HttpObjectAggregator添加到ChannelPipeline
        pipeline.addLast("aggregator",new HttpObjectAggregator(512*1024));
    }
}
