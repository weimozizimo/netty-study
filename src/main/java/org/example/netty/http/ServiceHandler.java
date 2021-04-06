package org.example.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * @Description 用于处理业务
 * @Author weiyifei
 * @date 2021/4/3
 */
public class ServiceHandler extends SimpleChannelInboundHandler<HttpObject> {

    private HttpRequest request;

    private boolean readingChunks;

    private final StringBuilder responseContent = new StringBuilder();

    private static final HttpDataFactory factory = new DefaultHttpDataFactory();


    //该解码器用于方便处理POST请求的请求体，但是必须在声明周期结束之后释放该对象
    private HttpPostRequestDecoder decoder;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (decoder != null) {
            //释放POST请求解码器的数据
            decoder.cleanFiles();
            ;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        System.err.println(msg.getClass().getName());
        /**
         * msg的类型
         * {@link io.netty.handler.codec.http.DefaultHttpRequest]
         * {@link io.netty.handler.codec.http.LastHttpContent}
         */
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;
            URI uri = new URI(request.uri());
            System.err.println("request uri==" + uri.getPath());
            if (uri.getPath().equals("/favicon.ico")) {
                return;
            }
            //当时请求"/"路径时，给客户端传输一个HTML页面作为导航页，并结束方法
            if (uri.getPath().equals("/")) {
                writeMenu(ctx);
                return;
            }

            responseContent.setLength(0);
            //htt中以\r\n换行
            responseContent.append("WELCOME TO THE YINGJIN's WEB SERVER\r\n");
            responseContent.append("===================================\r\n");

            responseContent.append("VERSION: " + request.protocolVersion().text() + "\r\n");

            responseContent.append("REQUEST_URI: " + request.uri() + "\r\n\r\n");
            responseContent.append("\r\n\r\n");
            //new getMethod
            for (Map.Entry<String, String> entry : request.headers()) {
                responseContent.append("HEADER:" + entry.getKey() + "=" + entry.getValue() + "\r\n");
            }
            responseContent.append("\r\n\r\n");

            Set<Cookie> cookies;
            String value = request.headers().get(COOKIE);
            if (value == null) {
                /**
                 * Return an empty set
                 */
                cookies = Collections.emptySet();
            } else {
                cookies = ServerCookieDecoder.LAX.decode(value);
            }
            for (Cookie cookie : cookies) {
                responseContent.append("COOKIE：" + cookie.toString() + "\re\n");
            }
            responseContent.append("\r\n\r\n");
            //取出uri后面跟着的参数
            QueryStringDecoder decoderQuery = new QueryStringDecoder(request.uri());
            Map<String, List<String>> uriAttributees = decoderQuery.parameters();
            for (Map.Entry<String, List<String>> attr : uriAttributees.entrySet()) {
                for (String attrVal : attr.getValue()) {
                    responseContent.append("URI:" + attr.getKey() + "=" + attrVal + "\r\n");
                }
            }
            responseContent.append("\r\n\r\n");
            //if GET Method: should not try to create a HttpPostRequestDecoder
            //so stop here
            if (request.method().equals(HttpMethod.GET)) {
                responseContent.append("\r\n\r\nEND OF GET CONTENT\r\n");
                writeResponse(ctx.channel());
                return;
            }
            //判断request请求是否是post请求
            if (request.method().equals(HttpMethod.POST)) {
                System.err.println("=====this is a post request====");
            }
            try {
                /**
                 * 通过HttpDataFactory和request构造解码器
                 */
                decoder = new HttpPostRequestDecoder(factory, request);
            } catch (HttpPostRequestDecoder.ErrorDataDecoderException el) {
                el.printStackTrace();
                responseContent.append(el.getMessage());
                writeResponse(ctx.channel());
                ctx.channel().close();
                return;
            }
            //判断是否使用分块传输协议
            readingChunks = HttpUtil.isTransferEncodingChunked(request);
            responseContent.append("Is Chunked:" + readingChunks + "\r\n");
            responseContent.append("IsMultipart:" + decoder.isMultipart() + "\r\n");
            if (readingChunks) {
                //Chunk version
                responseContent.append("Chunks:");
                readingChunks = true;
            }
        }

