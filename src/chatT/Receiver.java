package chatT;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;

public class Receiver extends Thread{
	
	Socket socket;
	BufferedReader in = null;
	
	public Receiver(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		}
		catch(Exception e){
			System.out.println("예외> Receiver > 생성자:" + e);
		}
	}
	
	public void run() {
		while(in != null) {
			try {
				String r_msg = URLDecoder.decode(in.readLine(), "UTF-8");
				System.out.println("[R메시지] : " + r_msg);
			}
			catch(SocketException ne) {
				System.out.println("SocketException");
				break;
			}
			catch(Exception e) {
				System.out.println("예외 > Receiver > run1: " + e);
				break;
			}
		}
		try {
			in.close();
			System.out.println("예외 > Receiver > in스트림 종료");
			System.exit(0);
		}
		catch(Exception e) {
			System.out.println("예외 > Receiver > run2: " + e);
		}
		
	}

}
