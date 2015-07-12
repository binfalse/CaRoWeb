/**
 * Copyright © 2015 Martin Scharm <martin@binfalse.de>
 * 
 * This file is part of CaRoWeb.
 * 
 * CaRoWeb is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * CaRoWeb is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with CaRoWeb. If not, see <http://www.gnu.org/licenses/>.
 */
package de.unirostock.sems.caroweb;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletContext;

import de.binfalse.bflog.LOGGER;



// TODO: Auto-generated Javadoc
/**
 * The Class CaRoWebutils.
 *
 * @author Martin Scharm
 */
public class CaRoWebutils
{
	
	/** The Constant DEFAULT_BUFFER_SIZE. */
	public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
  
  /** The Constant CACHE_TIME. */
  public static final long CACHE_TIME = 60 * 60;
	
	/** The Constant downloadDateFormater. */
	public static final SimpleDateFormat downloadDateFormater = new SimpleDateFormat ("EEE, d MMM yyyy HH:mm:ss Z");	
	
	/** The storage. */
	public static Path							STORAGE		= null;
	
	/** The formatter. */
	public static SimpleDateFormat	formatter	= new SimpleDateFormat (
																							"yyyy-MM-dd-HH-mm-aaZ");
	
	
	/**
	 * Gets the current time stamp.
	 *
	 * @return the time stamp
	 */
	public static String getTimeStamp ()
	{
		return formatter.format (new Date ());
	}
	
	
	/**
	 * Gets the storage location.
	 *
	 * @param context the servlet context
	 * @return the storage location
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
	
	
	/**
	 * Clean up our storage.
	 *
	 * @param context the context
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
