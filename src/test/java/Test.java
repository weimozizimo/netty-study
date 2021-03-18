import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.TooLongFrameException;
import org.example.netty.junit.AbsIntegerEncoder;
import org.example.netty.junit.FrameChunkDecoder;
import org.junit.Assert;

import static org.junit.Assert.*;

public class Test {
    /**
     * 测试出站消息
     */
    @org.junit.Test
    public void test1(){
        ByteBuf buf = Unpooled.buffer(); //通过Unpooled获取一个非池花的ByteBuf对象
        for (int i = 0; i < 10; i++) {
            buf.writeInt(i*-1);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new AbsIntegerEncoder());

        assertTrue(channel.writeOutbound(buf));
        assertTrue(channel.finish());

        //read bytes
        for (Integer i = 0; i < 10; i++) {
            assertEquals(i,channel.readOutbound());
        }

        assertNull(channel.readOutbound());

    }

    /**
     * 测试异常处理
     */
    @org.junit.Test
    public void test2(){
        ByteBuf buf = Unpooled.buffer();

        //创建一个ByteBuf并向其中添加9个字节
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }

        ByteBuf input = buf.duplicate();

        EmbeddedChannel channel = new EmbeddedChannel(new FrameChunkDecoder(3));

        assertTrue(channel.writeInbound(input.readBytes(2)));

        try{
            channel.writeInbound(input.readBytes(4));
            Assert.fail();
        }catch (TooLongFrameException e){
        }


        assertTrue(channel.writeInbound(input.readBytes(3)));

        assertTrue(channel.finish());

        ByteBuf read = (ByteBuf) channel.readInbound();

        assertEquals(buf.readSlice(2),read);
        read.release();

        read  = (ByteBuf)channel.readInbound();

        assertEquals(buf.skipBytes(4).readSlice(3),read);

        read.release();
        buf.release();

    }


}
