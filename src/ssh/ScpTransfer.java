package ssh;

import java.io.File;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class ScpTransfer {
	private String user;
	private String host;
	
	public void ScpTo(String fileOri, String fileDest){
		try{
			JSch jsch=new JSch();
		
			File knownHostsFile = new File("ssh/known_hosts");
			if (knownHostsFile.exists() && knownHostsFile.isFile()){
				jsch.setKnownHosts("ssh/known_hosts");
			}
		} catch (JSchException e){
			e.printStackTrace();
		}
	}
}
