package chat7T;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;

//서버가 보내는 Echo메세지를 읽어오는 쓰레드 클래스
public class Receiver extends Thread {

	Socket socket;
	BufferedReader in = null;

	public Receiver(Socket socket) {
		this.socket = socket;

		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		}
		catch (Exception e) {
			System.out.println("예외>Receiver>생성자:"+ e);
		}
	}

	@Override
	public void run() {

		while(in != null) {
			try {
				System.out.println("Thread Receive : "+ URLDecoder.decode(in.readLine(), "UTF-8"));
			}
			catch(SocketException ne) {
				System.out.println("SocketException");
				break;
			}
			catch (Exception e) {
				System.out.println("예외>Receiver>run1:"+ e);
			}
		}

		try {
			in.close();
		}
		catch (Exception e) {
			System.out.println("예외>Receiver>run2:"+ e);
		}
	}
}
