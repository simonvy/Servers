package net;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class PolicyRequestHandler extends SimpleChannelUpstreamHandler {

	private static final String POLICY_REQUEST = "<policy-file-request/>";
	private static final String POLICY = "<?xml version='1.0'?>" +
				"<cross-domain-policy>" +  
					"<allow-access-from domain=\"*\" to-ports=\"6668\" />" +
				"</cross-domain-policy>";
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer buffer = (ChannelBuffer)e.getMessage();
		String request = new String(buffer.array());
		
		if (POLICY_REQUEST.equals(request)) {
			byte[] policy = POLICY.getBytes();
			ChannelBufferOutputStream bout = 
					new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer(
							policy.length, ctx.getChannel().getConfig().getBufferFactory()));
			bout.write(policy);
			buffer = bout.buffer();
			ChannelFuture writeFuture = e.getChannel().write(buffer);
			writeFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.getChannel().close();
				}
			});
		}
    }
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		
	}
}
