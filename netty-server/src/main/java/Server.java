
import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@Sharable
public class Server {
	public void bind(int port) throws Exception {
		//配置服务端的NIO线程组
		//实际上EventLoopGroup就是Reactor线程组
		//两个Reactor一个用于服务端接收客户端的连接，另一个用于进行SocketChannel的网络读写
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try{
			/**
			 * 由于我们使用在 NIO 传输，我们
				已指定 NioEventLoopGroup接受和处理新连接，指定 NioServerSocketChannel
				为信道类型。在此之后，我们设置本地地址是 InetSocketAddress 与所选择的端口（6）如。
				服务器将绑定到此地址来监听新的连接请求。
			 */
			//ServerBootstrap对象是Netty用于启动NIO服务端的辅助启动类，目的是降低服务端开发的复杂度
			ServerBootstrap b = new ServerBootstrap();
			//Set the EventLoopGroup for the parent (acceptor) and the child (client). 
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(port))
				//配置NioServerSocketChannel的TCP参数
				.option(ChannelOption.SO_BACKLOG, 1024)
				//绑定I/O事件的处理类ChildChannelHandler,作用类似于Reactor模式中的Handler类
				//主要用于处理网络I/O事件，例如记录日志，对消息进行编解码等
				.childHandler(new ChannelInitializer<SocketChannel>(){
				//添加TimeServerHandler到Channel的ChannelPipeline
					//通过TimeServerHandler给每一个新来的Channel初始化
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ServerHandler());
					}
				});
			//绑定监听端口，调用sync同步阻塞方法等待绑定操作完成，完成后返回ChannelFuture类似于JDK中Future
			ChannelFuture f = b.bind(port).sync();
			//使用sync方法进行阻塞，等待服务端链路关闭之后Main函数才退出
			f.channel().closeFuture().sync();
		}finally {
			//优雅退出，释放线程池资源
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
		
		
	}
	
	
	private class ServerHandler extends ChannelInboundHandlerAdapter {
		
		//每个信息入站都会调用
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf buf = (ByteBuf) msg;
//			byte[] req = new byte[buf.readableBytes()];
//			buf.readBytes(req);
//			String body = new String(req, "UTF-8");
			System.out.println("The time server receive order :" + buf.toString());
			
			ctx.write(buf);
		}
		
		//通知处理器最后的channelread()是当前批处理中的最后一条消息时调用
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}
		
		//读操作时捕获到异常时调用
		@Override
		public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
			ctx.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		int port = 8080;
		if(args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			}catch (NumberFormatException e) {
				//采用默认值
			}
		}
		new Server().bind(port);
	}
}


