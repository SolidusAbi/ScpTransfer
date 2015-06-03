package Connection;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;


public class ScpUser implements UserInfo {

	String passwd;
	
	@Override
	public String getPassphrase() {
		return null;
	}

	@Override
	public String getPassword() {
		return passwd;
	}

	@Override
	public boolean promptPassphrase(String arg0) {
		return false;
	}

	@Override
	public boolean promptPassword(String message) {
		JTextField passwordField = (JTextField) new JPasswordField(20);
		Object[] ob={passwordField};
		int result=JOptionPane.showConfirmDialog(
				null, ob, message,
				JOptionPane.OK_CANCEL_OPTION);
		
		if(result==JOptionPane.OK_OPTION){
			passwd=passwordField.getText();
			return true;
		}
		else 
			return false; 
	}

	@Override
	public boolean promptYesNo(String str) {
		Object[] options={ "yes", "no" };
		int foo=JOptionPane.showOptionDialog(null,
	    		str,
	    		"Warning",
	    		JOptionPane.DEFAULT_OPTION,
	    		JOptionPane.WARNING_MESSAGE,
	    		null, options, options[0]);
	    return foo==0;
	}

	@Override
	public void showMessage(String message) {
		JOptionPane.showMessageDialog(null, message);
	}

}
