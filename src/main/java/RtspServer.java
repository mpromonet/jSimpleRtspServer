
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.SocketChannel;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;

public class RtspServer {
	public static class RtspServerHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

		private void sendAnswer(ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse rep)
		{
			final String cseq = req.headers().get(RtspHeaderNames.CSEQ);
			if (cseq != null)
			{
				rep.headers().add(RtspHeaderNames.CSEQ, cseq);
			}
            final String session = req.headers().get(RtspHeaderNames.SESSION);
            if (session != null)
            {
                rep.headers().add(RtspHeaderNames.SESSION, session);
            }
			if (!HttpHeaders.isKeepAlive(req)) {
				ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
			} else {
				rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
				ctx.write(rep);
			}					
		}
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {					
			if (msg instanceof DefaultHttpRequest) {
				
				DefaultHttpRequest req = (DefaultHttpRequest) msg;
				
				FullHttpResponse rep = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);
				if (req.method() == RtspMethods.OPTIONS)
				{					
					rep.setStatus(RtspResponseStatuses.OK);
					rep.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN");
					sendAnswer(ctx, req, rep);
				}
				else if (req.method() == RtspMethods.DESCRIBE)
				{

                    ByteBuf buf = Unpooled.copiedBuffer("c=IN IP4 10.5.110.117\r\nm=video 5004 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n", CharsetUtil.UTF_8);
                    rep.setStatus(RtspResponseStatuses.OK);
                    rep.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
                    rep.headers().add(RtspHeaderNames.CONTENT_LENGTH, buf.writerIndex());
                    rep.content().writeBytes(buf);
					sendAnswer(ctx, req, rep);
				}
                else if (req.method() == RtspMethods.SETUP)
                { 
                    rep.setStatus(RtspResponseStatuses.OK);
                    String session = String.format("%08x",(int)(Math.random()*65536));
                    rep.headers().add(RtspHeaderNames.SESSION, session);
                    rep.headers().add(RtspHeaderNames.TRANSPORT,"RTP/AVP;unicast;client_port=5004-5005");
                    sendAnswer(ctx, req, rep);
                }
                else if (req.method() == RtspMethods.PLAY)
                {
                    rep.setStatus(RtspResponseStatuses.OK);
                    sendAnswer(ctx, req, rep);
                }
				else
				{
					System.err.println("Not managed :" + req.method());					
					ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception	{    
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);			
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) {
					ChannelPipeline p = ch.pipeline();
					p.addLast(new RtspDecoder(), new RtspEncoder());
					p.addLast(new RtspServerHandler());
				}
			});

			Channel ch = b.bind(8554).sync().channel();
			System.err.println("Connect to rtsp://127.0.0.1:8554");
			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}		
	}
}