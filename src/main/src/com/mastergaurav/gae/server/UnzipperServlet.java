package com.mastergaurav.gae.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class UnzipperServlet extends HttpServlet
{
	private static final Logger LOG = Logger.getLogger(UnzipperServlet.class.getName());

	private static final String KEY_ZIP_FILE = "zipFile";
	private String zipFileName;

	private ZipFile zipFile;

	private FileNameMap filenamemap;

	private static final int MAX_AGE = 365 * 86400;

	@Override
	public void init() throws ServletException
	{
		super.init();

		zipFileName = getInitParameter(KEY_ZIP_FILE);
		if(zipFileName == null || zipFileName.length() == 0)
		{
			zipFileName = "/com/mastergaurav/gwt/sc.zip";
		}

		URL url = UnzipperServlet.class.getResource(zipFileName);

		try
		{
			zipFile = new ZipFile(url.getFile());
		} catch(IOException e)
		{
			e.printStackTrace();
		}

		filenamemap = URLConnection.getFileNameMap();
		// URLConnection.getFileNameMap();
	}

	@Override
	public void destroy()
	{
		try
		{
			zipFile.close();
		} catch(IOException e)
		{
			e.printStackTrace();
		}
		super.destroy();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String entryName = req.getPathInfo().substring(1);
		LOG.log(Level.WARNING, "Unzipping: " + entryName);
		ZipEntry entry = zipFile.getEntry(entryName);
		SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

		if(entry == null)
		{
			LOG.log(Level.WARNING, "Not serving the file... NOT found 404");
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if(req.getHeader("If-Modified-Since") != null)
		{
			String rawDate = req.getHeader("If-Modified-Since");
			long entryTime = entry.getTime();
			LOG.log(Level.WARNING, "Found header if Modified Since: " + rawDate);
			Date headerDate;
			try
			{
				headerDate = fmt.parse(rawDate);
				long headerTime = headerDate.getTime();

				if(headerTime >= entryTime)
				{
					LOG.log(Level.WARNING, "Not serving the file... simply 304");
					resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}
			} catch(ParseException e)
			{
				e.printStackTrace();
			}
		}

		LOG.log(Level.WARNING, "Serving the file...");
		String ctype = filenamemap.getContentTypeFor(entryName);
		InputStream input = zipFile.getInputStream(entry);

		resp.setContentType(ctype);
		resp.setHeader("Cache-Control", "public; max-age=" + MAX_AGE);

		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, MAX_AGE);
		resp.setHeader("Expires", fmt.format(cal.getTime()));

		OutputStream output = resp.getOutputStream();

		int i;
		while((i = input.read()) >= 0)
		{
			output.write(i);
		}
		input.close();
	}
}

