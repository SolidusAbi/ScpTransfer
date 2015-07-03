package ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

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
	
	public boolean scpTo(String fileOri, String fileDest, boolean timeStamp){
		FileInputStream fis=null;
		try{
			JSch jsch=new JSch();
		
			addKnownHosts(jsch);
			addIdentityFile(jsch);
			
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
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return true;
	}
	
	public boolean scpFrom(String remoteFile, String localFile, boolean timeStamp){
		FileOutputStream fos = null;
		
		String prefix=null;
	    if(new File(localFile).isDirectory())
	    	prefix=localFile+File.separator;
	    
	    JSch jsch=new JSch();
		
	    try {
			addKnownHosts(jsch);
			addIdentityFile(jsch);
			
			Session session = jsch.getSession(user, host, port);
			session.setUserInfo(new ScpUser());
			session.connect();
			
			// exec 'scp -f remoteFile' remotely
		    Channel channel=session.openChannel("exec");
		    execCommand("scp -f "+remoteFile, channel);
		    
		    // get I/O streams for remote scp
		    OutputStream out = channel.getOutputStream();
		    InputStream in = channel.getInputStream();
		    
		    channel.connect();
		    
		    byte[] buf=new byte[1024];

		    // send '\0'
		    buf[0]=0; out.write(buf, 0, 1); out.flush();

		    while(true){
		    	int c = checkAck(in);
		    	if (c!='C') //'C' is 67 in ASCII code
		    		break;
		    	
		    	//read '0644'
		    	in.read(buf, 0, 5);
		    	
		    	long fileSize=0L; //Seria bueno echarle un segundo ojo a todo esto... xk no termino de cogerlo
		    	while (true){
		    		if (in.read(buf, 0, 1)<0){
		    			//error
		    			break;
		    		}
		    		if(buf[0]==' ') break;
		    		fileSize=fileSize*10L+(long)(buf[0]-'0');
		    	}
		    	
		    	String file=null;
		    	for(int i=0;;i++){
		    		in.read(buf, i, 1);
		    		if(buf[i]==(byte)0x0a){ //Hace referencia al estado del socket?, http://www.dialogic.com/webhelp/CSP1010/8.4.1_IPN3/exsapi_quickref_tlv_-_0x0asocket_status_.htm
		    			file=new String(buf, 0, i);
		    			break;
		    		}
		    	}
		    	
		    	System.out.println("filesize="+fileSize+", file="+file);
		    	
		    	// send '\0'
			    buf[0]=0; out.write(buf, 0, 1); out.flush();
		    	
			    // read a content of localFile
		        fos=new FileOutputStream(prefix==null ? localFile : prefix+file);
			    int foo;
			    while(true){
			    	if(buf.length<fileSize)
			    		foo=buf.length;
			    	else
			    		foo=(int)fileSize;
			    	
			    	foo=in.read(buf, 0, foo);
			    	if (foo<0){
			    		//error
			    		break;
			    	}
			    	fos.write(buf,0,foo);
			    	fileSize-=foo;
			    	if(fileSize==0L) break;
			    }
		        
			    fos.close();
			    fos=null;
			    
			    if(checkAck(in)!=0){
			    	System.exit(0);
			    }
			    
			    // send '\0'
			    buf[0]=0; out.write(buf, 0, 1); out.flush();
		    }
		    
		    session.disconnect();
		} catch (JSchException | IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		return true;
	}
	
	private void addKnownHosts(JSch jsch) throws JSchException{
		File knownHostsFile = new File(knownHostsPath);
		if (knownHostsFile.exists() && knownHostsFile.isFile()){
			jsch.setKnownHosts(knownHostsPath);
		}
	}
	
	private void addIdentityFile(JSch jsch) throws JSchException{
		File IdentityFile = new File(identityKeyPath);
		if (IdentityFile.exists() && IdentityFile.isFile()){
			jsch.addIdentity(identityKeyPath);
		}
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
	
	private void execCommand(String command, Channel channel){
		((ChannelExec)channel).setCommand(command);
	}
}
