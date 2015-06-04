package ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ScpTransfer {
	private String user = "root";
	private String host = "localhost";
	private int port = 2200;
	private String knownHostsPath = "ssh/known_hosts";
	private String identityKeyPath = "ssh/id_rsa";
	
	public ScpTransfer(String user, String host, int port){
		this.user = user;
		this.host = host;
		this.port = port;
	}
	
	public void setUser(String user){
		this.user = user;
	}
	
	public void setHost(String host){
		this.host = host;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public boolean ScpTo(String fileOri, String fileDest, boolean timeStamp){
		FileInputStream fis=null;
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
			
			// exec 'scp -t fileDest' remotely
		    String command="scp " + (timeStamp ? "-p" :"") +" -t "+fileDest;
		    Channel channel=session.openChannel("exec");
		    ((ChannelExec)channel).setCommand(command);

		    // get I/O streams for remote scp
		    OutputStream out=channel.getOutputStream();
		    InputStream in=channel.getInputStream();

		    channel.connect();
		    
		    if (checkAck(in)!=0){
		    	return false;
		    }
			
		    File _fileOri = new File(fileOri);
		    
			if (timeStamp == true){
				//Last modified time of file
				command="T"+(_fileOri.lastModified()/1000)+" 0";
				
				//Last access time of file
				//The access time should be sent here,
		        //but it is not accessible with JavaAPI ;-<
				//BasicFileAttributes attributes = Files.readAttributes(fileOri, BasicFileAttributes.class);
				command+=(" "+(_fileOri.lastModified()/1000)+" 0\n"); 
				
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in)!=0){
					return false;
			    }
			}
			
			// send "C0644 filesize filename", where filename should not include '/'
			long filesize = _fileOri.length();
			command="C0644 "+filesize+" ";
			if(fileOri.lastIndexOf('/')>0){
				command+=fileOri.substring(fileOri.lastIndexOf('/')+1);
			}else{
				command+=fileOri;
			}
			command+="\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in)!=0){
				return false;
		    }
			
			// send a content of fileOri
			fis=new FileInputStream(fileOri);
			byte[] buf=new byte[1024];
			while(true){
				int len=fis.read(buf, 0, buf.length);
				if(len<=0) break;
				out.write(buf, 0, len);
			}
			fis.close();
			fis=null;
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
			if (checkAck(in)!=0){
				return false;
		    }
			
			out.close();
			channel.disconnect();
			session.disconnect();
		} catch (JSchException | IOException e){
			e.printStackTrace();
		}
		return true;
	}
	
	private int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if(b==0) return b;
		if(b==-1) return b;
	    
		if (b==1 || b==2){
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c=in.read();
				sb.append((char)c);
			} while (c !='\n');
			
			if (b==1){ //Error
				System.out.println(sb.toString());
			}
			if (b==2){ //Fatal Error
				System.out.println(sb.toString());
			}
		}
		return b;
	}
}
