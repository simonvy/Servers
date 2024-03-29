package net;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

import common.net.APC;
import common.net.Server;
import common.net.SessionHandler;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import flex.messaging.io.amf.Amf3Output;

public class SceneServer extends Server {
	
	private int maxFrameLength = Integer.MAX_VALUE;
	
	@Override
	protected ChannelPipelineFactory clientChannelPipelineFactory() {
		final SceneServer host = this;
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new SessionHandler(host),
					new LengthFieldBasedFrameDecoder(host.maxFrameLength, 0, 4, 0, 4, true),
					new Amf3RPCHandler()
				);
			}
		};
	}
	
	public void setMaxFrameLength(int maxFrameLength) {
		if (maxFrameLength <= 4) {
			throw new IllegalStateException("the max length of frame is too short.");
		}
		this.maxFrameLength = maxFrameLength;
	}
	
	private class Amf3RPCHandler extends SimpleChannelHandler {
		
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			ChannelBuffer buffer = (ChannelBuffer)e.getMessage();
			Amf3Input input = new Amf3Input(new SerializationContext());		
			input.setInputStream(new ChannelBufferInputStream(buffer));
			try {
				APC rpc = (APC) input.readObject();
				invokeProcedure(e.getChannel(), rpc);
			} catch(Exception exc) {
				System.err.println(exc.getMessage());
				e.getChannel().close();
			}
	    }
		
		@Override
		public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			ChannelBufferOutputStream bout = 
					new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer(
							4 * 1024, ctx.getChannel().getConfig().getBufferFactory()));
			bout.write(new byte[4]); // reserved for the length
			Amf3Output out = new Amf3Output(new SerializationContext());
			out.setOutputStream(bout);
			out.writeObject(e.getMessage());
			out.flush();
			out.close();
			
			ChannelBuffer encoded = bout.buffer();
	        encoded.setInt(0, encoded.writerIndex() - 4);
	        Channels.write(ctx, e.getFuture(), encoded);
	    }
	}
}
