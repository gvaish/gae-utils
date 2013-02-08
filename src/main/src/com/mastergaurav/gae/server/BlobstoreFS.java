/**
 * (C) 2013, Gaurav Vaish
 * http://www.m10v.com / http://www.mastergaurav.com
 * LICENSE This code is released under Apache Public License 2.0
 */

package com.mastergaurav.gae.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

/**
 * Simple wrapper for Blobstore Filesystem
 * @author gvaish
 * 
 */
public class BlobstoreFS
{
	private static final Logger LOG = Logger.getLogger(BlobstoreFS.class.getName());

	public static BlobKey save(BlobKey key, String value) throws IOException {
		return save("text/plain; charset=utf-8", key, value);
	}

	/**
	 * Saves a file. Delete existing, if any, because in GAE Blobstore, you cannot change contents of a file.
	 * 
	 * @param mimeType Content-Type
	 * @param key Blob-key for the existing file, can be null if a new file has to be created
	 * @param value Contents of the file
	 * @param filename Name part of the file, optional
	 * @return Blob-key for the file saved
	 * @throws IOException if any other unexpected problem occurs
	 */
	public static BlobKey save(String mimeType, BlobKey key, String value, String filename) throws IOException
	{
		FileService svc = FileServiceFactory.getFileService();
		if(key != null)
		{
			delete(key);
		}
		AppEngineFile file = filename == null ? svc.createNewBlobFile(mimeType) : svc.createNewBlobFile(mimeType, filename);
		key = svc.getBlobKey(file);

		LOG.info("Created app engine file: " + file.getFullPath() + ", fs: " + file.getFileSystem().getName() + ", name: "
				+ file.getNamePart());
		LOG.info("Created app engine file, key = " + key);

		FileWriteChannel fwc = svc.openWriteChannel(file, true);

		PrintWriter writer = new PrintWriter(Channels.newWriter(fwc, "utf8"));
		writer.print(value);
		writer.close();
		fwc.closeFinally();

		key = svc.getBlobKey(file);
		LOG.info("Created app engine file, key = " + key);

		return key;
	}

	/**
	 * Deletes a file.
	 *
	 * @param key Blob-key of the file to be delete (a value returned from {@link #save(String, BlobKey, String, String)} method).
	 * @throws IOException if any other unexpected problem occurs
	 */
	public static void delete(BlobKey key) throws IOException
	{
		FileService svc = FileServiceFactory.getFileService();
		AppEngineFile file = svc.getBlobFile(key);
		if(file != null)
		{
			svc.delete(file);
		}
		BlobstoreService bsvc = BlobstoreServiceFactory.getBlobstoreService();
		bsvc.delete(key);
	}

	/**
	 * Opens the file and returns {@link InputStream} to read the contents.
	 *
	 * @param key Blob-key of the file to be delete (a value returned from {@link #save(String, BlobKey, String, String)} method).
	 * @return {@link InputStream} to read the contents.
	 * @throws IOException if any other unexpected problem occurs
	 */
	public static InputStream open(BlobKey key) throws IOException
	{
		LOG.finest("open - key: " + key);
		FileService svc = FileServiceFactory.getFileService();
		AppEngineFile file = svc.getBlobFile(key);

		FileReadChannel frc = svc.openReadChannel(file, false);
		InputStream input = Channels.newInputStream(frc);

		return input;
	}

	/**
	 * Opens the file and returns {@link Reader} to read the contents.
	 *
	 * @param key Blob-key of the file to be delete (a value returned from {@link #save(String, BlobKey, String, String)} method).
	 * @return {@link Reader} to read the contents.
	 * @throws IOException if any other unexpected problem occurs
	 */
	public static Reader openAsReader(BlobKey key) throws IOException
	{
		LOG.finest("openAsReader - key: " + key);
		FileService svc = FileServiceFactory.getFileService();
		AppEngineFile file = svc.getBlobFile(key);

		FileReadChannel frc = svc.openReadChannel(file, false);
		Reader rv = Channels.newReader(frc, "utf8");

		return rv;
	}

	/**
	 * Reads the contents till the end and returns the value.
	 * It assumes that the file is a text file. Use {@link #open(BlobKey)} to read binary content
	 *
	 * @param key Blob-key of the file to be delete (a value returned from {@link #save(String, BlobKey, String, String)} method).
	 * @return {@link Reader} to read the contents.
	 * @throws IOException if any other unexpected problem occurs
	 */
	public static String readToEnd(BlobKey key) throws IOException
	{
		StringBuilder sb;

		sb = new StringBuilder();
		Reader r = openAsReader(key);

		char[] data = new char[1024];

		try
		{
			int read = r.read(data, 0, data.length);

			while(read >= 0)
			{
				sb.append(data, 0, read);
				read = r.read(data, 0, data.length);
			}
		} finally
		{
			r.close();
		}

		return sb;
	}
}

