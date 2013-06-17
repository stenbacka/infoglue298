/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 */

package org.infoglue.cms.controllers.kernel.impl.simple;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.JDO;
import org.exolab.castor.jdo.PersistenceException;
import org.infoglue.cms.exception.SystemException;

public class CastorDatabaseService //extends DatabaseService
{
	public final static Logger logger = Logger.getLogger(CastorDatabaseService.class.getName());

	private static JDO jdo = null;
	private static boolean block = false;
	private static ThreadLocal<Database> threadDatabase = new ThreadLocal<Database>();

	public synchronized static JDO getJDO() throws SystemException
	{
		if(jdo != null)
		    return jdo;

		try
		{
			jdo = new JDO();
			jdo.setDatabaseName("INFOGLUE_CMS"); 

			//DatabaseDefinitionsController.getController().getCastorDatabaseDefinitionFile("default");
			//jdo.setConfiguration(CastorDatabaseService.class.getResource("/currentDatabase.xml").toString());
			jdo.setConfiguration(CastorDatabaseService.class.getResource("/database.xml").toString());
			jdo.setClassLoader(CastorDatabaseService.class.getClassLoader());
			jdo.setCallbackInterceptor(new CmsJDOCallback());
		}
		catch(Exception e)
		{
			throw new SystemException("An error occurred while trying to get a JDO object. Castor message:" + e, e);
		}

		return jdo;
	}

	public synchronized static Database getDatabase() throws SystemException
	{
		try
		{
			logger.info("Getting new databaseobject....");
			return getJDO().getDatabase();
		}
		catch(Exception e)
		{
			throw new SystemException("An error occurred while trying to get a Database object. Castor message:" + e, e);
		}
	}

	public static synchronized void setBlock(boolean block)
	{
		CastorDatabaseService.block = block;
	}

	/**
	 * <p>Gets a database instance that can be access by any method called within the current thread.
	 * The database instance is retrieved using the normal {@link #getDatabase()} method. On concurrent
	 * calls to the method the same instance will be returned.</p>
	 *
	 * <p>The usage pattern for this method as opposed to <em>getDatabase()</em> is slightly different.
	 * The developer should know beforehand which method will do the first call to this method. That method
	 * is responsible for the transaction's commit/rollback just as with the <em>getDatabase()</em> pattern
	 * but consecutive calls to the method should not commit nor rollback the Database instance. If an exception occurs
	 * it should be thrown so that the first caller of <em>getThreadDatabase()</em> can catch and handle it.</p>
	 * 
	 * <p>The first caller of this method in the thread have to call {@link #clearThreadDatabase()} after the
	 * transaction is complete. The {@link BaseController#commitThreadTransaction(Database)} and 
	 * {@link BaseController#rollbackThreadTransaction(Database)} is recommended to do the clearing.</p>
	 *
	 * @throws SystemException If {@link #getDatabase()} throw an exception
	 */
	public synchronized static Database getThreadDatabase() throws SystemException
	{
		Database db = threadDatabase.get();
		if (db == null)
		{
			db = getDatabase();
			try
			{
				db.begin();
				threadDatabase.set(db);
			}
			catch (PersistenceException ex)
			{
				throw new SystemException("Failed to begin Database transaction", ex);
			}
		}
		else if(logger.isInfoEnabled())
		{
			logger.info("Database was already defined, returning instance for thread: " + Thread.currentThread().getId());
		}
		return db;
	}

	/**
	 * Gets the current thread's database object and tries to commit the transaction. If the current thread
	 * doesn't have a database object nothing is done.
	 */
	public synchronized static void commitThreadDatabase()
	{
		Database db = threadDatabase.get();

		if(db == null)
		{
			/*
			 * An exception is thrown and caught here in order to get the stack trace.
			 */
			try
			{
				throw new IllegalStateException();
			}
			catch (IllegalStateException ex)
			{
				logger.warn("Tried to commit a thread database but there was no database for the thread. This is not an error but under normal circumstances this should not happen.", ex);
			}
			return;
		}

		if(logger.isInfoEnabled() && db != null)
		{
			logger.info("Commit thread database for thread: " + Thread.currentThread().getId());
		}
		try
		{
			if (db.isActive())
			{
				db.commit();
				db.close();
			}
		}
		catch(Exception ex)
		{
			logger.warn("An error occurred when we tried to commit a thread transaction. Reason: " + ex.getMessage());
		}
		threadDatabase.set(null);
	}

	/**
	 * Gets the current thread's database object and tries to rollback the transaction. If the current thread
	 * doesn't have a database object nothing is done.
	 */
	public synchronized static void rollbackThreadDatabase()
	{
		Database db = threadDatabase.get();

		if(db == null)
		{
			/*
			 * An exception is thrown and caught here in order to get the stack trace.
			 */
			try
			{
				throw new IllegalStateException();
			}
			catch (IllegalStateException ex)
			{
				logger.warn("Tried to rollback a thread database but there was no database for the thread. This is not an error but under normal circumstances this should not happen.", ex);
			}
			return;
		}

		if(logger.isInfoEnabled() && db != null)
		{
			logger.info("Rollback thread database for thread: " + Thread.currentThread().getId());
		}
		try
		{
			if (db.isActive())
			{
				db.rollback();
				db.close();
			}
		}
		catch(Exception ex)
		{
			logger.warn("An error occurred when we tried to rollback a thread transaction. Reason: " + ex.getMessage());
		}
		threadDatabase.set(null);
	}
}