package net;

import org.jboss.netty.channel.Channel;

import common.net.NetSession;

public class DbSession extends NetSession {

	public DbSession(int sessionid, Channel channel) {
		super(sessionid, channel);
	}
	
	public void call(String funcName, Object...params) {
		Object[] nparams = new Object[params.length + 1];
		nparams[0] = 0;
		System.arraycopy(params, 0, nparams, 1, params.length);
		super.call(funcName, nparams);
	}
}
