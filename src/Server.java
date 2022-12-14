import java.awt.Label;

import java.awt.Panel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 *
 * @author HuuDuc_20NS_VKU
 */
public class Server {
	DatagramSocket server_DatagramSocket;	
	InetAddress server_ip;
	final int server_port = 2222;
	String server_sendData;
	String server_receivedData;
	final int size_of_packet = 4096;
	Database db = new Database();
	
	private JTextArea listMail = new JTextArea(20,15);
	private Label listMail_Array[] = new Label[15];
	private String listMailTime_Array[];
	private String listMailView_Array[];
	private int listMailStatus_Array[];
	private int num_inbox=0;
	public Server() {
		listMailTime_Array = new String[100];
		listMailView_Array = new String[100];
		listMailStatus_Array = new int[100];
		
		try {
			server_ip = InetAddress.getByName("localhost");
			server_DatagramSocket = new DatagramSocket(server_port,server_ip);
			System.out.println("Server is running");
			JOptionPane.showMessageDialog(new Panel(),"Server đã chạy. Bấm ok để tiếp tục","Thông báo",JOptionPane.PLAIN_MESSAGE);
			while (true) {
				// Nhận gói tin từ Client bất kỳ
				byte[] server_receivedByte = new byte[size_of_packet];
				DatagramPacket server_receivedPacket = new DatagramPacket(server_receivedByte, server_receivedByte.length);
				server_DatagramSocket.receive(server_receivedPacket);
				server_receivedData = new String(server_receivedPacket.getData()).trim();
				
				// Xử lý gói tin
				server_sendData = process(server_receivedData);
				//System.out.printf(server_sendData + " " +server_receivedData + "\n");
				// Hồi đáp cho Client tương ứng
				InetAddress client_ip = server_receivedPacket.getAddress();
				int client_port = server_receivedPacket.getPort();
				byte[] server_sendByte = new byte[size_of_packet];
				server_sendByte = server_sendData.getBytes();
				DatagramPacket server_sendPacket = new DatagramPacket(server_sendByte, server_sendByte.length,client_ip,client_port);
				server_DatagramSocket.send(server_sendPacket);
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(new Panel(),"Server đã chạy rồi","Cảnh báo",JOptionPane.WARNING_MESSAGE);
			System.exit(0);
			//e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		new Server();
	}
	
	public String process(String s) {
		/* Quy ước gửi gói tin:
	 	Client --> Server
			0: register
			1: login 
			2: resetPass (getPass)
			3: changePass
			4: send
			5: reply
			6: forward
			7: read
			8: delete
			9: logout
		
		Phân biệt giữa các chuỗi truyền đi: ^$^
	*/
		switch (s.charAt(0)) {
			case '0':
				return Register(s);
			case '1':
				return Login(s);
			case '2':
				return GetPass(s);
			case '3':
				return ChangePass(s);
			case '4':
				return Send("4",s);
			case '5':
				return Send("5",s); //Reply
			case '6':
				return Forward(s);
			case '7':
				return Read(s);
			case '8':
				return Delete(s);
			case '9':
				return Logout(s);
			case 'u':
				return UpdateStatus(s);
			case 's':
				if (db.isRightUser(s.substring(1,s.length()-1))) return "1";
				else return "0";
		}
		return "0";
	}
	
	public String Login(String s) {
		String user="", pass="";
		// xử lý cắt, tách chuỗi nhận được để có user & pass 
		for (int i=1; i<= s.length()-4; i++)
			if ((s.charAt(i) == '^') && (s.charAt(i+1) == '$') && (s.charAt(i+2) == '^')) {
				user = s.substring(1,i);
				pass = s.substring(i+3, s.length()-1);
				break;
			}		
		if (db.isRightUser(user) && db.isRightPass(user, pass)) return "1";
		return "0";
	}
	
	public String Register(String s) {
		String user="", pass="", resetPass="";
		boolean kt;
		for (int i=0; i<= s.length()-3; i++)
			if (s.charAt(i) == ' ') {
				int    len = Integer.parseInt(s.substring(1, i));
				System.out.print("\n" + len);
				s=s.substring(i+1,s.length());
				user=s.substring(0, len);
				s=s.substring(len, s.length());
				System.out.printf("\nuser: " + user);
            //System.out.printf("\ns=" + s);
				break;
			}

		for (int i=0; i<= s.length()-3; i++)
			if (s.charAt(i) == ' ') {
				int    len = Integer.parseInt(s.substring(0, i));
				System.out.print("\n" + len);
				s=s.substring(i+1,s.length());
				pass=s.substring(0, len);
				s=s.substring(len, s.length());
				System.out.printf("\npass: " + pass);
				//System.out.printf("\ns=" + s);
				break;
			}
		resetPass = s.substring(0, s.length() - 1);
		System.out.printf("\nresetPass: " + resetPass);
		if( db.create_User(user, pass, resetPass) && (Create_User_Directory(user))) return "1";		
		return "0";
	}
	
	public boolean Create_User_Directory(String s) {
		boolean kt=false;
		kt = (new File("TVMail_Server_Data\\" + s)).mkdir();
		if (kt) kt = (new File("TVMail_Server_Data\\" + s + "\\inbox")).mkdir();
		else return kt;
		if (kt) kt = (new File("TVMail_Server_Data\\" + s + "\\outbox")).mkdir();
		else return kt;
		if (kt) kt = (new File("TVMail_Server_Data\\" + s + "\\drafts")).mkdir();
		else return kt;
		if (kt) kt = (new File("TVMail_Server_Data\\" + s + "\\trash")).mkdir();
		else return kt;
		if (kt) {
			try {
				kt = (new File("TVMail_Server_Data\\" + s + "\\contact.txt")).createNewFile();
			}
			catch (Exception e) {
				return kt;
			}
		}
		else return kt;
		if (kt) {
			try {
				kt = (new File("TVMail_Server_Data\\" + s + "\\status.txt")).createNewFile();
			}
			catch (Exception e) {
				return kt;
			}
		}
		else return kt;
		return kt;
	}
	
	public String GetPass(String s) {
		//String user="", resetPass="";
		// xử lý chuỗi nhận được để có user & resetPass
		// Nếu user & resetPass đúng thì return pass
		// Ngược lại nếu sai thì return "Tên đăng nhập hoặc mã khôi phục mật khẩu sai"
		
		String user2="", resetPass="";
		// xử lý chuỗi nhận được để có user & resetPass

                for (int i=1; i<= s.length()-4; i++)

			if ((s.charAt(i) == '^') && (s.charAt(i+1) == '$') && (s.charAt(i+2) == '^')) {
				user2 = s.substring(1,i);
				 resetPass = s.substring(i+3, s.length()-1);
				 break;

			}
		if (db.isRightUser(user2) && db.isRight_resetPass(user2, resetPass)) {
			System.out.printf("\ndung");
				return db.getPass(user2);
								
		}

		// Nếu user & resetPass đúng thì return pass
		// Ngược lại nếu sai thì return "Tên đăng nhập hoặc mã khôi phục mật khẩu sai"
		
		return "0";
	}

	public String ChangePass(String s) {
		
		
		return "0";
	}
	
	
	public String Send(String check_send,String s) {
		String from="", to="", subject="", content="";
		
		System.out.printf("\ns=\'" + s +"\'\n");
	    for (int i=0; i<= s.length()-3; i++)
	    	if (s.charAt(i) == ' ') {
	    		int len = Integer.parseInt(s.substring(1, i));
	    		System.out.print("\n" + len);
	    		s=s.substring(i+1,s.length());
	    		from=s.substring(0, len);
	    		s=s.substring(len, s.length());
	            System.out.printf("\nfrom: \'" + from + "\'");
	            //System.out.printf("\ns=" + s);
	            break;
			}

	    for (int i=0; i<= s.length()-3; i++)
	    	if (s.charAt(i) == ' ') {
	    		int len = Integer.parseInt(s.substring(0, i));
	    		System.out.print("\n" + len);
	    		s=s.substring(i+1,s.length());
	    		to=s.substring(0, len);
	    		s=s.substring(len, s.length());
	    		System.out.printf("\nto: \'" + to + "\'");
	    		//System.out.printf("\ns=" + s);
	    		break;
		}
	    
	    for (int i=0; i<= s.length()-3; i++)
	    	if (s.charAt(i) == ' ') {
	    		int len = Integer.parseInt(s.substring(0, i));
	    		System.out.print("\n" + len);
	    		s=s.substring(i+1,s.length());
	    		subject=s.substring(0, len);
	    		s=s.substring(len, s.length());
	    		System.out.printf("\nsubject: \'" + subject + "\'");
	    		//System.out.printf("\ns=" + s);
	    		break;
		}
	    content = s.substring(0, s.length() - 1);
	    System.out.printf("\ncontent: \'" + content + "\'");
	    
		String path="",file="";
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		SimpleDateFormat sdf_yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
		file = "_" + sdf_yyyyMMdd.format(cal.getTime()) + ".txt";
		cal.setTime(new Date());
		System.out.println(sdf_yyyyMMdd.format(cal.getTime()));//2008/01/31
		
		
		
		System.out.printf("\n\'" + file + "\'");
		
	    path = "TVMail_Server_Data\\" + from + "\\outbox\\" + file;
	    boolean kt=false;
		System.out.printf(path);
		try {
			FileWriter fw = new FileWriter(path);
			//BufferedWriter br = new BufferedWriter(fr);
			fw.write("0\r\n");
			String temp=to;
			while (temp.length() < 22) temp +=" ";
			//fw.write("0\r\n");
			fw.write("to: " + temp + "\r\n");
			fw.write("subject: " + subject + "\r\n");
			fw.write("content:\r\n " + content + "\r\n");
			System.out.printf("da ghi xong");
			fw.close();
			kt = true;
		}
		catch (Exception e) {
			System.out.printf("1 loi loi");
		}
		
		if (kt) {
			try {
				path = "TVMail_Server_Data\\" + to + "\\inbox\\" + file;
				System.out.printf(path);
				FileWriter fw = new FileWriter(path);
				//BufferedWriter br = new BufferedWriter(fr);
				fw.write("1\r\n");
				while (from.length() < 22) from +=" ";
				//fw.write("0\r\n");
				fw.write("from: " + from + "\r\n");
				fw.write("subject: " + subject + "\r\n");
				fw.write("content:\r\n " + content + "\r\n");
				System.out.printf("da ghi xong");
				fw.close();
				return "1";
			}
			catch(Exception e) {
				System.out.printf("2 loi loi");
			}
		}
		//else if (kt) return "1";
		return "0";
	}
	
	public String Reply(String s) {
		
		
		return "0";
	}
	
	public String Forward(String s) {
		
		
		return "0";
	}
	
	public String UpdateStatus(String s) {
		String username="",path="", file="";
		for (int i=1; i < s.length(); i++) {
			if (s.charAt(i) == '$') {
				username = s.substring(1,i);
				file = s.substring(i+1, s.length());
				break;
			}
		}
		path = "TVMail_Server_Data\\" + username + "\\inbox\\" + file;
		System.out.printf(path);
		for (int i=0; i< num_inbox; i++) {
			int vt=1;
			//if (i==0) vt=2;
			if (listMailTime_Array[i].compareTo(file) == 0) {
				listMailView_Array[i] = "0" + listMailView_Array[i].substring(vt,listMailView_Array[i].length());
				System.out.printf("\n" + listMailView_Array[i]);
				//File f= new File(path);
				try {
					FileWriter fw = new FileWriter(path);
					//BufferedWriter br = new BufferedWriter(fr);
					fw.write(listMailView_Array[i]);
					System.out.printf("da ghi xong");
					fw.close();
				}
				catch (Exception e) {
					System.out.printf("loi loi");
				}
				break;
			}
		}
		
		
		
		return "thanh cong";
	}
	public String Read(String s) {
		System.out.printf(s);
		String username="", dir="",path="";
		dir = s.substring(1,2);
		System.out.printf("\n" + dir);
		username = s.substring(2,s.length());
		System.out.printf("\n" + username);
		path = "TVMail_Server_Data\\" + username;
		switch (dir.charAt(0)) {
			case '1':
				path += "\\inbox";
				break;
			case '2':
				path += "\\outbox";
				break;
			case '3':
				path += "\\drafts";
				break;
			case '4':
				path += "\\trash";
				break;
				
		
		}
		
		System.out.printf("\n" + path);
		File f= new File(path);
		//new File("TVMail_Server_Data\\" + s));
		
		File[] f_array = f.listFiles();
		
		num_inbox = 0;
		int dem;
		for(File t:f_array) {
			if (t.isFile()) {				
				listMailTime_Array[num_inbox] = new String("");
				listMailTime_Array[num_inbox] = t.getName();
				
				listMailView_Array[num_inbox] = new String("");
				try {
					FileReader fr = new FileReader(t.getPath());
			    	BufferedReader br = new BufferedReader(fr);
			    	String x="";
			    	dem=0;
			    	do {
			    		x = br.readLine();
			    		x.trim();
			    		System.out.printf("\n Độ dài: " + String.valueOf(x.length()) + "\'" + x + "\'");
			    		if(x==null) break;
			    		/*
			    		if (dem == 1) {
			    			if (x.length() < 30) {
			    				int len = x.length(); 
			    				for (int i=1; i< 30 - len; i++) x += " "; 
			    			}
			    		}
			    		*/
			    		
			    		
			    		//System.out.printf("\n X mới: " + String.valueOf(x.length()) + x);
			    		//if (dem == 3) listMailView_Array[num_inbox] += "  "; 
			    			listMailView_Array[num_inbox] += x + "\r\n";
			    		dem++;
			    	} while (x!=null);
			    	fr.close();
			    	br.close();
				}
				catch(Exception e) {
					
				}
				
				
				num_inbox++;
			}			
		}
		
		if (num_inbox > 0) {
			// Xếp thư theo thứ tự mới nhất ở đầu tiên
			for (int i=0; i< num_inbox - 1; i++) {
				for (int j=i+1; j<= num_inbox - 1; j++) {
					if (listMailTime_Array[i].compareTo(listMailTime_Array[j]) < 0) {
						String temp = listMailTime_Array[i];
						listMailTime_Array[i] = listMailTime_Array[j];
						listMailTime_Array[j] = temp;
						temp = listMailView_Array[i];
						listMailView_Array[i] = listMailView_Array[j];
						listMailView_Array[j] = temp;
					}
				}
			
			}
		
			server_sendData = "";
			String temp="";
			if (num_inbox > 15) num_inbox=15;
			for (int i=0; i< num_inbox; i++) {
				if (listMailView_Array[i].charAt(0) != '1' && listMailView_Array[i].charAt(0) != '0') listMailView_Array[i] = listMailView_Array[i].substring(1, listMailView_Array[i].length()) ;
				temp = listMailView_Array[i] + listMailTime_Array[i];
				server_sendData += String.valueOf(temp.length()) + " " + temp;
			}
		}
		else server_sendData="Không có thư";
		return server_sendData;
	}
	
	public String Delete(String s) {
		String user="", dir="", filename="", path="";
		
		System.out.printf("\ns=\'" + s +"\'\n");
	    for (int i=0; i<= s.length()-3; i++)
	    	if (s.charAt(i) == ' ') {
	    		int len = Integer.parseInt(s.substring(1, i));
	    		System.out.print("\n" + len);
	    		s=s.substring(i+1,s.length());
	    		user=s.substring(0, len);
	    		s=s.substring(len, s.length());
	            System.out.printf("\nuser: \'" + user + "\'");
	            //System.out.printf("\ns=" + s);
	            break;
			}
	    for (int i=0; i<= s.length()-3; i++)
	    	if (s.charAt(i) == ' ') {
	    		int len = Integer.parseInt(s.substring(0, i));
	    		System.out.print("\n" + len);
	    		s=s.substring(i+1,s.length());
	    		dir=s.substring(0, len);
	    		s=s.substring(len, s.length());
	    		System.out.printf("\ndir: \'" + dir + "\'");
	    		//System.out.printf("\ns=" + s);
	    		break;
		}
	    filename = s.substring(0, s.length() - 1);
	    System.out.printf("\nfilename: \'" + filename + "\'");
	    path = "TVMail_Server_Data\\" + user + "\\" + dir + "\\" + filename;
	    System.out.printf("\n\n" + path);
	    
		File f = new File(path);
		System.out.printf("\n1OK");
		if ( !f.exists ())
			System.out.printf("\nKhong ton tai");

		     if ( !f.canWrite ())
		    	 System.out.printf("\nKhong ghi duoc");

		     // If it is a directory, make sure it is empty


		if (f.delete()) {
			System.out.printf("\n2OK");
			return "1";
		}
		//f.setWritable(true);
		System.out.printf("\nloi");
		
		return "0";
	}
	
	public String Logout(String s) {
		
		
		return "0";
	}
	/*
	public void sendData(String s, InetAddress client_ip, int client_port) {
		try {
			byte[] server_sendByte = new byte[size_of_packet];
			server_sendData = server_receivedData.toUpperCase();
			server_sendByte = s.getBytes();
			DatagramPacket server_sendPacket = new DatagramPacket(server_sendByte, server_sendByte.length,client_ip,client_port);
			server_DatagramSocket.send(server_sendPacket);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getData() {	   
		try {
				byte[] server_receivedByte = new byte[size_of_packet];	
				System.out.println("ok1");
				DatagramPacket server_receivedPacket = new DatagramPacket(server_receivedByte, server_receivedByte.length);
				System.out.println("ok2");
				server_DatagramSocket.receive(server_receivedPacket);
				System.out.println("ok3");
				String s = new String (server_receivedPacket.getData()).trim();
				System.out.println("ok4");
				return s;
				
		}
		catch (Exception e) {
			System.out.println("ok5");
			e.printStackTrace();
		}
		System.out.println("ok6");
		return "error";
	}
	*/
}
