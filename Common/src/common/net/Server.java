package common.net;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

public class Server implements APCHost {
	// use little endian, default is big endian.
	public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	
	private ChannelGroup clients = new DefaultChannelGroup("client-channels");
	private Map<Integer, NetSession> sessions = new HashMap<Integer, NetSession>();
	
	private AsynchronousProcedureQueue rpcManager;
	
	private int port;
	private Channel serverChannel;
	private ServerBootstrap server;
	// the max length of the frame.
	// if the frame of the length is too long, then TooLongFrameException will be thrown immediately, which will be caught in SessionHandler.
	private int maxFrameLength = Integer.MAX_VALUE;
	
	public Server() {
		this.rpcManager = new AsynchronousProcedureQueue();
		
		this.server = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),	// for boss 
				Executors.newCachedThreadPool()		// for read/write worker
		));
		
		setOptions(this.server);
		
		// the pipeline is for the children channels.
		server.setPipelineFactory(clientChannelPipelineFactory());
	}
	
	protected void setOptions(Bootstrap s) {
		s.setOption("child.tcpNoDelay", true);
		s.setOption("child.keepAlive", true);
		s.setOption("bufferFactory", HeapChannelBufferFactory.getInstance(BYTE_ORDER));
		s.setOption("child.bufferFactory", HeapChannelBufferFactory.getInstance(BYTE_ORDER));
	}
	
	protected ChannelPipelineFactory clientChannelPipelineFactory() {
		final Server host = this;
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new SessionHandler(host),
					new LengthFieldBasedFrameDecoder(host.maxFrameLength, 0, 4, 0, 4, true),
					new ObjectRPCHandler(host)
				);
			}
		};
	}
	
	public void start(int port, int nExecutors) {
		this.port = port;
		this.rpcManager.initExecutors(nExecutors);
		this.serverChannel = this.server.bind(new InetSocketAddress(this.port));
	}
	
	public void shutdown() {
		this.clients.close().awaitUninterruptibly();
		if (this.serverChannel != null) {
			this.serverChannel.close().awaitUninterruptibly();
		}
		this.server.releaseExternalResources();
		this.rpcManager.shutdown();
	}
	
	@Override
	public void invokeProcedure(Channel channel, APC apc) {
		NetSession session = null;
		if (channel != null) {
			int sessionid = channel.getId();
			session = sessions.get(sessionid);
		}
		rpcManager.invokeProcedure(session, apc);
	}
	
	@Override
	public void registerProcedures(Class<?> clazz) {
		rpcManager.registerProcedures(clazz);
	}
	
	public void addChildChannel(Channel child) {
		if (child != null) {
			NetSession session = new NetSession(child.getId(), child);
			synchronized(sessions) {
				clients.add(child);
				sessions.put(session.getId(), session);
			}
		}
	}
	
	public void removeChildChannel(Channel child) {
		synchronized(sessions) {
			if (child != null && sessions.containsKey(child.getId())) {
				// fire a disconnected apc so that module can handle this
				APC apc = new APC();
				apc.setFunctionName("disconnected");
				apc.setParameters(new Object[1]);
				invokeProcedure(child, apc);
				
				clients.remove(child);
				sessions.remove(child.getId());
			}
		}
	}
	
	public Channel getChildChannel(int channelId) {
		NetSession session = sessions.get(channelId);
		return session != null ? session.getChannel() : null;
	}
	
	public void setMaxFrameLength(int maxFrameLength) {
		if (maxFrameLength <= 4) {
			throw new IllegalStateException("the max length of frame is too short.");
		}
		this.maxFrameLength = maxFrameLength;
	}
}
