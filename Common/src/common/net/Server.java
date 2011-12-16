package common.net;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

public class Server {
	// use little endian, default is big endian.
	public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	
	private ChannelGroup clients = new DefaultChannelGroup("client-channels");
	private Map<Integer, NetSession> sessions = new HashMap<Integer, NetSession>();
	
	private ExecutorService rpcExecutor;
	private AsynchronousProcedureQueue rpcManager;
	
	private int port;
	private ServerBootstrap serverStrap;
	private Channel serverChannel;
	
	public Server() {
		ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), // for boss 
				Executors.newCachedThreadPool()); // for read/write worker
		
		serverStrap = new ServerBootstrap(factory);
		
		setOptions(serverStrap);
		
		// the pipeline is for the children channels.
		serverStrap.setPipelineFactory(clientChannelPipelineFactory(this));
		
		rpcExecutor = Executors.newFixedThreadPool(1);
		rpcManager = new AsynchronousProcedureQueue(rpcExecutor);
	}
	
	protected void setOptions(Bootstrap s) {
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
		this.serverChannel = this.serverStrap.bind(new InetSocketAddress(this.port));
	}
	
	public void stop() {
		this.clients.close().awaitUninterruptibly();
		this.serverStrap.releaseExternalResources();
		this.serverChannel.close().awaitUninterruptibly();
		
		this.rpcExecutor.shutdown();
		while(!this.rpcExecutor.isShutdown()) {
			try {
				this.rpcExecutor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void invokeProcedure(Channel channel, APC apc) {
		NetSession session = null;
		if (channel != null) {
			int sessionid = channel.getId();
			session = sessions.get(sessionid);
		}
		rpcManager.invokeProcedure(session, apc);
	}
	
	public void registerProcedures(Class<?> clazz) {
		rpcManager.registerProcedures(clazz);
	}
	
	public void addChildChannel(Channel child) {
		NetSession session = new NetSession(child.getId(), child);
		synchronized(sessions) {
			clients.add(child);
			sessions.put(session.getId(), session);
		}
	}
	
	public void removeChildChannel(Channel child) {
		synchronized(sessions) {
			if (sessions.containsKey(child.getId())) {
				// fire a disconnected apc so that module can handle this
				APC apc = new APC();
				apc.setFunctionName("disconnected");
				invokeProcedure(child, apc);
				
				clients.remove(child);
				sessions.remove(child.getId());
			}
		}
	}
	
	public Channel getChildChannel(int channelId) {
		NetSession session = sessions.get(channelId);
		if (session != null) {
			return session.getChannel();
		}
		return null;
	}
}
