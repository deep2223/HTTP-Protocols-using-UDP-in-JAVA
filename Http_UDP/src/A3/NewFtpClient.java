package A3;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class NewFtpClient {

	static long sequenceNum=0;
	static List<Long> receivedPackets = new ArrayList<>();
	static int timeout = 3000;
	static int ackCount=0;
	public static void main(String[] args) throws Exception {

		 // Router address
        String routerHost = "localhost";
        int routerPort = 3000;

        ArrayList<String> requestList = new ArrayList<>();
    	File file = new File("Downloads");
    	file.mkdir();
    	while(true){
    		String request="";
    		// httpfs get/ http://localhost:1234/get?
    		System.out.print(">");
    		receivedPackets.clear();
    		sequenceNum=0;
    		ackCount=0;
    		Scanner sc=new Scanner(System.in);
    		request = sc.nextLine();
    		
    		if(request.isEmpty() || request.length()==0){
    			System.out.println("Invalid Command");
    			continue;
    		}
    		String[] requestArray = request.split(" ");
    		requestList = new ArrayList<>();
    		for (int i = 0; i < requestArray.length; i++) {
    			requestList.add(requestArray[i]);
    		}
    		String url ;
    		if(request.contains("post")){
    			url = requestList.get(2);
    		}
    		else{
    			url =  requestList.get(requestList.size()-1);
    		}
    		// getting host from url
    		String serverHost = new URL(url).getHost();
    		int serverPort = new URL(url).getPort();
    		
    	SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);
       
        establishConnection(routerAddress, serverAddress);
        runClient(routerAddress, serverAddress, request);
        
    	}
	}
    	private static int establishConnection(SocketAddress routerAddress, InetSocketAddress serverAddress) throws Exception {
		
		
		try(DatagramChannel channel = DatagramChannel.open()){
	        String msg = "Hi";
	        sequenceNum++;
            Packet p = new Packet.Builder()
                    .setType(0)			// SYN
                    .setSequenceNumber(sequenceNum)
                    .setPortNumber(serverAddress.getPort())
                    .setPeerAddress(serverAddress.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddress);
            System.out.println("Sending Hi");

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
     
            selector.select(timeout);
            
            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
            	System.out.println("No response after timeout\nSending again");
            	resend(channel,p,routerAddress);
            }
           
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            System.out.println(payload+" received..!");
            receivedPackets.add(resp.getSequenceNumber());
        	//sequenceNum++;
            keys.clear();
          
            return 0;   
          }
	}
    private static void resend(DatagramChannel channel, Packet p, SocketAddress routerAddress) throws IOException {
		
		channel.send(p.toBuffer(), routerAddress);
        System.out.println(new String(p.getPayload()));
        if(new String(p.getPayload()).equals("Received")){
        	ackCount++;
        }
        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
  //    logger.info("Waiting for the response");
        selector.select(timeout);
        
        Set<SelectionKey> keys = selector.selectedKeys();
        if(keys.isEmpty() && ackCount<10){
           // logger.error("No response after timeout");
        	System.out.println("No response after timeout\nSending again");
        	resend(channel, p, routerAddress);
           // return 5;
        }
        else{
        	return;
        }
	}
    
    private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr,String msg) throws IOException {
		String dir = System.getProperty("user.dir");
        try(DatagramChannel channel = DatagramChannel.open()){
           sequenceNum++;
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(sequenceNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddr);				// sending http ftp request
            System.out.println("Piggybacking ACK with request..!");
            
            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(timeout);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
            	System.out.println("No response after timeout\nSending again");
            	resend(channel, p, routerAddr);
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            
            if(!receivedPackets.contains(resp.getSequenceNumber())){
         	   receivedPackets.add(resp.getSequenceNumber());
      
            if(msg.contains("Content-Disposition:attachment")){
            String[] responseArray = payload.split("\\|");
            
            File file = new File(dir+"/Downloads/"+responseArray[1].trim());
			file.createNewFile();
			
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			
			pw.print(responseArray[2]);
			pw.flush();
			pw.close();
            
            System.out.println(responseArray[0]);
            System.out.println("File downloaded in "+dir+"\\Downloads");
            }
            else{
            	System.out.println(payload);
            }
            
            sequenceNum++;
            Packet pAck = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(sequenceNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload("Received".getBytes())
                    .create();
            channel.send(pAck.toBuffer(), routerAddr);		// Sending ACK for the receival of the response
            
         // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(timeout);
            
            keys = selector.selectedKeys();
            if(keys.isEmpty()){
               // logger.error("No response after timeout");
            	//System.out.println("No close connection received.");
            	resend(channel, pAck, router);
            }

            ByteBuffer buf1 = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router1 = channel.receive(buf1);
            buf.flip();
            Packet resp1 = Packet.fromBuffer(buf1);
           // String payload1 = new String(resp1.getPayload(), StandardCharsets.UTF_8);
            System.out.println("Connection closed..!");
            keys.clear();
            
            sequenceNum++;
            Packet pClose = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(sequenceNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload("Ok".getBytes())
                    .create();
            channel.send(pClose.toBuffer(), routerAddr);
            System.out.println("OK sent");
            }
        }
    }
}
