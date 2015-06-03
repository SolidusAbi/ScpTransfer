import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import Connection.ScpUser;


public class Main {
	public static void main(String[] arg){
		JSch jsch=new JSch();
		ScpUser user = new ScpUser();
		Session session;
		try {
			session = jsch.getSession("root", "localhost", 2200);
			session.setUserInfo(user);
			session.connect();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//user.promptPassword("-");
		
	}
}
