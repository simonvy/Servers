package net;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class DbSessionHandler extends SimpleChannelHandler {

	private SceneServer server;

	public DbSessionHandler(SceneServer server) {
		this.server = server;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		server.setDbChannel(e.getChannel());
		ctx.sendUpstream(e);
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		server.removeDbChannel(e.getChannel());
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
			// which might be caused by the abnormal disconnect of client
			if (e.getChannel().isConnected()) {
				e.getChannel().close();
			}
		} else {
			cause.printStackTrace();
		}
	}
	
}
