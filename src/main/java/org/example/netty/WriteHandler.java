package org.example.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class WriteHandler extends ChannelHandlerAdapter {

    private ChannelHandlerContext ctx;

    /**
     * 将当前conext引用缓存起来，以供之后使用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    /**\
     * 使用之前存储的conext对象的引用来发送消息
     * @param msg
     */
    public void send(String msg){
        ctx.writeAndFlush(msg);
    }


}
