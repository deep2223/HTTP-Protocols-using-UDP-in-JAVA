
Steps to run Project:-
****==>Import Http_UDP folder as project in Eclipse --> find three source files in source folder 
	
==>	1. Open Cmd and Run router_x86.exe file 
	
		you can set drop rate and max delay of router using below command
		
	-->	* without drop rate and max delay
			router_x86.exe --port=3000 --drop-rate=0 --max-delay=0s --seed=1
			
	-->	* with drop rate but without max delay
			router_x86.exe --port=3000 --drop-rate=0.2 --max-delay=0s --seed=1
			
	-->	* without drop rate but with max delay
			router_x86.exe --port=3000 --drop-rate=0 --max-delay=10ms --seed=1
		
	-->	* with both drop rate and max delay
			router_x86.exe --port=3000 --drop-rate=0.2 --max-delay=10ms --seed=1
			
==>	2. Run NewServer.java  
	
	Commands::
	
	--> * httpfs -v -p 8888
	
	--> * With -d option ::  httpfs -d (your destination folder path) -v -p 8888
	
==>	3. Run NewFtpClient.java 


	-->	* Implement GET /  :   httpfs get/ http://localhost:8888/get?

	-->	* Implement GET /filename:  httpfs get/hello.txt  http://localhost:8888/get?

	-->	* Implement POST /filename:  httpfs post/helo.txt http://localhost:8888/post?

	-->	* HTTP/1.1 404 FILE NOT FOUND  error message 
		: httpfs get/Downloads/2.txt http://localhost:8888/get?

	-->	* Concurrent Requests: Multiple client can read but can not write at same time.

	-->	* Content-Type and Content-Disposition: 
	
			httpfs get/hello -h Content-Type:txt http://localhost:8888/get
			httpfs get/hello.txt -h Content-Disposition:inline http://localhost:8888/get
			httpfs get/test -h Content-Type:json -h Content-Disposition:attachment http://localhost:8888/get
			
	-->	* -d option :
		    
			httpfs post/test.json http://localhost:8888/post? -d '{"Assignment":3}'
			
