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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.PersistenceException;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.applications.databeans.ReferenceBean;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController.DeleteSiteNodeParams;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.Repository;
import org.infoglue.cms.entities.management.RepositoryLanguage;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.entities.management.TableCount;
import org.infoglue.cms.entities.management.impl.simple.RepositoryImpl;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.sorters.ReflectionComparator;
import org.infoglue.deliver.util.CacheController;

public class RepositoryController extends BaseController
{
	private final static Logger logger = Logger.getLogger(RepositoryController.class.getName());

	/**
	 * Factory method
	 */

	public static RepositoryController getController()
	{
		return new RepositoryController();
	}

	public RepositoryVO create(RepositoryVO vo) throws ConstraintException, SystemException
	{
		Repository ent = new RepositoryImpl();
		ent.setValueObject(vo);
		ent = (Repository) createEntity(ent);
		return ent.getValueObject();
	}
	
//	private void x(LinkedList<List<Integer>> batches, ContentVO content, Database db) throws PersistenceException, SystemException, Bug
//	{
//		List<Content> children = ContentController.getContentController().getContentChildrenForContent(content.getContentId(), db);
//		
//		for (Content c : children)
//		{
//			x(batches, c.getValueObject(), db);
//		}
//		
//		List<Integer> batch = batches.getFirst();
//		if (batch.size() > 50)
//		{
//			batch = new ArrayList<Integer>();
//			batches.addFirst(batch);
//		}
//		batch.add(content.getContentId());
//	}
	
	private Integer getTotalNumberSiteNodes(Integer repositoryId)
	{
		try
		{
			String tableName = "cmSiteNode";
			if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
			{
				tableName = "cmSiNo";
			}
			TableCount tableCount = BaseController.getTableCount(tableName);
			if (tableCount != null)
			{
				return tableCount.getCount();
			}
		}
		catch (Exception ex)
		{
			logger.warn("Failed to compute number of site nodes in repository. Message: " + ex.getMessage());
		}
		return null;
	}

	/**
	 * This method removes a Repository from the system and also cleans out all depending repositoryLanguages.
	 * The method will return before the repository is deleted. The process of the deletion will be reported in the
	 * provided <em>processBean</em>.
	 */
	public void delete(final RepositoryVO repositoryVO, final String userName, final boolean forceDelete, final InfoGluePrincipal infoGluePrincipal, final boolean skipServiceBindings, final ProcessBean processBean) throws ConstraintException, SystemException
	{
		new Thread()
		{
			public void run()
			{
				try
				{
					processBean.setStatus(ProcessBean.RUNNING);
					processBean.updateProcess("tool.managementtool.repository.delete.process.init");
					Database db = CastorDatabaseService.getThreadDatabase();
					ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

					Repository repository = null;

					try
					{
						repository = getRepositoryWithId(repositoryVO.getRepositoryId(), db);

						processBean.updateProcess("tool.managementtool.repository.delete.process.init");

						DeleteSiteNodeParams params = new DeleteSiteNodeParams();
						Map<String, List<ReferenceBean>> siteNodeContactPersons = new HashMap<String, List<ReferenceBean>>();
						params.setContactPersons(siteNodeContactPersons);
						params.setForceDelete(forceDelete);
						params.setSkipServiceBindings(skipServiceBindings);
						params.setProcessBean(processBean);
						params.setTotalSiteNodes(getTotalNumberSiteNodes(repositoryVO.getRepositoryId()));
						SiteNodeController.getController().deleteSiteNodesInRepository(repositoryVO.getRepositoryId(), infoGluePrincipal, params, db);

						processBean.updateProcess("tool.managementtool.repository.delete.process.deleteContents");
						ContentController.getContentController().deleteContentInRepository(repositoryVO.getRepositoryId(), infoGluePrincipal, forceDelete, db);

						RepositoryLanguageController.getController().deleteRepositoryLanguages(repository, db);

						db.remove(repository);

						//If any of the validations or setMethods reported an error, we throw them up now before remove.
						ceb.throwIfNotEmpty();

						commitThreadTransaction();
						
						processBean.updateProcess("tool.managementtool.repository.delete.process.notifySiteNodes");
						SiteNodeController.getController().notifyContactPersonsForSiteNode(siteNodeContactPersons);
						
						processBean.setStatus(ProcessBean.FINISHED);
					}
					catch(ConstraintException ce)
					{
						logger.warn("An error occurred so we should not completes the transaction:" + ce, ce);
						rollbackThreadTransaction();
						// TODO improve handling of constrain exceptions
						processBean.setError("An error occurred so we should not completes the transaction:" + ce, ce);
					}
					catch(Throwable ex)
					{
						logger.error("An error occurred so we should not completes the transaction. Message: " + ex);
						logger.warn("An error occurred so we should not complete the transaction", ex);
						rollbackThreadTransaction();
						processBean.setError("An error occurred when deleting the repository. Message: " + ex.getMessage(), ex);
					}
				}
				catch (Throwable tr)
				{
					logger.warn("An unexpected exception occured when deleting a repository", tr);
					processBean.setError("An unexpected exception occured when deleting a repository. Message: " + tr.getMessage(), tr);
				}
			}
		}.start();
	}

