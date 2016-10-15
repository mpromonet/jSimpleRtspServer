import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.SocketChannel;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.*;

public class RtspServer {
	public static class RtspServerHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {					
			if (msg instanceof DefaultHttpRequest) {
				
				DefaultHttpRequest req = (DefaultHttpRequest) msg;
				
				if (req.method() == RtspMethods.OPTIONS)
				{					
					FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
					response.headers().add(RtspHeaders.Names.PUBLIC, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE");
					final String cseq = req.headers().get("CSEQ");
					if (cseq != null)
					{
						response.headers().add(RtspHeaders.Names.CSEQ, cseq);
					}
					if (!HttpHeaders.isKeepAlive(req)) {
						ctx.write(response).addListener(ChannelFutureListener.CLOSE);
					} else {
						response.headers().set(RtspHeaders.Names.CONNECTION, RtspHeaders.Values.KEEP_ALIVE);
						ctx.write(response);
					}					
				}
				else
				{
					System.err.println("Not managed :" + req.method());					
					FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.NOT_FOUND);
					ctx.write(response).addListener(ChannelFutureListener.CLOSE);
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