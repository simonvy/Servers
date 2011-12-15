package common.net;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

public class Server {
	// use little endian, default is big endian.
	public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	
	protected ChannelGroup clients = new DefaultChannelGroup("client-channels");
	protected Map<Integer, NetSession> sessions = new HashMap<Integer, NetSession>();
	
	protected ExecutorService rpcExecutor;
	protected AsynchronousProcedureQueue rpcManager;
	
	private int port;
	private ServerBootstrap server;
	
	public Server() {
		ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), // for boss 
				Executors.newCachedThreadPool()); // for read/write worker
		
		server = new ServerBootstrap(factory);
		
		setOptions(server);
		
		// the pipeline is for the children channels.
		server.setPipelineFactory(clientChannelPipelineFactory(this));
		
		rpcExecutor = Executors.newFixedThreadPool(1);
		rpcManager = new AsynchronousProcedureQueue(rpcExecutor);
	}
	
	protected void setOptions(ServerBootstrap s) {
		s.setOption("child.tcpNoDelay", true);
		s.setOption("child.keepAlive", true);
		s.setOption("bufferFactory", HeapChannelBufferFactory.getInstance(BYTE_ORDER));
		s.setOption("child.bufferFactory", HeapChannelBufferFactory.getInstance(BYTE_ORDER));
	}
	
	protected ChannelPipelineFactory clientChannelPipelineFactory(final Server host) {
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new SessionHandler(host),
					new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
					new ObjectRPCHandler(host)
				);
			}
		};
	}
	
	public void start(int port) {
		this.port = port;
		this.server.bind(new InetSocketAddress(this.port));
	}
	
	public void stop() {
		ChannelGroupFuture closeFuture = this.clients.close();
		closeFuture.addListener(new ChannelGroupFutureListener() {
			@Override
			public void operationComplete(ChannelGroupFuture future) throws Exception {
				server.releaseExternalResources();
			}
		});
		closeFuture.awaitUninterruptibly();
		
		rpcExecutor.shutdown();
		while(!rpcExecutor.isShutdown()) {
			try {
				rpcExecutor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void invokeProcedure(Channel channel, APC apc) {
		int sessionid = channel.getId();
		NetSession session = sessions.get(sessionid);
		if (session != null) {
			rpcManager.invokeProcedure(session, apc);
		}
	}
	
	public void registerProcedures(Class<?> clazz) {
		rpcManager.registerProcedures(clazz);
	}
	
	public NetSession addChildChannel(Channel child) {
		NetSession session = new NetSession(child.getId(), child);
		synchronized(sessions) {
			clients.add(child);
			sessions.put(child.getId(), session);
		}
		return session;
	}
	
	public void removeChildChannel(Channel child) {
		synchronized(sessions) {
			sessions.remove(child.getId());
			clients.remove(child);
		}
	}
}