	public RepositoryVO update(RepositoryVO vo) throws ConstraintException, SystemException
	{
		return (RepositoryVO) updateEntity(RepositoryImpl.class, (BaseEntityVO) vo);
	}

	public RepositoryVO update(RepositoryVO repositoryVO, String[] languageValues) throws ConstraintException, SystemException
	{
    	Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
        	Repository repository = RepositoryController.getController().getRepositoryWithId(repositoryVO.getRepositoryId(), db);
        	
        	RepositoryLanguageController.getController().deleteRepositoryLanguages(repository, db);

        	//add validation here if needed   			
            List repositoryLanguageList = new ArrayList();
            if(languageValues != null)
			{
				for (int i=0; i < languageValues.length; i++)
	            {
	            	Language language = LanguageController.getController().getLanguageWithId(new Integer(languageValues[i]), db);
	            	RepositoryLanguage repositoryLanguage = RepositoryLanguageController.getController().create(repositoryVO.getRepositoryId(), new Integer(languageValues[i]), new Integer(i), db);
	            	repositoryLanguageList.add(repositoryLanguage);
					language.getRepositoryLanguages().add(repositoryLanguage);
	            }
			}
			
			repository.setValueObject(repositoryVO);
			repository.setRepositoryLanguages(repositoryLanguageList);
			
			repositoryVO = repository.getValueObject();
			
            //If any of the validations or setMethods reported an error, we throw them up now before create.
            ceb.throwIfNotEmpty();
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
            logger.warn("An error occurred so we should not completes the transaction:" + ce, ce);
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return repositoryVO;
    }        
    
	// Singe object
    public Repository getRepositoryWithId(Integer id, Database db) throws SystemException, Bug
    {
		return (Repository) getObjectWithId(RepositoryImpl.class, id, db);
    }

    public RepositoryVO getRepositoryVOWithId(Integer repositoryId) throws ConstraintException, SystemException, Bug
    {
		return  (RepositoryVO) getVOWithId(RepositoryImpl.class, repositoryId);        
    }
	
    public RepositoryVO getRepositoryVOWithId(Integer repositoryId, Database db) throws ConstraintException, SystemException, Bug
    {
		return  (RepositoryVO) getVOWithId(RepositoryImpl.class, repositoryId, db);        
    }
    
	/**
	 * Returns the RepositoryVO with the given name.
	 * 
	 * @param name
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */
	
	public RepositoryVO getRepositoryVOWithName(String name) throws SystemException, Bug
	{
		RepositoryVO repositoryVO = null;
		
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			Repository repository = getRepositoryWithName(name, db);
			if(repository != null)
				repositoryVO = repository.getValueObject();
			
			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
		
		return repositoryVO;	
	}
	
	/**
	 * Returns the Repository with the given name fetched within a given transaction.
	 * 
	 * @param name
	 * @param db
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */

	public Repository getRepositoryWithName(String name, Database db) throws SystemException, Bug
	{
		Repository repository = null;
		
		try
		{
			OQLQuery oql = db.getOQLQuery("SELECT f FROM org.infoglue.cms.entities.management.impl.simple.RepositoryImpl f WHERE f.name = $1");
			oql.bind(name);
			
			QueryResults results = oql.execute();
			this.logger.info("Fetching entity in read/write mode" + name);

			if (results.hasMore()) 
			{
				repository = (Repository)results.next();
			}
			
			results.close();
			oql.close();
		}
		catch(Exception e)
		{
			throw new SystemException("An error occurred when we tried to fetch a named repository. Reason:" + e.getMessage(), e);    
		}
		
		return repository;		
	}

