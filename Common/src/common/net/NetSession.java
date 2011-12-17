package common.net;

import org.jboss.netty.channel.Channel;

public class NetSession {

	private int sessionid;
	private Channel channel;
	public Object attachment;
	
	public NetSession(int sessionid, Channel channel) {
		this.channel = channel;
		this.sessionid = sessionid;
	}
	
	public void call(String funcName, Object...params) {
		APC apc = new APC();
		
		apc.setFunctionName(funcName);
		apc.setParameters(params);
		
		if (channel.isWritable()) {
			channel.write(apc);
		}
	}
	
	public int getId() {
		return this.sessionid;
	}
	
	public Channel getChannel() {
		return this.channel;
	}
}
