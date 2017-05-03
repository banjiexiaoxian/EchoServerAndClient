

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

@Sharable 
class ClientHandler extends SimpleChannelInboundHandler<ByteBuf>{

	@Override
	public void channelActive(ChannelHandlerContext ctx){
		ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",CharsetUtil.UTF_8));
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		System.out.println("Client received: "+in.toString(CharsetUtil.UTF_8));	
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx,
					Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	

}

public class Client {
	private final String host;
	private final int port;
	
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void start() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try{
			Bootstrap b = new Bootstrap();
			b.group(group)
				.channel(NioSocketChannel.class)
				.remoteAddress(new InetSocketAddress(host,port))
				.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) 
							throws Exception {
					ch.pipeline().addLast(new ClientHandler());
				}
			});
			ChannelFuture f = b.connect().sync();
			
			f.channel().closeFuture().sync();
		}finally {
			group.shutdownGracefully().sync();
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2){
			System.err.println(
					"Usage :" + Client.class.getSimpleName()+
					"<host><port>"
					);
			return;
		}
		
		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		
		new Client(host,port).start();
	}

}
