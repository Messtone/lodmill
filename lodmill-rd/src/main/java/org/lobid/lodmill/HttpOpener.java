package org.lobid.lodmill;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.ObjectReceiver;
import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;
import org.culturegraph.mf.stream.source.Opener;

/**
 * Extension of org.culturegraph.mf.stream.source.HttpOpener. Opens a
 * {@link URLConnection} and passes a reader to the receiver.
 * 
 * @author Christoph Böhme
 * @author Jan Schnasse
 * 
 */
@Description("Opens a http resource.")
@In(String.class)
@Out(java.io.Reader.class)
public final class HttpOpener extends
		DefaultObjectPipe<String, ObjectReceiver<Reader>> implements Opener {

	private String defaultEncoding = "UTF-8";
	private String defaultAccept = "text/plain";

	/**
	 * Returns the default encoding used when no encoding is provided by the
	 * server. The default setting is UTF-8.
	 * 
	 * @return current default setting
	 */
	public String getDefaultEncoding() {
		return defaultEncoding;
	}

	/**
	 * Sets the default accept to use when no accept is provided by the server.
	 * The default setting is text/plain.
	 * 
	 * @param defaultAccept new default encoding
	 */
	public void setDefaultAccept(final String defaultAccept) {
		this.defaultAccept = defaultAccept;
	}

	/**
	 * Returns the default accept used when no accept is provided by the server.
	 * The default setting is text/plain.
	 * 
	 * @return current default setting
	 */
	public String getDefaultAccept() {
		return defaultAccept;
	}

	/**
	 * Sets the default encoding to use when no encoding is provided by the
	 * server. The default setting is UTF-8.
	 * 
	 * @param defaultEncoding new default encoding
	 */
	public void setDefaultEncoding(final String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	@Override
	public void process(final String urlStr) {
		try {
			final URL url = new URL(urlStr);
			final URLConnection con = url.openConnection();
			con.addRequestProperty("Accept", defaultAccept);
			String enc = con.getContentEncoding();
			if (enc == null) {
				enc = defaultEncoding;
			}
			getReceiver().process(new InputStreamReader(con.getInputStream(), enc));
		} catch (IOException e) {
			throw new MetafactureException(e);
		}
	}
}
