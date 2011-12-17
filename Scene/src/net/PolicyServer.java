package net;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder;

import common.net.Server;

public class PolicyServer extends Server {
	
	private static final String POLICY_REQUEST = "<policy-file-request/>";
	private static final String POLICY = "<?xml version='1.0'?>" +
				"<cross-domain-policy>" +  
					"<allow-access-from domain=\"*\" to-ports=\"6668\" />" +
				"</cross-domain-policy>";
	
	@Override
	protected ChannelPipelineFactory clientChannelPipelineFactory() {
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new FixedLengthFrameDecoder(22),  // <policy-file-request/>
					new PolicyRequestHandler()
				);
			}
		};
	}
	
	private class PolicyRequestHandler extends SimpleChannelUpstreamHandler {
		
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			ChannelBuffer buffer = (ChannelBuffer)e.getMessage();
			String request = new String(buffer.array());
			
			if (POLICY_REQUEST.equals(request)) {
				writePolicy(e.getChannel());
			}
	    }
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			e.getChannel().close();
		}
		
		private void writePolicy(Channel channel) throws Exception {
			byte[] policy = POLICY.getBytes();
			ChannelBufferOutputStream bout = 
					new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer(
							policy.length, channel.getConfig().getBufferFactory()));
			bout.write(policy);
			ChannelBuffer buffer = bout.buffer();
			ChannelFuture writeFuture = channel.write(buffer);
			writeFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.getChannel().close();
				}
			});
		}
	}
}
