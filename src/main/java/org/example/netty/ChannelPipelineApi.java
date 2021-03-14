package org.example.netty;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;

public class ChannelPipelineApi {
    public static void main(String[] args) {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addFirst("handler1",new DiscardOutBoundHandler());
    }
}
