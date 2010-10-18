/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.ide.filesystem.s3;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.ListAllMyBucketsResponse;
import com.aptana.core.epl.IMemento;
import com.aptana.core.util.StringUtil;
import com.aptana.ide.core.io.ConnectionPoint;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IBaseRemoteConnectionPoint;

/**
 * @author Max Stepanov
 * @author cwilliams
 */
public class S3ConnectionPoint extends ConnectionPoint implements IBaseRemoteConnectionPoint
{

	public static final String DEFAULT_HOST = "s3.amazonaws.com"; //$NON-NLS-1$

	public static final String TYPE = "s3"; //$NON-NLS-1$

	private static final String ELEMENT_HOST = "host"; //$NON-NLS-1$
	private static final String ELEMENT_PATH = "path"; //$NON-NLS-1$
	private static final String ELEMENT_ACCESS_KEY = "accessKey"; //$NON-NLS-1$

	private IPath path = Path.ROOT;
	private String accessKey = StringUtil.EMPTY;
	private char[] password;
	private String host = DEFAULT_HOST;

	private boolean isConnected;

	/**
	 * Default constructor
	 */
	public S3ConnectionPoint()
	{
		super(TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#loadState(com.aptana.ide.core.io.epl.IMemento)
	 */
	@Override
	protected void loadState(IMemento memento)
	{
		super.loadState(memento);
		IMemento child = memento.getChild(ELEMENT_HOST);
		if (child != null)
			host = child.getTextData();
		else
			host = DEFAULT_HOST;

		child = memento.getChild(ELEMENT_PATH);
		if (child != null)
		{
			if (child.getTextData() == null)
				path = Path.ROOT;
			else
				path = Path.fromPortableString(child.getTextData());
		}
		child = memento.getChild(ELEMENT_ACCESS_KEY);
		if (child != null)
		{
			accessKey = child.getTextData();
		}
		if (CoreIOPlugin.getAuthenticationManager().hasPersistent(getAccessKey()))
			password = CoreIOPlugin.getAuthenticationManager().getPassword(getAccessKey());
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#saveState(com.aptana.ide.core.io.epl.IMemento)
	 */
	@Override
	protected void saveState(IMemento memento)
	{
		super.saveState(memento);
		memento.createChild(ELEMENT_HOST).putTextData(getHost());
		if (!Path.ROOT.equals(path))
		{
			memento.createChild(ELEMENT_PATH).putTextData(path.toPortableString());
		}
		if (getAccessKey().length() != 0)
		{
			memento.createChild(ELEMENT_ACCESS_KEY).putTextData(getAccessKey());
		}
		CoreIOPlugin.getAuthenticationManager().setPassword(getAccessKey(), password, true);
	}

	public String getAccessKey()
	{
		return accessKey;
	}

	public void setAccessKey(String accessKey)
	{
		this.accessKey = accessKey;
		notifyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#getLogin()
	 */
	public String getLogin()
	{
		return getAccessKey();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#setLogin(java.lang.String)
	 */
	public void setLogin(String login)
	{
		setAccessKey(login);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.IBaseRemoteConnectionPoint#getPath()
	 */
	public IPath getPath()
	{
		return path;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.IBaseRemoteConnectionPoint#setPath(org.eclipse.core.runtime.IPath)
	 */
	public void setPath(IPath path)
	{
		this.path = path;
		notifyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.IBaseRemoteConnectionPoint#getPassword()
	 */
	public char[] getPassword()
	{
		return password;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.ftp.IBaseRemoteConnectionPoint#setPassword(char[])
	 */
	public void setPassword(char[] password)
	{
		this.password = password;
		String authId = Policy.generateAuthId(TYPE, this);
		CoreIOPlugin.getAuthenticationManager().setPassword(authId, password, false);
		notifyChanged();
	}

	@Override
	public URI getRootURI()
	{
		try
		{
			String userInfo = getAccessKey();
			return new URI(TYPE, userInfo, getHost(), getPort(), getPath().toString(), (String) null, (String) null);
		}
		catch (URISyntaxException e)
		{
			S3FileSystemPlugin.log(e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#getHost()
	 */
	public String getHost()
	{
		return host;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#getPort()
	 */
	public int getPort()
	{
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#setHost(java.lang.String)
	 */
	public void setHost(String host)
	{
		this.host = host;
		notifyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.IBaseRemoteConnectionPoint#setPort(int)
	 */
	public void setPort(int port)
	{
		// do nothing, no port
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#connect(boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void connect(boolean force, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			AWSAuthConnection connection = ((S3FileStore) getRoot()).getAWSConnection();
			ListAllMyBucketsResponse resp = connection.listAllMyBuckets(null);
			if (resp == null || resp.entries == null)
			{
				throw new CoreException(new Status(IStatus.ERROR, S3FileSystemPlugin.PLUGIN_ID,
						"Failed to connect. Invalid credentials?"));
			}
			isConnected = true;
		}
		catch (CoreException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw S3FileSystemPlugin.coreException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#disconnect(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void disconnect(IProgressMonitor monitor) throws CoreException
	{
		// do nothing
		isConnected = false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#isConnected()
	 */
	@Override
	public boolean isConnected()
	{
		return isConnected;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ide.core.io.ConnectionPoint#canDisconnect()
	 */
	@Override
	public boolean canDisconnect()
	{
		return isConnected();
	}

}
