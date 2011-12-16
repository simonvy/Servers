import java.sql.SQLException;

import module.ChatModule;
import net.SceneServer;

import common.Context;

public class Main {

	public static void main(String[] args) throws SQLException {
		
		SceneServer server = Context.instance().register(SceneServer.class);
		
		server.registerProcedures(ChatModule.class);
		
		try {
			server.start(6668, 843, "localhost", 6669);
			
			synchronized(server) {
				server.wait();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		} finally {
			server.stop();
		}
	}
}
