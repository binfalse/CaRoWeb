/**
 * 
 */
package de.unirostock.sems.caroweb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;
import de.binfalse.bfutils.GeneralTools;
import de.unirostock.sems.caro.CaRoConverter;
import de.unirostock.sems.caro.CaRoNotification;
import de.unirostock.sems.caro.converters.CaToRo;
import de.unirostock.sems.caro.converters.RoToCa;



/**
 * @author Martin Scharm
 * 
 */
@MultipartConfig
public class Converter
	extends HttpServlet
{
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5459458067603941235L;


	private void run (HttpServletRequest request, HttpServletResponse response)
		throws ServletException,
			IOException
	{
		
		new Thread (new Runnable ()
		{
			
			@Override
			public void run ()
			{
				try
				{
					CaRoWebutils.cleanStorage (getServletContext ());
				}
				catch (IOException e)
				{
					LOGGER.error (e, "could not clean up..");
				}
			}
		}).start ();
		
		Path STORAGE = CaRoWebutils.getStorage (getServletContext ());
		
		String[] req = request.getRequestURI ()
			.substring (request.getContextPath ().length ()).split ("/");
		
		if (req == null || req.length < 2)
		{
			error (request, response, "do not know what to do");
			return;
		}
		
		if (req.length > 2 && req[1].equals ("checkout"))
		{
			checkout (request, response, STORAGE, req[2]);
			return;
		}
		
		if (!req[1].equals ("caro") && !req[1].equals ("roca"))
		{
			error (request, response, "do not know what to do");
			return;
		}
		
		File tmp = File.createTempFile ("conatiner", "upload");
		File out = File.createTempFile ("conatiner", "converted");
		out.delete ();
		// System.out.println (Arrays.toString (req));
		Part filePart = request.getPart ("container");
		if (filePart == null)
		{
			error (request, response, "no file supplied");
			return;
		}
		String uploadedName = extractFileName (filePart);
		if (uploadedName == null)
			uploadedName = "container";
		uploadedName.replaceAll ("[^A-Za-z0-9 ]", "_");
		if (uploadedName.length () < 3)
			uploadedName += "container";
		
		filePart.write (tmp.getAbsolutePath ());
		CaRoConverter conv = null;
		
		if (req[1].equals ("caro"))
			conv = new CaToRo (tmp);
		else if (req[1].equals ("roca"))
			conv = new RoToCa (tmp);
		else
		{
			error (request, response, "do not know what to do");
			return;
		}
		conv.convertTo (out);
		
		List<CaRoNotification> notifications = conv.getNotifications ();
		
		Path result = null;
		if (out.exists ())
		{
			result = Files.createTempFile (STORAGE, uploadedName, "-converted-"
				+ CaRoWebutils.getTimeStamp () + "."
				+ (req[1].equals ("caro") ? "ro" : "omex"));
			try
			{
				Files.copy (out.toPath (), result, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (Exception e)
			{
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_ERROR,
					"wasn't able to copy converted container to storage"));
			}
		}
		
		JSONArray errors = new JSONArray ();
		JSONArray warnings = new JSONArray ();
		JSONArray notes = new JSONArray ();
		for (CaRoNotification note : notifications)
			if (note.getSeverity () == CaRoNotification.SERVERITY_ERROR)
				errors.add (note.getMessage ());
			else if (note.getSeverity () == CaRoNotification.SERVERITY_WARN)
				warnings.add (note.getMessage ());
			else if (note.getSeverity () == CaRoNotification.SERVERITY_NOTE)
				notes.add (note.getMessage ());
		
		JSONObject json = new JSONObject ();
		json.put ("errors", errors);
		json.put ("warnings", warnings);
		json.put ("notifications", notes);
		
		if (result != null && Files.exists (result))
			json.put ("checkout", result.getFileName ().toString ());

		response.setContentType ("application/json");
		response.setCharacterEncoding ("UTF-8");
		PrintWriter outWriter = response.getWriter ();
		outWriter.print (json);
		out.delete ();
	}

	protected static final String extractFileName (Part part)
	{
		String header = part.getHeader ("content-disposition");
		if (header == null)
			header = part.getHeader ("Content-Disposition");
		if (header == null)
			header = part.getHeader ("CONTENT-DISPOSITION");
		if (header == null)
		{
			LOGGER.error ("cannot find CONTENT-DISPOSITION");
		}
		
		String[] items = part.getHeader ("content-disposition").split (";");
		for (String s : items)
			if (s.trim ().startsWith ("filename"))
				return s.substring (s.indexOf ("=") + 2, s.length () - 1);
		return "container";
	}
	
	private void checkout (HttpServletRequest request,
		HttpServletResponse response, Path storage, String req)
		throws ServletException,
			IOException
	{
		Path target = storage.resolve (req).toAbsolutePath ().normalize ();
		if (!target.startsWith (storage) || !Files.exists (target))
		{
			error (request, response, "you're not allowed to download that file.");
			return;
		}
		
		try
		{
			String mime = 
				target.toString ().endsWith ("omex") ?
					"application/zip" : "application/vnd.wf4ever.robundle+zip";
			
			response.reset ();
			response.setBufferSize (CaRoWebutils.DEFAULT_BUFFER_SIZE);
			response.setContentType (mime);
			response.setHeader ("Content-Length", String.valueOf (target.toFile ().length ()));
			response.setHeader ("Content-Disposition", "attachment; filename=\""
				+ req + "\"");
			response.setHeader (
				"Expires",
				CaRoWebutils.downloadDateFormater.format (new Date (System
					.currentTimeMillis () + CaRoWebutils.CACHE_TIME * 1000)));
			response
				.setHeader ("Cache-Control", "max-age=" + CaRoWebutils.CACHE_TIME);
			response.setHeader ("Last-Modified",
				CaRoWebutils.downloadDateFormater.format (new Date (Files.getLastModifiedTime (target).toMillis ())));
			response.setHeader ("ETag",
				GeneralTools.hash (target + "-" + Files.getLastModifiedTime (target)));
			
			BufferedInputStream input = new BufferedInputStream (new FileInputStream (
				target.toFile ()), CaRoWebutils.DEFAULT_BUFFER_SIZE);
			BufferedOutputStream output = new BufferedOutputStream (
				response.getOutputStream (), CaRoWebutils.DEFAULT_BUFFER_SIZE);
			
			// pass the stream to client
			byte[] buffer = new byte [CaRoWebutils.DEFAULT_BUFFER_SIZE];
			int length;
			while ( (length = input.read (buffer)) > 0)
			{
				output.write (buffer, 0, length);
			}
			
			input.close ();
			output.close ();
			
			return;
		}
		catch (IOException e)
		{
			// whoops, that's our fault. shouldn't happen. hopefully.
			LOGGER.error ("unable to dump file " + target + " (at least not in an expected form)");
		}
		error (request, response, "couldn't dump file");
	}
	
	
	private void error (HttpServletRequest request, HttpServletResponse response,
		String message) throws ServletException, IOException
	{
		request.setAttribute ("error", message);
		Index.run (request, response);
	}
	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet (HttpServletRequest request, HttpServletResponse response)
		throws ServletException,
			IOException
	{
		run (request, response);
	}
	
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost (HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException
	{
		run (request, response);
	}
	
}
