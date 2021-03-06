package chat14;

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
import java.util.Scanner;


public class MultiServer extends IConnectImpl {

	//멤버변수 
	String toFixname = "";
	String forFixname = "";
	String blockName ="";
	String userName = "";
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	boolean acceptChoice;
	//클라이언트 정보저장을 위한 Map 컬렉션 생성
	Map<String,PrintWriter>clientMap;
	Thread mst;
	Map<String,String>allOfRoomList =new HashMap<String,String>();
	Map<String,String>privateRoomList =new HashMap<String,String>();
	Map <PrintWriter,String>roomJoiner = new HashMap<PrintWriter,String>();
	Map <String,String>roomList = new HashMap<String,String>();
	Map <String,String>passRoom = new HashMap<String,String>();
	Map <String,PrintWriter>roompeople = new HashMap<String,PrintWriter>();
	HashSet<String>blackList = new HashSet<String>();
	HashSet<String>pWords = new HashSet<String>();
	Scanner scan = new Scanner(System.in);
	Receiver receiver ;
	HashSet<String>set =new HashSet<String>();
	String king = "";
	//생성자
	public MultiServer() {
		super("kosmo","1234");
		//클라이언트의 이름과 출력스트림을 저장할 HashMap 컬렉션 생성
		clientMap = new HashMap<String,PrintWriter>();
		//HashMap 동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는 것을 차단함.
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(allOfRoomList);
		Collections.synchronizedMap(privateRoomList);
		Collections.synchronizedMap(roomJoiner);
		Collections.synchronizedMap(roomList);
	}
	//채팅서버 초기화
	public void init(){
		try {
			pWords.add("병신");
			pWords.add("개새끼");
			pWords.add("미친놈");
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
							name = "";
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
						if(clientMap.size()==15) {
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
						String newSentence = "";
						String[] pArr = s.split(" ");
						for(int i = 0 ; i<pArr.length;i++) {
							for(String a : pWords) {
								if(pArr[i].equals(a)) {
									pArr[i]="나쁜말";
								}
							}
							 newSentence+=pArr[i]+ " ";

						}
						Iterator<String>sendRoom =roomList.keySet().iterator();
						Iterator<PrintWriter>findOut =roomJoiner.keySet().iterator();
//						while(sendRoom.hasNext()) {
//							String joiner = sendRoom.next();
//							String joinRoomName = (String)roomList.get(joiner);
//							if(name.equals(joiner)) {
//								while(findOut.hasNext()) {
//									PrintWriter sendObject = findOut.next();
//									if(roomJoiner.get(sendObject).equals(joinRoomName)) {
//										set.add(joiner);
//									}
//									
//									
//								}
//							}
//						}
						if(newSentence==null) break;
						//서버의 콘솔에 출력되고
//						System.out.println(name + ">>" + newSentence);
//						System.out.println(passRoom.get(name));
//						System.out.println(privateRoomList.get(passRoom.get(name)));
						
						if(passRoom.containsKey(name)) {
							if(privateRoomList.get(passRoom.get(name)).equalsIgnoreCase(s)){
								set.add(name);
								roomJoiner.put(out, passRoom.get(name));
								roomList.put(name, passRoom.get(name));
								roompeople.put(name, out);
								clientMap.remove(name);
								sendAllMsg(name,name,name+"님이 입장하셨습니다.","room");
								passRoom.remove(name);
								
							}
							else {
								sendAllMsg(name,name,"비밀번호가 틀려 입장에 실패하셨습니다.","one");
								passRoom.remove(name);
							}
						}
										
						
						//클라이언트 측으로 전송한다.
						if(s.charAt(0)=='/') {
							String[] strArr = newSentence.split(" ");
							String msgContent = "";
							for(int i=2; i<strArr.length; i++) {
								msgContent += strArr[i]+ " ";
							}
							if(strArr[0].equals("/to")) {
								sendAllMsg(name,strArr[1], msgContent, "One");
							}
							else if(strArr[0].equals("/fixto")) {
								forFixname = name;
								toFixname = strArr[1];
								sendAllMsg(forFixname,toFixname,msgContent, "One");
							}
							else if(strArr[0].equals("/unfixto")) {
								forFixname = "";
								toFixname = "";
								sendAllMsg(name,name,"귓속말 고정옵션이 해제되었습니다.","One");
							}
							else if(strArr[0].equals("/block")) {
								userName = name;
								blockName = strArr[1];
								sendAllMsg(name,name,blockName+"님을 차단하셨습니다.","One");
							}
							else if(strArr[0].equals("/unblock")) {
								userName = "";
								blockName = "";
								sendAllMsg(name,name,blockName+"님을 차단해제셨습니다.","One");
							}
							else if(strArr[0].equals("/list")) {
								printJoinName(name);
								
							}
							else if(strArr[0].equals("/makeroom")) {
								king = name;
								publicRoom(strArr[1],strArr[2],name);
								if(strArr.length==4) {
									privateRoom(strArr[1],strArr[3],name);
								}
								else if(strArr.length==3) {
									
								}
								roomList.put(name, strArr[1]);
								roomJoiner.put(out, strArr[1]);
								roompeople.put(name,out);
								set.add(name);
								clientMap.remove(name);
							}
							else if(strArr[0].equals("/roomenter")) {
								boolean a = true;
								Iterator<String>searchPrv = privateRoomList.keySet().iterator();
								Iterator<String>searchPub = allOfRoomList.keySet().iterator();
								while(searchPrv.hasNext()) {
									String searchRoomName = searchPrv.next();
									if(strArr[1].equals(searchRoomName)) {
										sendAllMsg(name, name, "비밀번호를 입력해주세요.", "room");
										passRoom.put(name, strArr[1]);
										a = false;
									}
								}
								if(a) {
									while(searchPub.hasNext()) {
										String searchRoomName = searchPub.next();
										if(strArr[1].equals(searchRoomName)) {
											sendAllMsg(name, name, "채팅방에 입장하셨습니다.", "join");
											roomJoiner.put(out, strArr[1]);
											roomList.put(name, strArr[1]);
											set.add(name);
											roompeople.put(name,out);
											clientMap.remove(name);
											System.out.println(set.size());
											
										}
									}
								}
							}
							else if(strArr[0].equals("/roomlist")) {
								Iterator<String>printRoom = allOfRoomList.keySet().iterator();
								while(printRoom.hasNext()) {
									String roomList = printRoom.next();
									if(privateRoomList.containsKey(roomList)) {
										sendAllMsg(name, name,"[비공개]"+roomList , "search");
									}
									else {
										sendAllMsg(name, name,roomList , "One");                                         
									}
								}
							}
							else if(strArr[0].equals("/redcard")) {
								if(name.equals(king)) {
									String inroom = roomList.get(name);
									set.remove(strArr[1]);
									roomList.remove(strArr[1]);
									clientMap.put(strArr[1], roompeople.get(strArr[1]));
									roomJoiner.remove(roompeople.get(strArr[1]));
									roompeople.remove(strArr[1]);
									sendAllMsg("",name,strArr[1]+"님이 강퇴당하셨습니다.","room");
									sendAllMsg(name,strArr[1],inroom+" 방에서 강퇴당하셨습니다.","One");
									
								}
								else {
									sendAllMsg(name,name,"방장이 아니므로 실행할 수 없습니다.","room");
								}
							}
							else if(strArr[0].equals("/boom")) {
								if(name.equals(king)) {
									for(String e : set) {
										if(roomList.get(name).equals(roomList.get(e))) {
											sendAllMsg(e,e,"방장이 방을 폭파시켰습니다. 대기실로 이동합니다.","server");
											clientMap.put(e, roompeople.get(e));
											roomList.remove(e);
											roomJoiner.remove(roompeople.get(e));
											roompeople.remove(e);
//											set.remove(e);
											
										}
									}
								}
								else {
									sendAllMsg(name,name,"방장이 아니므로 실행할 수 없습니다.","server");
								}
							}
								
						}
						
						else {
							if(set.contains(name)) {
								for(String i : set) {
									if(name.equals(i)) {
										System.out.println(i);
										sendAllMsg("",name,newSentence,"room");
										
									}
								}
								
							}
							else if(forFixname.equals(name)) {
								sendAllMsg(forFixname,toFixname,newSentence,"One");
							}
							else {
								sendAllMsg("",name,newSentence,"All");
							}
						}
						
						
					}
				}
				
			}
			catch(NullPointerException e){
				
			}
			catch(SocketException e) {
				e.printStackTrace();
			}
			catch(Exception e) {
				e.printStackTrace();
//				System.out.println("예외 : " + e);
			}
			
