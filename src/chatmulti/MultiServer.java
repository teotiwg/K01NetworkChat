package chatmulti;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MultiServer {
	
	//멤버변수
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	//클라이언트 정보저장을 위한 Map 컬렉션 생성	
	Map<String, PrintWriter> clientMap;
	Map<String, String> secretDM;
	HashSet<String> black = new HashSet<String>();
	HashSet<String> pWords = new HashSet<String>();

	//생성자
	public MultiServer() {
		//클라이언트의 이름과 출력스트림을 저장할 HashMap 컬렉션 생성
		clientMap = new HashMap<String, PrintWriter>();
		secretDM = new HashMap<String, String>();
		//HashMap 동기화설정. 쓰레드가 사용자정보에 동시에 접근하는것을 차단
		Collections.synchronizedMap(clientMap);
		black = new HashSet<String>();
		pWords = new HashSet<String>();
	}
	
	public interface NumOfMem{
		int MAX = 2;
	}
	
	//채팅 서버 초기화
	public void init() {
		try {
			//서버 소켓 오픈
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			/*
			1명의 클라이언트가 접속할때마다 접속을 허용(Accept)해주고 동시에 MultiServerT 쓰레드를 생성. 
			해당 쓰레드는 1명의 클라이언트가 전송하는 메세지를 읽어서 Echo해주는 역할을 담당. 
			 */
			if(clientMap.size() >= NumOfMem.MAX) { // -------------------인원제한
				System.out.println("인원이 초과됐습니다. 접속을 차단합니다.");
				Thread.currentThread().interrupt();
			}
			else {
				while(true) {
				//클라이언트의 접속 허가
					socket = serverSocket.accept();
					System.out.println(
						socket.getInetAddress() + "(클라이언트)의 "
						+socket.getPort() + " 포트를 통해 "
						+socket.getLocalAddress() + "(서버)의"
						+socket.getLocalPort() + " 포트로 연결됨");
					//쓰레드로 정의된 내부클래스 객체생성 및 시작
					//클라이언트 한명당 하나씩의 쓰레드가 생성된다. 
					Thread mst = new MultiServerT(socket);
				mst.start();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/*
	chat4까지는 init()이 static이었으나, chat5부터는 일반적인 멤버메소드를 변경된다. 
	따라서 객체를 생성후 호출하는 방식으로 변경된다. 
	 */
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	
	public boolean blackCheck(String word) {
		black.add("낙현");
		black.add("인하");
		black.add("혜수");
		
		for(String check : black) {			
			if(word.equals(check))
				return true;
		}
		return false;	
	}
	
	public String banCheck(String word) {
		pWords.add("대출");
		pWords.add("스팸");
		pWords.add("광고");
		
		for(String check : pWords) {			
			if(word.equals(check) || word.contains(check)) {
				String bReplace = word.replace(check, "XX");
				return bReplace;
			}
		}
		return word;
	}

	//접속된 모든 클라이언트 측으로 서버의 메세지를 Echo해주는 역할 담당
	public void sendAllMsg(String name, String msg, String flag)
	{
		//Map에 저장된 객체의 키값(대화명)을 먼저 얻어온다. 
		Iterator<String> it = clientMap.keySet().iterator();
		//저장된 객체(클라이언트)의 갯수만큼 반복한다. 
		while(it.hasNext()) {
			try {
				// 컬렉션의 key는 클라이언트의 대화명
				String clientName = it.next();
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				PrintWriter it_out = (PrintWriter) clientMap.get(clientName);
						//(PrintWriter) clientMap.get(it.next());
				
				if(flag.equals("One")) {
					// flag가 One이면 해당 클라이언트 한명에게만 전송(귓속말)
					if(name.equals(clientName)) {
						// 컬렉션에 저장된 접속자명과 일치하는 경우에만 메시지를 전송
						//it_out.println("[귓속말]" + msg);
						it_out.println(URLEncoder.encode("[귓속말]" + name + " : " + msg, "UTF-8"));
					}
				}
				else {
					// 그 외엔 모든 클라이언트에게 전송
					// 클라이언트에게 메세지를 전달할 때 매개변수로 name이 있는 경우와 없는 경우를 구분해서 전달하게 됌. 
					if(name.equals("")) {
						//입장, 퇴장에서 사용되는 부분
						it_out.println(URLEncoder.encode(msg, "UTF-8"));
					}
					else {
						//메세지를 보낼때 사용되는 부분
						it_out.println(URLEncoder.encode("["+ name + "] "+ msg, "UTF-8"));
						//it_out.println(URLEncoder.encode(msg, "UTF-8"));
					}
				}
			}
			catch(Exception e) {
				System.out.println("예외 : " + e);
			}
		}
	}
	/*
	내부클래스
		: init()에 기술되었던 스트림을 생성 후 메세지를 읽기/쓰기 하던 부분이 해당 내부클래스로 이동.
	 */
	class MultiServerT extends Thread {
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		/*
		내부클래스의 생성자
			: 1명의 클라이언트가 접속할때 생성했던 Socket객체를 매개변수로 받아 이를 기반으로 입출력 스트림을 생성. 
		 */
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				//in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
			}
			catch (Exception e) {
				System.out.println("예외: "+ e);
			}
		}
		/*
		쓰레드로 동작할 run()에서는 클라이언트의 접속자명과 메세지를 지속적으로 읽어 Echo해주는 역할. 
		 */
		@Override
		public void run() {
			String name = "";
			String s = "";
			
			//if(clientMap.size() >= NumOfMem.MAX) { // -------------------인원제한
				//System.out.println("인원이 초과됐습니다. 접속을 차단합니다.");
				//Thread.currentThread().interrupt();
//				System.out.println("차단1");
//				Thread.interrupted();
//				System.out.println("차단2");
			//}
			try {
				//클라이언트가 보내는 최초메세지는 대화명이므로 접속에 대한 부분을 출력하고, Echo함.
				//클라이언트의 이름을 읽어온다.
				name = in.readLine();
				name = URLDecoder.decode(name, "UTF-8");
				if(clientMap.containsKey(name) == true) { 
					Thread.currentThread().interrupt();
				}
				else if(blackCheck(name) == true) {
					// 신규 접속시 블랙리스트 Collection을 검사하여 접속을 차단한다.
					Thread.currentThread().interrupt();
				}
				else {
					//방금 접속한 클라이언트를 제외한 나머지에게 입장을 알린다.
					sendAllMsg("", name + "님이 입장하셨습니다.", "All");
					//현재 접속한 클라이언트를 HashMap에 저장한다. 
					clientMap.put(name, out);
					//접속자의 이름을 서버의 콘솔에 띄워주고						
					System.out.println(name +" 접속");
					//HashMap에 저장된 객체의 수로 현재 접속자를 파악할 수 있다.
					System.out.println("현재 접속자 수는 "+ clientMap.size()
						+"명 입니다.");
					//입력한 메세지는 모든 클라이언트에게 Echo된다. 
					while (in != null) {// ------------------> 요것때문에 엔터누르면 먹통되는건가
						s = in.readLine();
						s = URLDecoder.decode(s, "UTF-8");
						//System.out.println(s);
						if ( s == null )
							//continue;
							break;
						// 금칙어 체크
						s = banCheck(s);
						//서버의 콘솔에 출력되고...
						System.out.println(name + " >> " + s);
						//클라이언트 측으로 전송한다. 
						if(s.charAt(0) == '/') {
							String[] strArr = s.split(" ");
							String msgContent = "";
							for(int i=2; i<strArr.length; i++) {
								msgContent += strArr[i] + " ";
							}
							if(strArr[0].equals("/to")){
								sendAllMsg(strArr[1], msgContent, "One");
							}  
							else if(strArr[0].equals("/fixto")) {
								sendAllMsg(strArr[1], msgContent, "One");
									while(!strArr[0].equals("/unfixto")) {
										s = in.readLine();
										s = URLDecoder.decode(s, "UTF-8");	
										String[] check = s.split(" ");
										if(check[0].equals("/unfixto")) {
											strArr[0] = "/unfixto";
											break;
										}
										else {
											sendAllMsg(strArr[1], s, "One");
										}
									}
							}
							else if(strArr[0].equals("/block") ) { //--------------------- 블록 차단
								if(clientMap.containsKey(strArr[1])) {
									
								}
								
								break;
								//String[] checkArr = s.split(" ");
								//String block = strArr[1];
								//if(block.equals(name)) {}
							}
							else if(strArr[0].equals("/list")) {
								System.out.println("[접속자 명단]" + clientMap.keySet());
								
								String list = "[접속자 명단]" + clientMap.keySet();
								System.out.println(list);
								sendAllMsg("", list, "All");
							}
						}
						else {
							sendAllMsg(name, s, "All");
						}
					}
				}
			}
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
			finally {				
				// 클라이언트가 접속을 종료하면 Socket예외가 발생하게 되어 finally절로 진입하게 됌. 
				// 이때 "대화명"을 통해 정보를 삭제. 
				sendAllMsg("", name + "님이 퇴장하셨습니다.", "All");
				clientMap.remove(name);  // ===> 조건문으로? 보완 필요
				//System.out.println(Thread.currentThread().getName());
				clientMap.remove(Thread.currentThread().getName()); // 수정필요!!!!!!!!!!!!!!!!!!!!
				System.out.println(name + " [" + Thread.currentThread().getName() +  "] 퇴장");
				//System.out.println(name + " 퇴장");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명입니다.");
								
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
	}
}
