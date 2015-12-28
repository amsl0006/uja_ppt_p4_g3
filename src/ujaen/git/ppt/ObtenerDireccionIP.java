package ujaen.git.ppt;

import java.net.Socket;

public class ObtenerDireccionIP
{
	Socket mSocket = null;
	String IP = null;
	Boolean PEjec = true;
	
	public ObtenerDireccionIP(Socket IPSocket)
	{
		mSocket = IPSocket;
	}
	
	//aqui identificamos si se trata de una direccion IPV4 o IPV6
	//esta funcion nos devuelve la direccion IP solamente.
	public String getIP()
	{
		//tomamos la direccion IP
		String IP1 = mSocket.getRemoteSocketAddress().toString();
		
		String IP2 = IP1.substring(1, IP1.length());
		
		//dividimos la IP2 en multiples cadenas
		String[] cadenas = IP2.split(":");
		
		//miramos el numero de piezas y si tratamos con una direccion IP IPV6 veremos que tiene mas de dos cadenas
		//si se cumple que tiene mas de dos cadenas eliminamos el puerto que quedaria indervible con un bucle
		if(cadenas.length > 2)
		{
			for(int i = 0; i < cadenas.length - 1; i++)
			{
				if(PEjec)
				{
					PEjec = false;
					IP = cadenas[i];
				}
				else
				{
					IP += ":" + cadenas[i];
				}
			}
			PEjec = true;
		}
		else
		{
			IP = cadenas[0];
		}
		
		return IP;
	}
}