package chatT;

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

import chat9.Const;

public class MultiServer extends DBConnect{
	
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	Map<String, PrintWriter> clientMap;
	HashSet<String> blackListSet = new HashSet<String>();
	HashSet<String> pWords = new HashSet<String>();
	HashMap<String, String> tofixMap = new HashMap<String, String>();
	HashMap<String, String> blockMap = new HashMap<String, String>();
	
	public MultiServer() {
		clientMap = new HashMap<String, PrintWriter>();
		Collections.synchronizedMap(clientMap);
		
		blackListSet.add("낙현"); blackListSet.add("인하"); blackListSet.add("혜수");
		
		pWords.add("대출"); pWords.add("광고"); pWords.add("스팸");
	}
	
	public interface Const{
		int CLIENT_CNT = 3;
	}
	
	public void init() {
		try {
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작됐습니다.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println("로컬서버: " + socket.getLocalAddress() + ":" + socket.getLocalPort());
				System.out.println("원격클라이언트: " + socket.getInetAddress() + ":" + socket.getPort());
				
				Thread mst = new MultiServerT(socket);
				
				if(clientMap.size() >= Const.CLIENT_CNT) {
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(URLEncoder.encode("정원(" + Const.CLIENT_CNT + "명)을 초과하여 접속하실 수 없습니다.", "UTF-8"));
					break;
				}
				else {
					mst.start();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
				socket.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void sendAllMsg(String name, String msg, String flag) {
		
		Iterator<String> it = clientMap.keySet().iterator();
		String clientName;
		
		while(it.hasNext()) {
			try {
				clientName = it.next();
				
				String blockVal = null;
				boolean cFlag = true;
				if(blockMap.containsKey(clientName)) {
					blockVal = blockMap.get(clientName);
					String[] blockNameArr = blockVal.split("|");
					for(int i = 0; i < blockNameArr.length-1; i++) {
						if(name.equals(blockNameArr[i]))
							cFlag = false;
					}
					if(cFlag == false) continue;
				}
				
				PrintWriter it_out = (PrintWriter) clientMap.get(clientName);
				
				String pwComment = "금지단어가 포함됐습니다.";
				for(String s : pWords) {
					if(msg.indexOf(s) != -1) {
						msg = pwComment;
						
						System.out.println(pwComment);
						it_out.println("[" + URLEncoder.encode(name, "UTF-8") + "] " + URLEncoder.encode(pwComment, "UTF-8"));
						return;
					}
				}
				
				if(flag.equals("One")) {
					if(name.equals(clientName)) {
						it_out.println("[" + URLEncoder.encode("귓속말", "UTF-8") + "]" + 
										URLEncoder.encode(msg,"UTF-8"));
					}
				}
				else
					if(name.equals("")) {
						it_out.println(URLEncoder.encode(msg, "UTF-8"));
					}
					else {
						it_out.println("[" + URLEncoder.encode(name, "UTF-8") + "]" + URLEncoder.encode(msg, "UTF-8"));
					}
			}
			catch(Exception e) {
				System.out.println("예외 : " + e);
			}
		}
	}
	
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	
	public void addBlock(String user, String blockUser, char flag) {
		if(flag == '+') {
			if(blockMap == null) {
				blockMap.put(user, blockUser + "|");
			}
			else {
				if(blockMap.containsKey(user)) {
					blockMap.put(user, blockMap.get(user) + blockUser + "|");
				}
				else {
					blockMap.put(user,  blockUser + "|");
				}
			}
		}
		else if(flag == '-') {
			if(blockMap != null && blockMap.containsKey(user)) {
				String newblockUser = blockMap.get(user).replace(blockUser + "|", "");
				blockMap.put(user,  newblockUser);
			}
		}
		System.out.println(blockMap);
	}
	
	
	class MultiServerT extends Thread{
		
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
			}
			catch(Exception e) {
				System.out.println("예외: " + e);
			}
		}
		
		public void run() {
			String name = "";
			String s = "";
			
			try {
				name = URLDecoder.decode(in.readLine(), "UTF-8");
				
				Iterator<String> itr = clientMap.keySet().iterator();
				
				while(itr.hasNext()) {
					String connName = itr.next();
					if(connName.equalsIgnoreCase(name)) {
						System.out.println("동일한 대화명이 존재합니다. 접속을 종료합니다.");
						out.println(URLEncoder.encode("동일한 대화명이 존재합니다. 접속을 종료합니다.", "UTF-8"));
						
						name = name + "temp";
						this.interrupt();
						return;
					}
				}
				
				
				for(String b : blackListSet) {
					if(name.equals(b)) {
						System.out.println("블랙리스트입니다. 접속을 종료합니다.");
						out.println(URLEncoder.encode("블랙리스트입니다. 접속을 종료합니다.", "UTF-8"));
						name = name + "temp";
						this.interrupt();
						return;
					}
				}
				
				
				sendAllMsg("", name + "님이 입장하셨습니다.", "All");
				
				clientMap.put(name, out);
				
				System.out.println(name + "> 접속");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명입니다.");
				
				
				while(in != null) {
					s = URLDecoder.decode(in.readLine(), "UTF-8");
					if(s == null) break;
					
					
					try {
						String query = "INSERT INTO chatting_tb VALUES (chatting_seq.nextval, ?, ?, sysdate)";
						psmt = con.prepareStatement(query);
						psmt.setString(1,  name);
						psmt.setString(2, s);
						int affected = psmt.executeUpdate();
						System.out.println(query + "행이 입력됐습니다.");
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					
					
					if(s.equals("/unfixto")) {
						tofixMap.remove(name);
						out.println(URLEncoder.encode("귓속말 고정이 해제되었습니다.", "UTF-8"));
					}
					else {
						if(tofixMap != null && tofixMap.containsKey(name)) {
							s = "/to " + tofixMap.get(name) + " " + s;
							System.out.println("s = " + s);
						}
					}
					
					
					if(s.charAt(0) == '/') {
						if(s.equals("/list")) {
							StringBuffer sb = new StringBuffer();
							sb.append("[접속자리스트]\n");
							
							Iterator<String> it = clientMap.keySet().iterator();
							while(it.hasNext()) {
								sb.append(it.next() + "\n");
							}
							sb.append("--------------------------------");
							System.out.println("[" + name + "]님이 리스트를 출력했습니다.");
							out.println(URLEncoder.encode(sb.toString(), "UTF-8"));
						}
						else {
							String[] strArr = s.split(" ");
							
							if(strArr[0].equals("/to")) {
								String contents = "";
								for(int i = 2; i < strArr.length; i++) {
									contents += strArr[i] + " ";
								}
								sendAllMsg(strArr[1], "[" + name + "]" + contents, "One");
								System.out.println("[" + name + "]님이 ["+strArr[1]+"]님꼐 귓속말을 보냈습니다.");
								out.println(URLEncoder.encode("[" + name + "]님이 ["+strArr[1]+"]님꼐 귓속말을 보냈습니다.", "UTF-8"));
							}
							else if(strArr[0].equals("/tofix")) {
								System.out.println(name + " <> " + strArr[1]);
								tofixMap.put(name, strArr[1]);
								out.println(URLEncoder.encode(strArr[1]+ "님께 귓속말이 고정됐습니다.", "UTF-8"));
							}
							else if(strArr[0].equals("/block")) {
								addBlock(name, strArr[1], '+');
								out.println(URLEncoder.encode(strArr[1] + "님이 차단됐습니다.", "UTF-8"));
							}
							else if(strArr[0].equals("/unblock")) {
								addBlock(name, strArr[1], '-');
								out.println(URLEncoder.encode(strArr[1] + "님이 차단 해제됐습니다.", "UTF-8"));
							}
						}
					}	
					else {
						System.out.println(name + ">>" + s);
						sendAllMsg(name, s, "All");
					}
				}
				
			}
			catch(Exception e) {
				System.out.println("예외: " + e);
				e.printStackTrace();
			}
			finally {
				clientMap.remove(name);
				sendAllMsg("", name + "님이 퇴장하셨습니다.", "All");
				System.out.println(name + "[ " + Thread.currentThread().getName() + "] 퇴장");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명입니다.");
				
				try {
					in.close();
					out.close();
					socket.close();
					System.out.println("종료?");
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		
	}

}
