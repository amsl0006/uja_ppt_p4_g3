package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import ujaen.git.ppt.mail.Mail;
import ujaen.git.ppt.mail.Mailbox;
import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;


public class Connection implements Runnable, RFC5322 {

	public static final int S_NOCOMMAND = -1;
	public static final int S_HELO = 0;
	public static final int S_EHLO = 1;
	public static final int S_MAIL_FROM = 2;
	public static final int S_RCPT_TO = 3;
	public static final int S_DATA = 4;
	public static final int S_RESET = 5;
	public static final int S_QUIT = 6;
	
	
	protected Socket mSocket;
	protected int mEstado = S_HELO;
	private boolean mFin = false;
	protected String mArguments="";
	protected boolean firstExecute=false;
	protected boolean pHELO=false;
	protected String HELOArguments="";
	protected boolean pRCPT_TO=false;
	protected String mFrom="";
	protected String mTo="";
	protected boolean pMAIL_FROM=false;
	
	
	public static String MSG_WELCOME = "OK Bienvenido al servidor de pruebas\r\n"; //de lo que esta subido en github
	
	
	
	public Connection(Socket s) {
		mSocket = s;
		mEstado = 0;
		mFin = false;
	}

	@Override
	public void run() {

		String inputData = null;
		String outputData = "";
		//int estado=0; lo que tiene puesto en el que subio a github
		SimpleDateFormat formatofecha= new SimpleDateFormat ("EEEE, Z HH:mm:ss d MMMM yyyy");
		Date fechaactual= new Date();
		
		
		if (mSocket != null) {
			try {
				//para identificar el mensaje de manera unica (message-id)
				File fIdentificador = new File("Identificador.txt");
				//comprobamos si existe el archivo y si no creamos el archivo por si no exisitiera
				if(!fIdentificador.exists() && !fIdentificador.isFile()){
					PrintWriter escribir= new PrintWriter (fIdentificador, "UTF-8");
				escribir.println("0");
				escribir.close();
				}
				
				
				
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				
				//output.write(MSG_WELCOME.getBytes()); del que subio a github
				
				// Envío del mensaje de bienvenida
				String respuesta = RFC5321.getReply(RFC5321.R_220) + SP + RFC5321.MSG_WELCOME
						+ RFC5322.CRLF;
				output.write(respuesta.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null)) {
					
					System.out.println("Servidor [Recibido]> " + inputData);
					//String campos[]= inputData.spllit("");
					
					//tendramos que hacer una clase que sea RFC5621 para que realize esta funcion.
					//SMTPMessage comando = RFC5621.analizaComando(inputData);//de lo que tiene subido el github
					
					// Todo análisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					//Mail ma = new Mail (inputData);
					if(mEstado != S_DATA && m.getCommandId()==S_DATA){
						firstExecute=true;
					}
					
					if(mEstado!=S_DATA){
						mEstado=m.getCommandId();
						mArguments=m.getArguments();
					}
					
					
					//if (m.getCommandId() != -1){
					// TODO: Máquina de estados del protocolo
						switch (mEstado) {
						case S_HELO:
						
							if (!pHELO) {
								HELOArguments=mArguments;
								
								
							}
						
							break;
						case S_EHLO:
							
							if (!pHELO) {
								HELOArguments=mArguments;
							}
							break;
							
						case S_MAIL_FROM:
							if (pHELO && !pRCPT_TO) {
								mFrom=mArguments;
							}
							break;
							
						case S_RCPT_TO:
							if (pHELO && pMAIL_FROM) {
								//primero comprobamos si existe el usuario en el mail box 
								if(Mailbox.checkRecipient(mArguments)){
									mTo=mArguments;
									pRCPT_TO=true;
							}else{
								pRCPT_TO=false;
							}
							}
							break;
						
						case S_RESET:
							
							break;
							
						case S_QUIT:
							
							break;
						
					//hay que seguir va?		
						}
					}else

					// TODO montar la respuesta
					// El servidor responde con lo recibido
					outputData = RFC5321.getReply(RFC5321.R_220) + SP + inputData + CRLF;
					output.write(outputData.getBytes());
					output.flush();

				}
				System.out.println("Servidor [Conexión finalizada]> " + mSocket.getInetAddress().toString() + ":" + mSocket.getPort());

				input.close();
				output.close();
				mSocket.close();
			} catch (SocketException se) {
				se.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}
