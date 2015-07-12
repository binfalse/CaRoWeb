/**
 * 
 */
package de.unirostock.sems.caroweb;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletContext;

import de.binfalse.bflog.LOGGER;



/**
 * @author Martin Scharm
 * 
 */
public class CaRoWebutils
{
	public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
  public static final long CACHE_TIME = 60 * 60;
	public static final SimpleDateFormat downloadDateFormater = new SimpleDateFormat ("EEE, d MMM yyyy HH:mm:ss Z");	
	
	public static Path							STORAGE		= null;
	public static SimpleDateFormat	formatter	= new SimpleDateFormat (
																							"yyyy-MM-dd-HH-mm-aaZ");
	
	
	public static String getTimeStamp ()
	{
		return formatter.format (new Date ());
	}
	
	
	public static Path getStorage (ServletContext context) throws IOException
	{
		if (STORAGE != null)
			return STORAGE;
		String storage = context.getInitParameter ("STORAGE");
		if (storage == null)
			storage = "/tmp/CaRoWebStorage";
		
		STORAGE = Paths.get (storage);
		Files.createDirectories (STORAGE);
		return STORAGE;
	}
	
	
	public static void cleanStorage (ServletContext context) throws IOException
	{
		Path storage = getStorage (context);
		// max 1h
		long oldestMillies = System.currentTimeMillis () - 1000 * CACHE_TIME;
		
		DirectoryStream<Path> stream = Files.newDirectoryStream (storage);
		for (Path path : stream)
		{
			try
			{
				if (!Files.isDirectory (path))
					continue;
				FileTime time = Files.getLastModifiedTime (path);
				if (time.toMillis () < oldestMillies)
					Files.delete (path);
			}
			catch (IOException e)
			{
				LOGGER.error (e, "wasn't able to clean ", path);
			}
		}
	}
}
