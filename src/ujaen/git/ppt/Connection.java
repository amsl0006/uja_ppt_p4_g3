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

import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.mail.Mail;
import ujaen.git.ppt.smtp.SMTPMessage;
import ujaen.git.ppt.mail.Mailbox;
import ujaen.git.ppt.smtp.RFC5321;
import java.net.InetAddress;


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
	protected int Nidenteficador = 0;
	protected String nombrehost = "";
	Mail mail = null;
	Mailbox mBox = null;
	ObtenerDireccionIP obtIP = null;
	protected String direcIP = null;
	protected String crearMessID;
	protected String obtfecha;
	protected boolean enviarMail = false;
	
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
							
						case S_DATA:
							if (pHELO && pMAIL_FROM && pRCPT_TO) {
								
								if(firstExecute)
								{
									//lee el ID 
									BufferedReader leerID = new BufferedReader(new FileReader(fIdentificador));
									Nidenteficador = Integer.parseInt(leerID.readLine());
									leerID.close();
									Nidenteficador++;
									
									//se escribe el nombre en el interior de arcivo
									PrintWriter mostrar = new PrintWriter(fIdentificador, "UTF-8");
									mostrar.println(Nidenteficador);
									mostrar.close();
									
									//nombre del host
									InetAddress nombre;
								    nombre = InetAddress.getLocalHost();
								    nombrehost = nombre.getHostName();
								    
								    //obtenemos la IP
								    obtIP = new ObtenerDireccionIP(mSocket);
								    direcIP = obtIP.getIP();
								    
								    //creacion de MESSAGE-ID
								    crearMessID = "<" + Nidenteficador + "@" + nombrehost + ">";
								    
								    //obtener fecha
								    obtfecha = formatofecha.format(fechaactual);
								    mail = new Mail();
								    
								    //metemos algunos de los parametros que hemos ibtenido anteriormente.
								    mail.setHost(nombrehost);
								    mail.setMailfrom(mFrom);
								    mail.setRcptto(mTo);
								    System.out.println("Rcpt to: " + mTo);
								    
								    //por ultimo añadimos las cabeceras
								    mail.addHeader("Return-Path", mFrom);
								    mail.addHeader("Received", HELOArguments);
								    mail.addHeader("host", nombrehost);
								    mail.addHeader("IP", direcIP);
								    mail.addHeader("date", obtfecha);
								    mail.addHeader(" identificador de mensaje (Message-ID)", crearMessID);

								}
								else
								{
									mail.addMailLine(inputData);
									inputData += CRLF;
								}
								
								if(inputData.compareTo(ENDMSG) == 0)
								{
									
									enviarMail = true;
									pMAIL_FROM = false;
									pRCPT_TO = false;
									mBox = new Mailbox(mail);
									
								}
							}
							break;	
						
						case S_RESET:
							pMAIL_FROM = false;
							pRCPT_TO = false;
							break;
							
						case S_QUIT:
							mFin = true;
							break;
						
							
						}
					

					// TODO montar la respuesta
					// El servidor responde con lo recibido
						//aqui creamos otra maquina de estados para gestionar las respuestas en funcion del estado en el que
						//encontremos. Los formatos de las respuestas se definen en la clase RFC5321 y desde aqui lo que hacemos 
						//es solamente llamarlas.
						switch (mEstado)
						{
							//si se recibe un comando no valido se le notifica un error al acliente.
						case S_NOCOMMAND:
								outputData = RFC5321.getError(RFC5321.E_500_SINTAXERROR) + SP 
											 + RFC5321.getErrorMsg(RFC5321.E_500_SINTAXERROR) + CRLF;
								break;
							//respuesta del comando HELO
							case S_HELO:
								if(!pHELO)
								{
									//outputData="250 Hello" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getReply(RFC5321.R_250) + SP +
												 "Hello." + CRLF;
									pHELO = true;
								}
								else
								{
									//outputData="503 error de secuencia de comandos" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
												+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
								}
								break;
							//respuesta del comando EHLO
							case S_EHLO:
								
								if(!pHELO)
								{
									//outputData="250 Hello" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getReply(RFC5321.R_250) + SP +
												 "Hello." + CRLF;
									pHELO = true;
								}
								else
								{
									//outputData="503 error de secuencia de comandos" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
												+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
								}
								break;
								
							//respuesta del comando MAIL FROM
							case S_MAIL_FROM:
								//evaluamos pRCPT_TO solo si !pHELO es falso
								if(!pHELO || pRCPT_TO)
								{
									//outputData="503 error de secuencia de comandos" + CLRF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
												+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
								}
								else
								{
									//outputData="250 Mail From" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getReply(RFC5321.R_250) + SP
												+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
								}
								break;
								
							//respuesta del comando RCPT TO
							case S_RCPT_TO:
								if(!pHELO || !pMAIL_FROM)
								{
									//outputData="503 error de secuencia de comandos" + CLRF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
												+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
								}
								//si pasamos primero por pHELO despues por pMAIL_FROM y al final estamos en un estado distinto de pRCPT_TO
								// entonces tenemos un error de usuario no local.
								else if(pHELO && pMAIL_FROM && !pRCPT_TO)
								{
									//outputData="503 error, usuario no local" + CLRF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
												+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + CRLF;
								}
								//si pasamos primero por pHELO despues por pMAIL_FROM y al final estamos en el estado pRCPT_TO
								// entonces se cumple bien la condicion
								else if(pHELO && pMAIL_FROM && pRCPT_TO)
								{
									outputData = RFC5321.getReply(RFC5321.R_250) + SP
												+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
								}
								break;
								
							// respuesta del comando S_DATA	
							case S_DATA:
								//para llegar al caso S_DATA primerp tenemos que pasar, en este orden por,
								//pHELO, pMAIL_FROM, pRCPT_TO, firstExecute, tenemos que tener la primera ejecucion
								//para que tenga los argumentos cogidos.
								if(pHELO && pMAIL_FROM && pRCPT_TO && firstExecute)
								{
									//outputData="354 Inicie el envío del correo" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC532
									outputData = RFC5321.getReply(RFC5321.R_354) + SP
												+ RFC5321.getReplyMsg(RFC5321.R_354) + CRLF;
								}
								else if(pHELO && !pMAIL_FROM && !pRCPT_TO && enviarMail)
								{
									//outputData="250 OK" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC532
									outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ "Queued." + CRLF;
									//para que el usuario pueda continuar realizando otras cosas como volver a enviar un correo cambiamos el estado.
									enviarMail = false;
									mEstado = S_NOCOMMAND;
								}
								else if(!pMAIL_FROM || !pRCPT_TO)
								{
									mEstado = S_NOCOMMAND;
									//outputData="503 error de secuencia de comandos" + CLRF --> tambien se podria poner asi mas abreviado, pero lo hacemos
									//cogiendo las referencias que hay en la clase RFC5321
									outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP
									+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
								}
								break;
								
							// respuesta del comando RESET
							case S_RESET:
								//outputData="250 Reset" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
								//cogiendo las referencias que hay en la clase RFC5321
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
								+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
								break;
								
							//respuesta del comando QUIT
							case S_QUIT:
								//outputData="221 Quit" + CRLF --> tambien se podria poner asi mas abreviado, pero lo hacemos
								//cogiendo las referencias que hay en la clase RFC5321
								outputData = RFC5321.getReply(RFC5321.R_221) + SP + 
								RFC5321.getReplyMsg(RFC5321.R_221) + SP + 
								RFC5321.MSG_BYE + CRLF;
								break;
						}
						if(!(mEstado == S_DATA && !enviarMail) || (mEstado == S_DATA && firstExecute))
						{
							if(firstExecute)
							{
								firstExecute = false;
							}
							output.write(outputData.getBytes());
							output.flush();	
						}
						
						
					//outputData = RFC5321.getReply(RFC5321.R_220) + SP + inputData + CRLF;
					//output.write(outputData.getBytes());
					//output.flush();

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