			finally {
				try {
					clientMap.remove(name);
					sendAllMsg("","", name+"님이 퇴장하셨습니다.","All");
					System.out.println(name + "["+ Thread.currentThread().getName()+"] 퇴장");
					System.out.println("현재 접속자 수는" + clientMap.size()+"명 입니다.");
					in.close();
					out.close();
					socket.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}/************    /list 기능 구현 메소드     *****************/
		public void printJoinName(String name) {
			int howMany = 1;
			Iterator<String> print = clientMap.keySet().iterator();
			PrintWriter print_out = (PrintWriter)clientMap.get(name);
			while(print.hasNext()) {
				String printName = print.next();
				if(!(name.equals(printName))) {
					print_out.println(printName);
					howMany++;
				}
				
			}
			print_out.println(name+"님을 포함한 총 "+howMany+"명이 채팅방에 접속중입니다.");
		}
		public void publicRoom(String roomName,String limitJoin,String name) {
			allOfRoomList.put(roomName, limitJoin);
			sendAllMsg(name, name, "'"+roomName+"' "+"방을 생성하셨습니다. 총 인원 : 1/"+limitJoin, "room");
		}
		public void privateRoom(String roomName,String passWord,String name) {
			privateRoomList.put(roomName, passWord);
			sendAllMsg(name,name, "'"+roomName+"' "+"방은 비공개방으로, 비밀번호는 "+passWord+" 입니다.", "room");
		}
//		public void joinRoom(String room,String name) {
//			String pass=privateRoomList.get(room);
//			receiver = new Receiver(socket);
//			String answer=receiver.passwordReturn();
//			System.out.println(answer);
//			if(answer.equals(pass)) {
//				roomJoiner.put(out,room);
//				roomList.put(name, room);
//				set.add(name);
//				sendAllMsg(name,name,name+"님이 입장하셨습니다.","room");
//			}
//			else {
//				sendAllMsg(name,name,"비밀번호가 틀려 입장에 실패하셨습니다.","one");
//			}
//		}
		
		//접속된 모든 클라이언트 측으로 서버의 메세지를 Echo해주는 역할 담당
		public void sendAllMsg(String fname,String name, String msg, String flag) {
			
			//Map에 저장된 객체의 키값(대화명)을 먼저 얻어온다.
			Iterator<String> it = clientMap.keySet().iterator();
			Iterator<PrintWriter>that =roomJoiner.keySet().iterator();
			if(flag.equals("All")) {
				
				String sql = "INSERT INTO chat_talking values "
						+ "(idx_num.nextval,?,default,?,sysdate)";
				try {
					psmt = con.prepareStatement(sql);
					psmt.setString(1,name);
					psmt.setString(2, URLDecoder.decode(msg,"UTF-8"));
					
					int affected = psmt.executeUpdate();
					System.out.println(affected + "행이 업데이트 되었습니다.");
					System.out.println(fname);
				}
				catch(Exception e) {
					e.printStackTrace();
					
				}
				
			}
			if(flag.equals("room")) {
				while(that.hasNext()) {
					try {
						PrintWriter that_out = (PrintWriter)that.next();
						String room = roomJoiner.get(that_out);
						if(roomList.get(name).equals(room)) {
							that_out.println(URLEncoder.encode(msg,"UTF-8"));
						}
						
					}
					catch(UnsupportedEncodingException e1) {}
					catch(NullPointerException e) {}
				}
				
			}
//			저장된 객체(클라이언트)의 갯수만큼 반복한다.
			else {
				
				while(it.hasNext()) {
					try {
						//컬렉션의 key는 클라이언트의 대화명이다.
						String clientName = it.next();
						//각 클라이언트의 PrintWriter객체를 얻어온다.
						PrintWriter it_out = (PrintWriter)clientMap.get(clientName);
//						if(flag.equals("join") ) {
//							if(name.equals(clientName)) {
//								try {
//									it_out.println(URLEncoder.encode(msg,"UTF-8"));
//									
//								}
//								catch(UnsupportedEncodingException e1) {
//									
//								}
//							}
//						}
//					if(flag.equals("room")) {
//						for(String i : set) {
//							if(i.equals(clientName)) {
//								try {
//									it_out.println(URLEncoder.encode(msg,"UTF-8"));
//									
//								}
//								catch(UnsupportedEncodingException e1) {
//									
//								}
//							}
//						}
//					}
//						if(flag.equals("wrong") ) {
//							if(name.equals(clientName)) {
//								try {
//									it_out.println(URLEncoder.encode(msg,"UTF-8"));
//									
//								}
//								catch(UnsupportedEncodingException e1) {
//									
//								}
//							}
//						}
						if(flag.equals("search")) {
							if(name.equals(clientName)) {
								try {
									it_out.println("[서버]"+URLEncoder.encode(msg,"UTF-8"));
								}
								catch(UnsupportedEncodingException e1) {
									
								}
								
							}
						}
						if(flag.equals("One")) {
							//flag가 One이면 해당클라이언트 한명에게만 전송한다(귓속말)
							
							if(name.equals(clientName)) {
								//컬렉션에 저장된 접속자명과 일치하는 경우에만 메세지를 전송한다.
								try {
									it_out.println(URLEncoder.encode(fname,"UTF-8")+"[귓속말]"+URLEncoder.encode(msg,"UTF-8"));
									String sql = "INSERT INTO chat_talking values "
											+ "(idx_num.nextval,?,?,?,sysdate)";
									try {
										psmt = con.prepareStatement(sql);
										psmt.setString(1,URLDecoder.decode(fname,"UTF-8"));
										psmt.setString(2, URLDecoder.decode(name,"UTF-8"));
										psmt.setString(3, URLDecoder.decode(msg,"UTF-8"));
										
										int affected = psmt.executeUpdate();
										System.out.println(affected + "행이 업데이트 되었습니다.");
									}
									catch(Exception e) {
										
									}
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
							else {//차단한 사용자가 보낸 말을 차단당한 사용자에게 보내지 않는다.
								if(name.equals(userName)) {
									if(blockName.equals(clientName))
										continue;
									else {
										//메세지를 보낼때 사용되는 부분
										it_out.println("["+name+"]:"+URLEncoder.encode(msg,"UTF-8"));
										
									}
								}//차단된 사용자가 보낸말을 차단한 사용자에게 보내지 않는다
								else if(name.equals(blockName)) {
									if(userName.equals(clientName)) {
										continue;
									}
									else 
										it_out.println("["+name+"]:"+URLEncoder.encode(msg,"UTF-8"));
								}
								else {
									it_out.println("["+name+"]:"+URLEncoder.encode(msg,"UTF-8"));
								}
								
							}
						}
						
					}
					catch(Exception e) {
						System.out.println("예외"+e);
					}
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
