package com.mastergaurav.gae.server;

import java.io.ByteArrayOutputStream;
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

/**
 * Taks care of "too many files" issue at GAE.
 * <br/>
 * Have a look at http://code.google.com/p/googleappengine/issues/detail?id=161#c88
 * <br/>
 *   and http://blogs.mastergaurav.com/2010/10/21/google-app-engine-dealing-with-large-number-of-files/
 */
@SuppressWarnings("serial")
public class UnzipperServlet extends HttpServlet
{
	private static final Logger LOG = Logger.getLogger(UnzipperServlet.class.getName());

	private static final String KEY_ZIP_FILE = "zipFile";
	private static final String KEY_WELCOME_FILE_LIST = "welcome-file-list";
	private String zipFileName;
	private String welcomeFileList;

	private ZipFile zipFile;

	private FileNameMap filenamemap;

	private static final int MAX_AGE = 365 * 86400;

	@Override
	public void init() throws ServletException
	{
		super.init();

		zipFileName = getInitParameter(KEY_ZIP_FILE);
		LOG.log(Level.WARNING, "Initialization, zipFileName from init = " + zipFileName);
		if(zipFileName == null || zipFileName.length() == 0)
		{
			zipFileName = "/com/mastergaurav/gwt/sc.zip";
			LOG.log(Level.WARNING, "Initialization, zipFileName was null, hardcoded to: " + zipFileName);
		}

		welcomeFileList = getInitParameter(KEY_WELCOME_FILE_LIST);
		LOG.log(Level.WARNING, "Initialization, welcomeFileList = " + welcomeFileList);
		if(welcomeFileList == null || welcomeFileList.length() == 0)
		{
			welcomeFileList = "index.html";
			LOG.log(Level.WARNING, "Initialization, welcomeFileList was null, hardcoded to: " + welcomeFileList);
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
		if(entryName.length() == 0)
		{
			entryName = welcomeFileList;
		}

		LOG.log(Level.WARNING, "Unzipping: " + entryName);
		ZipEntry entry = zipFile.getEntry(entryName);
		SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		resp.setHeader("X-GVaish-Current-Date", fmt.format(new Date()));

		if(entry == null)
		{
			LOG.log(Level.WARNING, "Not serving the file... NOT found 404");
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		long entryTime = entry.getTime();
		if(req.getHeader("If-Modified-Since") != null)
		{
			String rawDate = req.getHeader("If-Modified-Since");
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

		String md5Tag = null;

		String md5File = entryName + ".md5";
		ZipEntry md5Entry = zipFile.getEntry(md5File);
		if(md5Entry != null && md5Entry.getSize() >= 32)
		{
			InputStream md5Input = zipFile.getInputStream(md5Entry);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int c;
			while((c = md5Input.read()) >= 0)
			{
				if(c == ' ')
				{
					break;
				}
				baos.write(c);
			}
			md5Input.close();
			byte[] md5Raw = baos.toByteArray();
			md5Tag = new String(md5Raw);
		}
		LOG.log(Level.WARNING, "MD5 Tag = " + md5Tag);

		String etag = req.getHeader("If-None-Match");
		LOG.log(Level.WARNING, "If-None-Match Header: " + etag);
		if(etag != null)
		{
			etag = etag.trim();
			LOG.log(Level.WARNING, "If-None-Match Header (after trim): " + etag);
			if(etag.charAt(0) == '"')
			{
				etag = etag.substring(1, etag.length());
			}
			LOG.log(Level.WARNING, "If-None-Match Header (after substring): " + etag);
			if(etag.equals(md5Tag))
			{
				LOG.log(Level.WARNING, "If-None-Match Header MATCHES the md5Tag... will do a 304");
				resp.setHeader("ETag", md5Tag);
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			} else
			{
				LOG.log(Level.WARNING, "If-None-Match Header does not match the md5Tag");
			}
		}

		LOG.log(Level.WARNING, "Serving the file...");
		String ctype = filenamemap.getContentTypeFor(entryName);
		InputStream input = zipFile.getInputStream(entry);

		resp.setContentType(ctype);
		resp.setHeader("Cache-Control", "public; max-age=" + MAX_AGE);

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, MAX_AGE);
		resp.setHeader("Expires", fmt.format(cal.getTime()));

		resp.setHeader("Last-Modified", fmt.format(new Date(entryTime)));

		if(md5Tag != null && md5Tag.length() > 0)
		{
			resp.setHeader("ETag", md5Tag);
		}
		resp.setHeader("Content-Length", String.valueOf(entry.getSize()));

		OutputStream output = resp.getOutputStream();
		int i;
		while((i = input.read()) >= 0)
		{
			output.write(i);
		}
		input.close();
	}
}
