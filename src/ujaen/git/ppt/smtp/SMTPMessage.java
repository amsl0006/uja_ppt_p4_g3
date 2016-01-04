package ujaen.git.ppt.smtp;


public class SMTPMessage implements RFC5322 {

	protected String mCommand = null;
	protected int mCommandId = RFC5321.C_NOCOMMAND;
	protected String mArguments = null;
	protected String[] mParameters = null;
	protected boolean mHasError = false;
	protected int mErrorCode = 0;

	/**
	 * The input string is processed to analyze the format of the message
	 * 
	 * @param data
	 */
	public SMTPMessage(String data) {

		if(data.length()>998)
		{
			mHasError=true;
			
		}
		else
			/*mHasError = parseCommand(data);*/
		{
			mHasError = false;
			//trata de dividir lo que se recibe mediante dos puntos (":") para encontrar comandos como RCPT_TO y MAIL_FROM.
			//si los campos tienen tama�o 0 siginifica que no se ha introducido ningun comando por lo que lo dividimos
			//mediante espacios(" ") lo que siginifica que los comandos recibidos son HELO, EHLO, DATA, RESET o QUIT
			
			String [] campos = data.split(":");
			//aqui comprobamos si se trata de un comando RCPT_TO o MAIL_FROM. 
			if(data.length() < 4)
			{
				mCommandId = checkCommand(data);
				mArguments = null;
			}
			else if(campos.length == 2)
			{
				//comprobamos si el comando es correcto, sino se guardara un -1 in mCommandId
				//y si es correcto se asociara a un codigo.
		
				
				if(campos[0].length() > 4)
				{
					mCommandId = checkCommand(campos[0]);
					mArguments = data.substring(campos[0].length() + 1, data.length());
				}
				else
				{
					mCommandId = RFC5321.C_NOCOMMAND;
					mArguments = null;
				}
			}
			// si el string de entrada (el mesaje recibido) contiene mas de dos separaciones de ":", es un mensaje no valido
			
			else if(campos.length > 2)
			{
				mCommandId = checkCommand(campos[0]);
				mArguments = null;
			}
			else
			{
				mCommandId = checkCommand(data.substring(0, 4));
				if(data.length() > 4 && (mCommandId == RFC5321.C_HELO || mCommandId == RFC5321.C_EHLO))
				{
					//si el quinto caracter no es un espacio " ", es un mensaje no valido
					if(data.substring(4,5).equalsIgnoreCase(" ") == false)
					{
						mCommandId = RFC5321.C_NOCOMMAND;
						mArguments = null;
					}
					else
					{
						mArguments = data.substring(4, data.length());
					}
				}
				else if(data.length() > 4)
				{
					mCommandId = RFC5321.C_NOCOMMAND;
					mArguments = null;
				}
			}
		}	

	}

	/**
	 * 
	 * @param data
	 * @return true if there were errors
	 */
	protected boolean parseCommand(String data) {

		if (data.indexOf(":") > 0) {
			String[] commandParts = data.split(":");// Se busca los comandos con
													// varias palabras MAIL
													// FROM:
		}

		return false;
	}

	public String toString() {
		if (!mHasError) {
			String result = "";
			result = this.mCommand;
			if (this.mCommandId == RFC5321.C_MAIL
					|| this.mCommandId == RFC5321.C_RCPT)
				result = result + ":";
			if (this.mArguments != null)
				result = result + this.mArguments;
			if (this.mParameters != null)
				for (String s : this.mParameters)
					result = result + SP + s;

			result = result + CRLF;
			//opcional
			//result=result+"id="+this.mCommandId;
			return result;
		} else
			return "Error";
	}

	/**
	 * 
	 * @param data
	 * @return The id of the SMTP command
	 */
	protected int checkCommand(String data) {
		int index = 0;

		this.mCommandId = RFC5321.C_NOCOMMAND;

		for (String c : RFC5321.SMTP_COMMANDS) {
			if (data.compareToIgnoreCase(c) == 0)
				this.mCommandId = index;

			index++;

		}

		if (mCommandId != RFC5321.C_NOCOMMAND)
			mCommand = RFC5321.getCommand(mCommandId);
		else
			mCommand = null;

		return this.mCommandId;
	}

	public String getCommand() {
		return mCommand;
	}

	public void setCommand(String mCommand) {
		this.mCommand = mCommand;
	}

	public int getCommandId() {
		return mCommandId;
	}

	public void setCommandId(int mCommandId) {
		this.mCommandId = mCommandId;
	}

	public String getArguments() {
		return mArguments;
	}

	public void setArguments(String mArguments) {
		this.mArguments = mArguments;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public void setParameters(String[] mParameters) {
		this.mParameters = mParameters;
	}

	public boolean hasError() {
		return mHasError;
	}

	
	public int getErrorCode() {
		return mErrorCode;
	}

	public void setErrorCode(int mErrorCode) {
		this.mErrorCode = mErrorCode;
	}

}
