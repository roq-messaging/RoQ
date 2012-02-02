package org.roqmessaging.utils;

import org.zeromq.ZMQ;

public class TimestampServer {
	private ZMQ.Context context;
	private ZMQ.Socket socket;
	
	public TimestampServer(){
		context = ZMQ.context(1);
		socket=context.socket(ZMQ.REP);
		socket.bind("tcp://*:5900");
		
		while(true){
			byte[] request;
			request=socket.recv(0);
			byte[] tstmp = (Long.toString(System.currentTimeMillis())+"0").getBytes();
			socket.send(tstmp, 0);
			//System.out.println("Sent 1 timestamp");
		}
	}
	
	public static void main(String[] args){
		TimestampServer server = new TimestampServer();
	}

}
