import java.sql.SQLException;

import module.ChatModule;
import net.DatabaseClient;
import net.PolicyServer;
import net.SceneServer;

import common.Context;

public class Main {

	public static void main(String[] args) throws SQLException {
		
		SceneServer server = Context.instance().register(SceneServer.class);
		PolicyServer policy = Context.instance().register(PolicyServer.class);
		DatabaseClient database = Context.instance().register(DatabaseClient.class);
		
		server.registerProcedures(ChatModule.class);
		
		try {
			database.start("localhost", 6669);
			server.start(6668, 1);
			policy.start(8430, 0);
			
			System.out.println("> Server started.");
			
			synchronized(server) {
				server.wait();
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			policy.shutdown();
			server.shutdown();
			database.shutdown();
			System.out.println("> Server stopped.");
		}
	}
}