        if (decoder != null) {
            if (msg instanceof HttpContent) {
                //New chunk is received
                HttpContent chunk = (HttpContent) msg;
                try {
                    //如果是chunk编码,必须要调用offer方法来编码。否则会抛出异常
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException el) {
                    el.printStackTrace();
                    responseContent.append(el.getMessage());
                    writeResponse(ctx.channel());
                    ctx.channel().close();
                    return;
                }
                responseContent.append("o");
                try {
                    while (decoder.hasNext()) {
                        InterfaceHttpData data = decoder.next();
                        if (data != null) {
                            try {
                                writeHttpData(data);
                            } finally {
                                data.release();
                            }
                        }
                    }
                } catch (HttpPostRequestDecoder.EndOfDataDecoderException e1) {
                    responseContent.append("\r\n\r\nEND OF CONTENT CHUNK BY CHUNK\r\n\r\n");
                }

                //example of reading only if at the end
                if (chunk instanceof LastHttpContent) {
                    writeResponse(ctx.channel());
                    readingChunks = false;
                    reset();
                }
            }
        }
    }


    //写出Http数据
    private void writeHttpData(InterfaceHttpData data) {
        /**
         * HttpDataType有三种类型
         * Attribute, FileUpload, InternalAttribute
         */
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            String value;
            try {
                value = attribute.getValue();
            } catch (IOException e1) {
                e1.printStackTrace();
                responseContent.append("\r\nBODY Attribute: " + attribute.getHttpDataType().name() + ":"
                        + attribute.getName() + " Error while reading value: " + e1.getMessage() + "\r\n");
                return;
            }
            if (value.length() > 100) {
                responseContent.append("\r\nBODY Attribute: " + attribute.getHttpDataType().name() + ":"
                        + attribute.getName() + " data too long\r\n");
            } else {
                responseContent.append("\r\nBODY Attribute: " + attribute.getHttpDataType().name() + ":"
                        + attribute.toString() + "\r\n");
            }
        }
    }

    private void reset() {
        request = null;
        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
    }

    private void writeResponse(Channel channel) {
        //convert the response content to a ChannelBuffer
        ByteBuf buf = null;
        try {
            buf = channel.alloc().buffer().writeBytes(responseContent.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //清空这个StringBuider对象一遍后续新的请求复用
        responseContent.setLength(0);
        //判断连接是否应该关闭
        boolean close = request.headers().contains(CONNECTION, HttpHeaderValues.CLOSE, true) ||
                request.protocolVersion().equals(HttpVersion.HTTP_1_0)//由于HTTP_1.0协议是默认不开启长连接的，所以如果是1.0协议需要关闭连接
                        && !request.headers().contains(CONNECTION, HttpHeaderValues.KEEP_ALIVE, true);

        //建立响应对象
        DefaultHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(CONTENT_TYPE, "text/plain;charset=utf-8");
        if (!close) {
            //这里不需要添加content-length的头信息
            //如果这是最后一个响应,因为在传输完毕这个之后服务器会关闭连接，那么客户端可以很容易知道消息的传输长度
            response.headers().set(CONTENT_LENGTH, buf.readableBytes());
        }

        Set<Cookie> cookies;
        String value = request.headers().get(COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.LAX.decode(value);
        }
        if (!cookies.isEmpty()) {
            for (Cookie cookie : cookies) {
                response.headers().add(SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
            }
        }
        ChannelFuture future = channel.writeAndFlush(response);
        if (close) {
            //一个由netty默认实现的future监听
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }


    private void writeMenu(ChannelHandlerContext ctx) {
        //使用池化的ByteBuf
        ByteBuf buf = ctx.alloc().buffer();
        // print several HTML forms
        // Convert the response content to a ChannelBuffer.
        responseContent.setLength(0);

        // create Pseudo Menu
        responseContent.append("<html>");
        responseContent.append("<head>");
        responseContent.append("<title>Netty Test Form</title>\r\n");
        responseContent.append("</head>\r\n");
        responseContent.append("<body bgcolor=white><style>td{font-size: 12pt;}</style>");

        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr>");
        responseContent.append("<td>");
        responseContent.append("<h1>Netty Test Form</h1>");
        responseContent.append("Choose one FORM");
        responseContent.append("</td>");
        responseContent.append("</tr>");
        responseContent.append("</table>\r\n");

        // GET
        responseContent.append("<CENTER>GET FORM<HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        responseContent.append("<FORM ACTION=\"/from-get\" METHOD=\"GET\">");
        responseContent.append("<input type=hidden name=getform value=\"GET\">");
        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr><td>Fill with value: <br> <input type=text name=\"info\" size=10></td></tr>");
        responseContent.append("<tr><td>Fill with value: <br> <input type=text name=\"info\" size=10></td></tr>");
        responseContent.append("<tr><td>Fill with value: <br> <input type=text name=\"secondinfo\" size=20>");
        responseContent
                .append("<tr><td>Fill with value: <br> <textarea name=\"thirdinfo\" cols=40 rows=10></textarea>");
        responseContent.append("</td></tr>");
        responseContent.append("<tr><td><INPUT TYPE=\"submit\" NAME=\"Send\" VALUE=\"Send\"></INPUT></td>");
        responseContent.append("<td><INPUT TYPE=\"reset\" NAME=\"Clear\" VALUE=\"Clear\" ></INPUT></td></tr>");
        responseContent.append("</table></FORM>\r\n");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");

        // POST
        responseContent.append("<CENTER>POST FORM<HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        responseContent.append("<FORM ACTION=\"/from-post\" METHOD=\"POST\">");
        responseContent.append("<input type=hidden name=getform value=\"POST\">");
        responseContent.append("<table border=\"0\">");
        responseContent.append("<tr><td>Fill with value: <br> <input type=text name=\"info\" size=10></td></tr>");
        responseContent.append("<tr><td>Fill with value: <br> <input type=text name=\"secondinfo\" size=20>");
        responseContent
                .append("<tr><td>Fill with value: <br> <textarea name=\"thirdinfo\" cols=40 rows=10></textarea>");
        responseContent.append("<tr><td>Fill with file (only file name will be transmitted): <br> "
                + "<input type=file name=\"myfile\">");
        responseContent.append("</td></tr>");
        responseContent.append("<tr><td><INPUT TYPE=\"submit\" NAME=\"Send\" VALUE=\"Send\"></INPUT></td>");
        responseContent.append("<td><INPUT TYPE=\"reset\" NAME=\"Clear\" VALUE=\"Clear\" ></INPUT></td></tr>");
        responseContent.append("</table></FORM>\r\n");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        responseContent.append("<CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>");
        responseContent.append("</body>");
        responseContent.append("</html>");

        try {
            buf.writeBytes(responseContent.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, buf.readableBytes());

        // Write the response.
        ctx.channel().writeAndFlush(response);
    }
}
