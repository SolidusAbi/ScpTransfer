package ssh;

import java.io.File;

import Connection.ScpUser;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ScpTransfer {
	private String user = "root";
	private String host = "localhost";
	private int port = 2200;
	private String knownHostsPath = "ssh/known_hosts";
	private String identityKeyPath = "ssh/id_rsa";
	
	public void ScpTo(String fileOri, String fileDest, boolean timeStamp){
		try{
			JSch jsch=new JSch();
		
			File knownHostsFile = new File(knownHostsPath);
			if (knownHostsFile.exists() && knownHostsFile.isFile()){
				jsch.setKnownHosts(knownHostsPath);
			}
			
			File IdentityFile = new File(identityKeyPath);
			if (IdentityFile.exists() && IdentityFile.isFile()){
				jsch.addIdentity(identityKeyPath);
			}
			
			Session session = jsch.getSession(user, host, port);
			session.setUserInfo(new ScpUser());
			session.connect();
			
			if (timeStamp == true){
				
			}
		} catch (JSchException e){
			e.printStackTrace();
		}
	}
}
