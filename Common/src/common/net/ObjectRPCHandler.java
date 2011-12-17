package common.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class ObjectRPCHandler extends SimpleChannelHandler {

	private APCHost host;
		
	public ObjectRPCHandler(APCHost host) {
		this.host = host;
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		ChannelBuffer buffer = (ChannelBuffer)e.getMessage();
		ObjectInputStream input = new ObjectInputStream(new ChannelBufferInputStream(buffer));
		
		try {
			APC rpc = (APC) input.readObject();
			host.invokeProcedure(e.getChannel(), rpc);
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
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(e.getMessage());
        oout.flush();
        oout.close();
        
        ChannelBuffer encoded = bout.buffer();
        encoded.setInt(0, encoded.writerIndex() - 4);
        Channels.write(ctx, e.getFuture(), encoded);
    }
}