	/**
	 * This method can be used by actions and use-case-controllers that only need to have simple access to the
	 * functionality. They don't get the transaction-safety but probably just wants to show the info.
	 */	
    
    public List getRepositoryVOList() throws ConstraintException, SystemException, Bug
    {   
		String key = "repositoryVOList";
		logger.info("key:" + key);
		List cachedRepositoryVOList = (List)CacheController.getCachedObject("repositoryCache", key);
		if(cachedRepositoryVOList != null)
		{
			logger.info("There was an cached authorization:" + cachedRepositoryVOList.size());
			return cachedRepositoryVOList;
		}
				
		List repositoryVOList = getAllVOObjects(RepositoryImpl.class, "repositoryId");

		CacheController.cacheObject("repositoryCache", key, repositoryVOList);
			
		return repositoryVOList;
    }

    
	/**
	 * This method can be used by actions and use-case-controllers that only need to have simple access to the
	 * functionality. They don't get the transaction-safety but probably just wants to show the info.
	 */	
	
	public List getAuthorizedRepositoryVOList(InfoGluePrincipal infoGluePrincipal, boolean isBindingDialog) throws ConstraintException, SystemException, Bug
	{    	
		List accessableRepositories = new ArrayList();
    	
		List allRepositories = this.getRepositoryVOList();
		Iterator i = allRepositories.iterator();
		while(i.hasNext())
		{
			RepositoryVO repositoryVO = (RepositoryVO)i.next();
			if(getIsAccessApproved(repositoryVO.getRepositoryId(), infoGluePrincipal, isBindingDialog))
			{
				accessableRepositories.add(repositoryVO);
			}
		}
    	
		Collections.sort(accessableRepositories, new ReflectionComparator("name"));

		return accessableRepositories;
	}



	
	/**
	 * Return the first of all repositories.
	 */
	
	public RepositoryVO getFirstRepositoryVO()  throws SystemException, Bug
	{
		Database db = CastorDatabaseService.getDatabase();
		RepositoryVO repositoryVO = null;
		
		try 
		{
			beginTransaction(db);
		
			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RepositoryImpl r ORDER BY r.repositoryId");
        	QueryResults results = oql.execute();
			this.logger.info("Fetching entity in read/write mode");

			if (results.hasMore()) 
            {
                Repository repository = (Repository)results.next();
                repositoryVO = repository.getValueObject();
            }
            
			results.close();
			oql.close();

			commitTransaction(db);
		}
		catch ( Exception e)		
		{
			throw new SystemException("An error occurred when we tried to fetch a list of roles in the repository. Reason:" + e.getMessage(), e);			
		}
		return repositoryVO;		
	}
	


	/**
	 * This method deletes the Repository sent in from the system.
	 */	
	public void delete(Integer repositoryId, Database db) throws SystemException, Bug
	{
		try
		{
			db.remove(getRepositoryWithId(repositoryId, db));
		}
		catch(Exception e)
		{
			throw new SystemException("An error occurred when we tried to delete Repository in the database. Reason: " + e.getMessage(), e);
		}	
	} 


	/**
	 * This method returns true if the user should have access to the repository sent in.
	 */
    
	public boolean getIsAccessApproved(Integer repositoryId, InfoGluePrincipal infoGluePrincipal, boolean isBindingDialog) throws SystemException
	{
		logger.info("getIsAccessApproved for " + repositoryId + " AND " + infoGluePrincipal + " AND " + isBindingDialog);
		boolean hasAccess = false;
    	
		Database db = CastorDatabaseService.getDatabase();
       
		beginTransaction(db);

		try
		{ 
		    if(isBindingDialog)
		        hasAccess = (AccessRightController.getController().getIsPrincipalAuthorized(db, infoGluePrincipal, "Repository.Read", repositoryId.toString()) || AccessRightController.getController().getIsPrincipalAuthorized(db, infoGluePrincipal, "Repository.ReadForBinding", repositoryId.toString()));
		    else
		        hasAccess = AccessRightController.getController().getIsPrincipalAuthorized(db, infoGluePrincipal, "Repository.Read", repositoryId.toString()); 
		        
			commitTransaction(db);
		}
		catch(Exception e)
		{
			logger.error("An error occurred so we should not complete the transaction:" + e, e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
		
		return hasAccess;
	}	
    
	
	/**
	 * This is a method that gives the user back an newly initialized ValueObject for this entity that the controller
	 * is handling.
	 */

	public BaseEntityVO getNewVO()
	{
		return new RepositoryVO();
	}
		
}
 
