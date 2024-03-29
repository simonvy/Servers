package common.net;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

public class SessionHandler extends SimpleChannelHandler {
	
	private Server server;

	public SessionHandler(Server server) {
		this.server = server;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		server.addChildChannel(e.getChannel());
		ctx.sendUpstream(e);
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		server.removeChildChannel(e.getChannel());
		ctx.sendUpstream(e);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Throwable cause = e.getCause();
		if (cause instanceof ClosedChannelException) {
			// this channel is already closed and still write data into it would throw this
			// do nothing since channelClosed handler is already handled this.
		} else if (cause instanceof IOException) {
			// often happened when read throws IOException
			// which might be caused by the abnormal disconnect of client;
			// or by the ObjectRPCHandler readObject.
			cause.printStackTrace(System.err);
		} else if (cause instanceof TooLongFrameException) {
			// the length of the frame is too long.
			System.err.println(cause.getMessage());
		} else {
			cause.printStackTrace(System.err);
		}
		if (e.getChannel().isConnected()) {
			e.getChannel().close();
		}
	}
}
