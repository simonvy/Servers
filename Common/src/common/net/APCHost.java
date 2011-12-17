package common.net;

import org.jboss.netty.channel.Channel;

public interface APCHost {

	void registerProcedures(Class<?> clazz);
	void invokeProcedure(Channel channel, APC apc);
	
}
