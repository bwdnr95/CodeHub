package chat8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


public class MultiServer {

	//멤버변수 
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	boolean acceptChoice;
	//클라이언트 정보저장을 위한 Map 컬렉션 생성
	Map<String,PrintWriter>clientMap;
	Thread mst;
	
	HashSet<String>blackList = new HashSet<String>();
	
	//생성자
	public MultiServer() {
		//클라이언트의 이름과 출력스트림을 저장할 HashMap 컬렉션 생성
		clientMap = new HashMap<String,PrintWriter>();
		//HashMap 동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는 것을 차단함.
		Collections.synchronizedMap(clientMap);
	}
	//채팅서버 초기화
	public void init(){
		try {
			blackList.add("kosmo");
			blackList.add("바보");
			blackList.add("대출");
			//서버 소켓 오픈
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			/*
			 1명의 클라이언트가 접속할때마다 접속을 허용(Accept)해주고
			 동시에 MultiServerT 쓰레드를 생성한다.
			 해당 쓰레드는 1명의 클라이언트가 전송하는 메세지를 읽어서 Echo
			 해주는 역할을 담당한다
			 */
			while(true) {
				
				//클라이언트의 접속 허가
				socket = serverSocket.accept();
				mst = new MultiServerT(socket);
				mst.start();
				
				System.out.println(socket.getInetAddress()+"(클라이언트)의"+
						socket.getPort()+ "포트를 통해 "+
						socket.getLocalAddress()+"(서버)의"+
						socket.getLocalPort()+"포트로 연결되었습니다.");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	/*
	 chat4 까지는 init()이 static이었으나, chat5부터는 일반적인
	 멤버메소드를 변경된다. 따라서 객체를 생성후 호출하는 방식으로 아래와 같이
	 변경된다.
	 */
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	/*
	 내부클래스
	 	: init()에 기술되었던 스트림을 생성후 메세지를 읽기/쓰기 하던
	 	부분이 해당 내부클래스로 이동되었다.
	 */
	class MultiServerT extends Thread {
		
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		/*
		 내부클래스의 생성자
		 	: 1명의 클라이언트가 접속할때 생성했던 Socket객체를
		 	매개변수로 받아 이를 기반으로 입출력 스트림을 생성한다.
		 */
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
			}
			catch(Exception e) {
				System.out.println("예외 : "+e);
				
			}
		}
		/*
		 쓰레드로 동작할 run()에서는 클라이언트의 접속자명과
		 메세지를 지속적으로 읽어 Echo해주는 역할을 한다.
		 */
		@Override
		public void run() {
			
			String name = "";
			String s = "";
			
			try {
				
				if(in != null) {
						//클라이언트의 이름을 읽어온다
					name = in.readLine();
					
					name = URLDecoder.decode(name,"UTF-8");
				
					Iterator<String> nameCompare = clientMap.keySet().iterator();
					while(nameCompare.hasNext()) {
						String nextName = nameCompare.next();
						if(nextName.equalsIgnoreCase(name)) {
							out.println("중복된 이름으로 접속하셔서 강퇴처리되셨습니다.");
							System.out.println("중복된 이름으로 접속하여 강퇴처리하였습니다.");
							this.interrupt();
							in.close();
							out.close();
							socket.close();
							return;
						
						}
						
					}
						for(String i : blackList) {
							if(i.equalsIgnoreCase(name)) {
								out.println("당신은 블랙리스트셔서 강퇴처리되셨습니다.");
								System.out.println("블랙리스트가 접속하여 강퇴처리하였습니다.");
								this.interrupt();
								in.close();
								out.close();
								socket.close();
								return;
							}
						}
						if(clientMap.size()==3) {
							out.println("채팅방인원제한으로 접속할 수 없습니다.");
							System.out.println("채팅방인원제한으로 강퇴처리하였습니다.");
							this.interrupt();
							in.close();
							out.close();
							socket.close();
							return;
						}
						clientMap.put(name, out);
							
					/*
					 방금 접속한 클라이언트를 제외한 나머지에게 입장을 알린다.
					 */
					sendAllMsg("","",name+"님이 입장하셨습니다.","All");
					//현재 접속한 클라이언트를 HashMap에 저장한다.
					
					
					
					//접속자의 이름을 서버의 콘솔에 띄워주고
					System.out.println(name + "접속");
					//HashMap에 저장된 객체의 수로 현재 접속자를 파악할 수 있다.
					System.out.println("현재 접속자 수는"+clientMap.size()+"명 입니다.");
					
					
					//입력한 메세지는 모든 클라이언트에게 Echo된다.
					while (in !=null) {
						s = in.readLine();
						s = URLDecoder.decode(s,"UTF-8");
						if(s==null) break;
						//서버의 콘솔에 출력되고
						System.out.println(name + ">>" + s);
						
						
						
						//클라이언트 측으로 전송한다.
						if(s.charAt(0)=='/') {
							String[] strArr = s.split(" ");
							String msgContent = "";
							for(int i=2; i<strArr.length; i++) {
								msgContent += strArr[i]+ " ";
							}
							if(strArr[0].equals("/to")) {
								sendAllMsg(name,strArr[1], msgContent, "One");
							}
						}
						else {
							sendAllMsg("",name,s,"All");
						}
						
						
					}
				}
				
			}
			catch(SocketException e	 ) {
				clientMap.remove(name);
				sendAllMsg("","", name+"님이 퇴장하셨습니다.","All");
				System.out.println(name + "["+ Thread.currentThread().getName()+"] 퇴장");
				System.out.println("현재 접속자 수는" + clientMap.size()+"명 입니다.");
			}
			catch(Exception e) {
				System.out.println("예외 : " + e);
			}
			
			finally {
				try {
					
					in.close();
					out.close();
					socket.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		//접속된 모든 클라이언트 측으로 서버의 메세지를 Echo해주는 역할 담당
		public void sendAllMsg(String fname,String name, String msg, String flag) {
			
			//Map에 저장된 객체의 키값(대화명)을 먼저 얻어온다.
			Iterator<String> it = clientMap.keySet().iterator();
			
			//저장된 객체(클라이언트)의 갯수만큼 반복한다.
			while(it.hasNext()) {
				try {
					//컬렉션의 key는 클라이언트의 대화명이다.
					String clientName = it.next();
					//각 클라이언트의 PrintWriter객체를 얻어온다.
					PrintWriter it_out = (PrintWriter)clientMap.get(clientName);
					
					if(flag.equals("One")) {
						//flag가 One이면 해당클라이언트 한명에게만 전송한다(귓속말)
						
						if(name.equals(clientName)) {
							//컬렉션에 저장된 접속자명과 일치하는 경우에만 메세지를 전송한다.
							try {
								it_out.println("[귓속말]"+URLEncoder.encode(msg,"UTF-8"));
							}
							catch(UnsupportedEncodingException e1) {}
						}
						
					}
					else {
						//그외에는 모든 클라이언트에게 전송한다
						
						/*
					 클라이언트에게 메세지를 전달할때 매개변수로 name이
					 있는 경우와 없는경우를 구분해서 전달하게 된다.
						 */
						if(name.equals("")) {
							//입장,퇴장에서 사용되는 부분
							try {
								it_out.println(URLEncoder.encode(msg,"UTF-8"));
							} 
							catch(UnsupportedEncodingException e1) {}
						}
						else {
							//메세지를 보낼때 사용되는 부분
							it_out.println("["+name+"]:"+msg+"\n");
						}
						
					}
				}
				catch(Exception e) {
					System.out.println("예외"+e);
				}
			}
			try {
				out.println(URLEncoder.encode(("> " + name + " ==>" + msg),"UTF-8"));
			}
			catch(UnsupportedEncodingException e1) {}
			catch(Exception e) {
				System.out.println("예외" + e);
			}
		}
	}

}
