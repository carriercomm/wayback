/*
 *  This file is part of the Wayback archival access software
 *   (http://archive-access.sourceforge.net/projects/wayback/).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual
 *  contributors.
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.wayback.webapp;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.wayback.util.webapp.AbstractRequestHandler;
import org.archive.wayback.util.webapp.RequestHandler;
import org.archive.wayback.util.webapp.RequestMapper;

/**
 * The LocalDispatchHandler serves local resources if exists.
 * <p>
 * This request handler is typically used for serving static resources
 * embedded in Wayback user interface, but it can also serve dynamic
 * resource (i.e. servlet) as long as the resource exists as local file
 * (ex. {@code .jsp} files).
 * It maps the path relative to {@code basePath}, and forwards to it
 * only if the path exists as a file.
 * </p>
 * <p>
 * Optionally it can delegate to other request handler if the path does
 * not exist as a file (typically delegated to
 * {@link ServerRelativeArchivalRedirect} for rectifying leaked archival URLs.)
 * <p>
 * <p>
 * This class serves the same purpose as {@link AccessPoint#dispatchLocal}, but
 * is more robust because request path is never parsed as replay requests.
 * </p>
 */
public class LocalDispatchHandler extends AbstractRequestHandler {

	private String basePath;
	private RequestHandler missingDelegate;
	private boolean mementoHeaderEnabled = true;

	public String getBasePath() {
		return basePath;
	}

	/**
	 * Set context relative local path where local resources are placed.
	 * This path is prepended to request path.
	 * {@code null} and empty string are interpreted as {@code "/"}.
	 * @param basePath context relative path
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	protected final String getPathPrefix() {
		if (basePath == null)
			return "/";
		if (basePath.startsWith("/")) {
			return basePath.endsWith("/") ? basePath : basePath + "/";
		} else {
			return "/" + (basePath.endsWith("/") ? basePath : basePath + "/");
		}
	}

	public RequestHandler getMissingDelegate() {
		return missingDelegate;
	}

	/**
	 * request handler to which requests for non-existent resource
	 * are delegated.
	 * @param missingDelegate RequestHandler
	 */
	public void setMissingDelegate(RequestHandler missingDelegate) {
		this.missingDelegate = missingDelegate;
	}

	public boolean isMementoHeaderEnabled() {
		return mementoHeaderEnabled;
	}

	/**
	 * Set {@code false} to disable Memento {@code dontnegotiate} header.
	 * @param mementoHeaderEnabled
	 */
	public void setMementoHeaderEnabled(boolean mementoHeaderEnabled) {
		this.mementoHeaderEnabled = mementoHeaderEnabled;
	}

	/* (non-Javadoc)
	 * @see org.archive.wayback.util.webapp.RequestHandler#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean handleRequest(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {
		final String prefix = getPathPrefix();
		String translatedNoQuery = RequestMapper.getRequestContextPath(httpRequest);
		String absPath = getServletContext().getRealPath(prefix + translatedNoQuery);
		// some container (notably Jetty) returns null for non-existent file
		if (absPath != null) {
			File absFile = new File(absPath);
			if (absFile.exists()) {
				String target = prefix + RequestMapper.getRequestContextPathQuery(httpRequest);
				RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(target);
				if (dispatcher != null) {
					// TODO: do we need to pass UIResults to target, as AccessPoint#dispatchLocal
					// does?
					dispatcher.forward(httpRequest, httpResponse);
					return true;
				}
			}
		}
		if (missingDelegate != null) {
			return missingDelegate.handleRequest(httpRequest, httpResponse);
		}
		return false;
	}

}
