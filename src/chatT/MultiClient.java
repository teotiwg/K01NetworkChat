package chatT;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {
		
		System.out.println("이름을 입력하세요: ");
		Scanner scan = new Scanner(System.in);
		String s_name = scan.nextLine();
		
		PrintWriter out = null;
		
		try {
			String ServerIP = "localhost";
			
			if(args.length > 0) {
				ServerIP = args[0];
			}
			
			Socket socket = new Socket(ServerIP, 9999);
			System.out.println("서버와 연결됐습니다.");
			
			Thread receiver = new Receiver(socket);
			receiver.start();
			
			Thread sender = new Sender(socket, s_name);
			sender.start();
			
		}
		catch(Exception e) {
			System.out.println("예외발생[MultiClient] " + e);
			System.out.println("다시 접속해주세요.");
		}
		

	}

}
