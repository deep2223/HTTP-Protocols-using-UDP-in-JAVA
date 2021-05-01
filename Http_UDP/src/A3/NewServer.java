/**
 * 
 */
package A3;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class NewServer {

	
	static final String OkStatusCode= "HTTP/1.1 200 OK";
	static final String fileNotFoundStatusCode= "HTTP/1.1 404 FILE NOT FOUND";
	static final String fileOverwrittenStatusCode= "HTTP/1.1 201 FILE OVER-WRITTEN";
	static final String newFileCreatedStatusCode= "HTTP/1.1 202 NEW FILE CREATED";
	static final String connectionAlive= "Connection: keep-alive";
	static final String server= "Server: httpfs/1.0.0";
	static final String date= "Date: ";
	static final String accessControlAllowOrigin = "Access-Control-Allow-Origin: *";
	static final String accessControlAllowCredentials = "Access-Control-Allow-Credentials: true";
	static final String via ="Via : 1.1 vegur";
	
	static boolean debug=false;
	static String dir = System.getProperty("user.dir");
	static File currentFolder;
	//static List<Long> receivedPackets = new ArrayList<>(); 
	
	//static long sequenceNum=0;
	static int timeout = 3000;
	static int closeCount=0;
	static int port=1234;
	
	
	public static void main(String[] args) throws Exception {
		 	String request;
			ArrayList<String> requestList = new ArrayList<>();
			
			
			System.out.print(">");
			Scanner sc=new Scanner(System.in);
			request = sc.nextLine();
			if(request.isEmpty() || request.length()==0){
				System.out.println("Invalid Command");
				}
			String[] requestArray = request.split(" ");
			requestList = new ArrayList<>();
			for (int i = 0; i < requestArray.length; i++) {
				requestList.add(requestArray[i]);
			}
			
			if(requestList.contains("-v")){
				debug=true;
			}
			
			if(requestList.contains("-p")){
				String portStr = requestList.get(requestList.indexOf("-p")+1).trim();
				port = Integer.valueOf(portStr);
			}
			
			if(requestList.contains("-d")){
				dir = requestList.get(requestList.indexOf("-d")+1).trim();
			}
			
			if(debug)
				System.out.println("Server up on port number : "+port);
			
			currentFolder= new File(dir);
	       
			
	        NewServer server = new NewServer();
	        Runnable task = () -> {
	        try {
				server.listenAndServe(port);
			} catch (Exception e) {
				e.printStackTrace();
			}
	        };
	        Thread thread = new Thread(task);
	        thread.start();
	    }
    
    	private void listenAndServe(int port) throws Exception {
		
		  try (DatagramChannel channel = DatagramChannel.open()) {
	            channel.bind(new InetSocketAddress(port));
	            
	            ByteBuffer buf = ByteBuffer
	                    .allocate(Packet.MAX_LEN)
	                    .order(ByteOrder.BIG_ENDIAN);

	            for (; ; ) {
	                buf.clear();
	                SocketAddress router = channel.receive(buf);
	                if(router!=null){
	                // Parse a packet from the received raw data.
	                buf.flip();
	                Packet packet = Packet.fromBuffer(buf);
	                buf.flip();

	                String requestPayload = new String(packet.getPayload(), UTF_8);
	                // Send the response to the router not the client.
	                // The peer address of the packet is the address of the client already.
	                // We can use toBuilder to copy properties of the current packet.
	                // This demonstrate how to create a new packet from an existing packet.
	              
	                if (requestPayload.equals("Hi")) {
	                	//receivedPackets.add(packet.getSequenceNumber());
	                	System.out.println(requestPayload);
	                	//sequenceNum++;
	                	 Packet resp = packet.toBuilder()
	                			//.setSequenceNumber(sequenceNum)
	 	                        .setPayload(requestPayload.getBytes())
	 	                        .create();
	 	                channel.send(resp.toBuffer(), router);
	 	                System.out.println("Sending Hi");
	 	                
	 	            /*// Try to receive a packet within timeout.
		                channel.configureBlocking(false);
		                Selector selector = Selector.open();
		                channel.register(selector, OP_READ);
		                selector.select(timeout);
		                
		                Set<SelectionKey> keys = selector.selectedKeys();
		                if(keys.isEmpty()){
		                	System.out.println("No request received.\nSending Hi again");
		                	resend(channel, resp, router);
		                }
		                
		               keys.clear();*/
					}
	                else if(requestPayload.contains("httpfs")){
	                //	receivedPackets.add(packet.getSequenceNumber());
	                //	sequenceNum++;
		                String responsePayload = processRequestPayload(requestPayload);
		                
		                Packet resp = packet.toBuilder()
		                	//	.setSequenceNumber(sequenceNum)
		                        .setPayload(responsePayload.getBytes())
		                        .create();
		                channel.send(resp.toBuffer(), router);
		                
		             /*// Try to receive a packet within timeout.
		                channel.configureBlocking(false);
		                Selector selector = Selector.open();
		                channel.register(selector, OP_READ);
		          //    logger.info("Waiting for the response");
		                selector.select(timeout);
		                
		                Set<SelectionKey> keys = selector.selectedKeys();
		                if(keys.isEmpty()){
		                   // logger.error("No response after timeout");
		                	System.out.println("No ack for the response received from client after timeout\nSending response again");
		                	resend(channel, resp, router);
		                }
		                keys.clear();*/
		                }
	                else if(requestPayload.equals("Received")){
			                // Parse a packet from the received raw data.
			             System.out.println(requestPayload);
			       //      receivedPackets.add(packet.getSequenceNumber());
		           //     sequenceNum++;
	                	Packet respClose = packet.toBuilder()
	                		//	.setSequenceNumber(sequenceNum)
	 	                        .setPayload("Close".getBytes())
	 	                        .create();
	 	                channel.send(respClose.toBuffer(), router);
		               	                
		             /*// Try to receive a packet within timeout.
		                channel.configureBlocking(false);
		                Selector selector = Selector.open();
		                channel.register(selector, OP_READ);
		             // logger.info("Waiting for the response");
		                selector.select(timeout);
		                
		                Set<SelectionKey> keys = selector.selectedKeys();
		                keys = selector.selectedKeys();
		                if(keys.isEmpty()){
		                	resend(channel, respClose, router);
		                }
		                keys.clear();*/
		             //  receivedPackets.clear();
		             //  closeCount=0;
	                }
	                else if(requestPayload.equals("Ok")){
	                	 	
		                	System.out.println(requestPayload+" received..!");
			                
		                }
		            }
	            }
		    }
		
	}
    	private String processRequestPayload(String requestPayload) throws Exception {
		
		
		String[] clientRequestArray = requestPayload.split(" ");
 		ArrayList<String> clientRequestList = new ArrayList<>();
 		for (int i = 0; i < clientRequestArray.length; i++) {
 			clientRequestList.add(clientRequestArray[i]);
 		}
 		
 		String url ;
 		String fileData = "";
 		String downloadFileName = "";
 		
 		if(requestPayload.contains("post")){
 			url = clientRequestList.get(2);
 		}
 		else{
 			url =  clientRequestList.get(clientRequestList.size()-1);
 		}
 		String host = new URL(url).getHost();
 		String method=clientRequestList.get(1);
 		DateFormat dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
 		Date date=new Date();
 		String currentDateAndTime = dateFormat.format(date);
 		String responseHeaders = OkStatusCode +"\n"+connectionAlive+"\n"+server+"\n"+date+currentDateAndTime+"\n"+
 				accessControlAllowOrigin+"\n"+accessControlAllowCredentials+"\n"+via+"\n";
         
 		if(debug)
				System.out.println("Processing the httpfs request");
			
			String body = "{\n";
			body = body+"\t\"args\":";
			body = body +"{},\n";
			body = body + "\t\"headers\": {";
			
			if(!method.endsWith("/") && method.contains("get/") && requestPayload.contains("Content-Disposition:attachment")){
				body = body + "\n\t\t\"Content-Disposition\": \"attachment\",";
			}
			else if(!method.endsWith("/") && method.contains("get/") && requestPayload.contains("Content-Disposition:inline")){
				body = body + "\n\t\t\"Content-Disposition\": \"inline\",";
			}
			body = body + "\n\t\t\"Connection\": \"close\",\n";
			body = body + "\t\t\"Host\": \""+host+"\"\n";
			body = body +  "\t},\n";
			
			if(method.equalsIgnoreCase("get/")){
				
				body = body + "\t\"files\": { ";
				ArrayList<String> files = listDirectoryFiles(currentFolder);
				for(int i=0;i<files.size();i++){
					if(i!=files.size()-1){
						body = body + files.get(i)+" , ";
					}
					else{
						body = body + files.get(i)+" },\n";
					}
				}
			}
			
			// if the request is 'GET /fileName'
			else if(!method.endsWith("/") && method.contains("get/")){
				
				//String response="";
				String requestedFileName = method.split("/")[1];
				ArrayList<String> files = listDirectoryFiles(currentFolder);
				
				if(requestPayload.contains("Content-Type")){
					String fileType = clientRequestList.get(clientRequestList.indexOf("-h")+1).split(":")[1]; 
					requestedFileName=requestedFileName+"."+fileType;
				}
				
				if(!files.contains(requestedFileName)){
					responseHeaders=fileNotFoundStatusCode +"\n"+connectionAlive+"\n"+server+"\n"+date+currentDateAndTime+
							"\n"+accessControlAllowOrigin+"\n"+accessControlAllowCredentials+"\n"+via+"\n";
					//out.writeUTF("404");
					}
				else{
					File file = new File(dir+"/"+requestedFileName);
					BufferedReader br = new BufferedReader(new FileReader(file));
					String st;
					while((st = br.readLine())!=null){
						fileData = fileData + st;
					}
					if(requestPayload.contains("Content-Disposition:attachment")){
						//out.writeUTF("203");
						downloadFileName = requestedFileName;
						//out.writeUTF(response);
						//out.writeUTF(requestedFileName);
						}
					else{
					//	out.writeUTF("203");
						body = body + "\t\"data\": \""+fileData+"\",\n";
					}
					
				}
			}
			
			else if(!method.endsWith("/") && method.contains("post/")){
				
				String fileName = method.split("/")[1];
				File file = new File(fileName);
				ArrayList<String> files = listDirectoryFiles(currentFolder);
				if (files.contains(fileName)) {
					//out.writeUTF(fileName+" exists. Do you want to overwrite it. Press 'Y' for yes or 'N' for No.");
					//String ans = in.readUTF().trim();
					//if(ans.equalsIgnoreCase("Y")){
						synchronized (file) {
						file.delete();
						file = new File(dir+"/"+fileName);
						file.createNewFile();
						FileWriter fw = new FileWriter(file);
						fw.write(requestPayload.substring(requestPayload.indexOf("-d")+3));
						fw.close();
						}
						responseHeaders=fileOverwrittenStatusCode +"\n"+connectionAlive+"\n"+server+"\n"+date+currentDateAndTime+
								"\n"+accessControlAllowOrigin+"\n"+accessControlAllowCredentials+"\n"+via+"\n";
				}
				
				else{
					//out.writeUTF(fileName+" does not exist. Do you want to create a new file? Press 'Y' for yes or 'N' for No." );
					//if(in.readUTF().equalsIgnoreCase("Y")){
					file = new File(dir+"/"+fileName);
					synchronized (file) {
					file.createNewFile();
					FileWriter fw = new FileWriter(file);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter pw = new PrintWriter(bw);
					
					pw.print(requestPayload.substring(requestPayload.indexOf("-d")+3));
					pw.flush();
					pw.close();
					}
					responseHeaders=newFileCreatedStatusCode +"\n"+connectionAlive+"\n"+server+"\n"+date+currentDateAndTime+
							"\n"+accessControlAllowOrigin+"\n"+accessControlAllowCredentials+"\n"+via+"\n";
					}
			}
			body = body +  "\t\"origin\": \""+InetAddress.getLocalHost().getHostAddress()+"\",\n"; 
			body = body +  "\t\"url\": \""+url+"\"\n";
			body = body +  	"}";
			
			if(debug)
				System.out.println("Sending the response");
			String responsePayload = responseHeaders + body;
			if(requestPayload.contains("Content-Disposition:attachment")){
				responsePayload = responsePayload + "|" + downloadFileName + "|" + fileData;
			}
			return responsePayload;
		}
 		
	static private ArrayList<String> listDirectoryFiles(File currentFolder){
		ArrayList<String> files = new ArrayList<>();
		for(File file : currentFolder.listFiles()){
			if(!file.isDirectory()){
				files.add(file.getName());
			}
		}
		return files;
	}
}
