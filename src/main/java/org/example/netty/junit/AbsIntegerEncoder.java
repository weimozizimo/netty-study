package org.example.netty.junit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class AbsIntegerEncoder extends MessageToMessageEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List out) throws Exception {
         while(in.readableBytes() >=4){ // 检查是否有足够的字节来进行编码
             int value = Math.abs(in.readInt()); //从输入ByteBuf读取下一个整数，并计算绝对值
             out.add(value);//将绝对值写入到编码消息的list中

         }
    }
}
