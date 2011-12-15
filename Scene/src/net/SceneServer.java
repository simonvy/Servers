package net;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

import common.net.APC;
import common.net.NetSession;
import common.net.Server;
import common.net.SessionHandler;

public class SceneServer extends Server {
	
	private NetSession dbSession;
	private ClientBootstrap database;
	
	public SceneServer() {
		ChannelFactory factory = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		
		database = new ClientBootstrap(factory);
		
		database.setOption("tcpNoDelay", true);
		database.setOption("keepAlive", true);
		database.setOption("bufferFactory", HeapChannelBufferFactory.getInstance(Server.BYTE_ORDER));
		
		database.setPipelineFactory(super.clientChannelPipelineFactory(this));
	}
	
	@Override
	protected ChannelPipelineFactory clientChannelPipelineFactory(final Server host) {
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new SessionHandler(host),
					new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
					new Amf3RPCHandler(host)
				);
			}
		};
	}
	
	public void start(final int port, String db, int dbport) {
		ChannelFuture future = this.database.connect(new InetSocketAddress(db, dbport));
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					databaseChannelConnected(future.getChannel());
					start(port);
					System.out.println("> server started.");
				} else {
					System.out.println("> cannot connect database.");
				}
			}
		});
	}
	
	private void databaseChannelConnected(Channel dbChannel) {
		// when db channel is connected, netty will first fire channel open event,
		// then set the connection future completed.
		// as a result, when this method is called, db channel has already been added into the client pool,
		// Have to remove it.
		super.removeChildChannel(dbChannel);
		this.dbSession = new DbSession(dbChannel.getId(), dbChannel);
		System.out.println("> database is connected.");
	}
	
	@Override
	public void removeChildChannel(Channel child) {
		if (dbSession != null && dbSession.getId() == child.getId()) {
			this.dbSession = null;
			System.out.println("> database is disconnected.");
		} else {
			super.removeChildChannel(child);
		}
	}
	
	public NetSession getDbSession() {
		return dbSession;
	}
	
	@Override
	public void invokeProcedure(Channel channel, APC apc) {
		if (dbSession != null && dbSession.getId() == channel.getId()) {
			// always use the last parameter as the client id
			Object[] params = apc.getParameters();
			if (params != null && params.length > 0) {
				Object first = params[0];
				if (first instanceof Integer) {
					Channel client = super.getChildChannel((Integer)first);
					super.invokeProcedure(client, apc);
				} else {
					throw new IllegalStateException("is not integer from database?");
				}
			} else {
				super.invokeProcedure(null, apc);
			}
		} else {
			super.invokeProcedure(channel, apc);
		}
	}
	
	@Override
	public void stop() {
		super.stop();
		if (dbSession != null) {
			dbSession.close();
		}
		System.out.println("> server stopped.");
	}
}
