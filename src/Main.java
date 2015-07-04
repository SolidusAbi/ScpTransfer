import ssh.ScpTransfer;

public class Main {
	public static void main(String[] arg){
		
		//My Test
		ScpTransfer scp = new ScpTransfer("root", "localhost", 2200);
		//scp.ScpTo("tmp/prueba", "pruebaDestino", true);
		scp.scpFrom("pruebaDestino", "tmp/prueba_scpto", false);	
	}
}
