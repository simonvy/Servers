package net;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

import common.Context;
import common.net.APC;
import common.net.APCHost;
import common.net.NetSession;
import common.net.ObjectRPCHandler;
import common.net.Server;

public class DatabaseClient implements APCHost {
	
	private Channel clientChannel;
	private ClientBootstrap dbClient;
	
	public DatabaseClient() {
		
		ExecutorService defaultExecutors = Executors.newCachedThreadPool();
		
		dbClient = new ClientBootstrap(new NioClientSocketChannelFactory(
				defaultExecutors, defaultExecutors
		));
		
		setOptions(dbClient);
		
		dbClient.setPipeline(Channels.pipeline(
			new ExceptionHandler(),
			new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
			new ObjectRPCHandler(this)
		));
	}
	
	private void setOptions(Bootstrap s) {
		s.setOption("tcpNoDelay", true);
		s.setOption("keepAlive", true);
		s.setOption("bufferFactory", HeapChannelBufferFactory.getInstance(Server.BYTE_ORDER));
	}
	
	public void start(String db, int port) {
		ChannelFuture future = this.dbClient.connect(new InetSocketAddress(db, port));
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					clientChannel = future.getChannel();
					System.out.println("> database is connected.");
				} else {
					System.out.println("> cannot connect database.");
				}
			}
		});
	}
	
	public void shutdown() {
		if (clientChannel != null) {
			clientChannel.close().awaitUninterruptibly();
		}
		dbClient.releaseExternalResources();
		System.out.println("> database is disconnected.");
	}

	@Override
	public void invokeProcedure(Channel channel, APC apc) {
		SceneServer server = Context.instance().get(SceneServer.class);
		if (server != null) {
			// always use the first parameter as the client id
			Object[] params = apc.getParameters();
			if (params != null && params.length > 0) {
				Object first = params[0];
				if (first instanceof Integer) {
					Channel client = server.getChildChannel((Integer)first);
					server.invokeProcedure(client, apc);
					return;
				}
			}
			server.invokeProcedure(null, apc);
		}
	}
	
	@Override
	public void registerProcedures(Class<?> clazz) {
		SceneServer server = Context.instance().get(SceneServer.class);
		server.registerProcedures(clazz);
	}
	
	public NetSession getDbSession() {
		return new DbSession(clientChannel.getId(), clientChannel);
	}
	
	private class ExceptionHandler extends SimpleChannelHandler {
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			e.getCause().printStackTrace(System.err);
		}
	}
}
