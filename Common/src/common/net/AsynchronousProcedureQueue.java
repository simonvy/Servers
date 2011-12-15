package common.net;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public final class AsynchronousProcedureQueue {
	
	private Map<String, Pair> knownProcedures = new HashMap<String, Pair>();
	private ExecutorService executor;
	
	public AsynchronousProcedureQueue(ExecutorService executor) {
		this.executor = executor;
	}
	
	public void registerProcedures(Class<?> clazz) {
		Object host = null;
		for (Method method : clazz.getMethods()) {
			Procedure rc = method.getAnnotation(Procedure.class);
			if (rc == null) {
				continue;
			}
			
			String funcName = method.getName();
			if (rc.name() != null && rc.name().length() > 0) {
				funcName = rc.name();
			}
			// if method of the same name is already registered, throw an exception
			if (knownProcedures.containsKey(funcName)) {
				throw new IllegalStateException("procedure [" + funcName + "] is already registered.");
			}
			
			if (host == null) {
				try {
					// constructor with no params is used to instance the host.
					host = clazz.newInstance();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			
			Pair p = new Pair();
			p.method = method;
			p.host = host;
			knownProcedures.put(funcName, p);
		}
	}
	
	public void invokeProcedure(NetSession session, APC apc) {
		ProcedureTask task = new ProcedureTask();
		task.apc = apc;
		task.session = session;
		
		this.executor.execute(task);
	}
	
	private void invokeRPC(NetSession client, APC apc) throws Exception {
		String funcName = apc.getFunctionName();
		Pair p = this.knownProcedures.get(funcName);
		Class<?>[] paramTypes = p.method.getParameterTypes();
		
		if (paramTypes.length == 0) {
			p.method.invoke(p.host);
		} else if (paramTypes.length == 1) {
			p.method.invoke(p.host, client);
		} else {
			Object[] params = apc.getParameters();
			
			if (params == null || params.length != paramTypes.length) {
				throw new IllegalArgumentException("#params!=" + (paramTypes.length));
			}
			params[0] = client;
			p.method.invoke(p.host, params);
		}
	}
	
	private class ProcedureTask implements Runnable {
		private APC apc;
		private NetSession session;
		
		@Override
		public void run() {
			if (validate()) {
				try {
					invokeRPC(session, this.apc);
				} catch(Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		private boolean validate() {
			String funcName = this.apc.getFunctionName();
			if (funcName == null || funcName.length() == 0) {
				System.err.println("> procedure name is empty.");
				return false;
			}
			
			if (!knownProcedures.containsKey(funcName)) {
				System.err.println("> procedure " + funcName + " is not registered.");
				return false;
			}
			
			return true;
		}
	} 
	
	private class Pair {
		Object host;
		Method method;
	}
}
