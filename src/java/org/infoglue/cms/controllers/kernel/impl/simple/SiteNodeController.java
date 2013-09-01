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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.applications.databeans.ReferenceBean;
import org.infoglue.cms.applications.databeans.ReferenceVersionBean;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.impl.simple.ContentImpl;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Repository;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.entities.management.ServiceDefinition;
import org.infoglue.cms.entities.management.ServiceDefinitionVO;
import org.infoglue.cms.entities.management.SiteNodeTypeDefinition;
import org.infoglue.cms.entities.management.TableCount;
import org.infoglue.cms.entities.management.impl.simple.RepositoryImpl;
import org.infoglue.cms.entities.management.impl.simple.SiteNodeTypeDefinitionImpl;
import org.infoglue.cms.entities.structure.ServiceBindingVO;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.SiteNodeVersionVO;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeVersionImpl;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.XMLHelper;
import org.infoglue.cms.util.mail.MailServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

public class SiteNodeController extends BaseController 
{
    private final static Logger logger = Logger.getLogger(SiteNodeController.class.getName());

	protected static final Integer NO 			= new Integer(0);
	protected static final Integer YES 			= new Integer(1);
	protected static final Integer INHERITED 	= new Integer(2);

	/**
	 * Factory method
	 */

	public static SiteNodeController getController()
	{
		return new SiteNodeController();
	}

   	/**
	 * This method returns selected active content versions.
	 */
    
	public List<SiteNode> getSiteNodeList(Integer repositoryId, Integer minimumId, Integer limit, Database db) throws SystemException, Bug, Exception
	{
		List<SiteNode> siteNodeList = new ArrayList<SiteNode>();

        OQLQuery oql = db.getOQLQuery( "SELECT sn FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl sn WHERE sn.repositoryId = $1 AND sn.siteNodeId > $2 ORDER BY sn.siteNodeId LIMIT $3");
    	oql.bind(repositoryId);
		oql.bind(minimumId);
		oql.bind(limit);
    	
    	QueryResults results = oql.execute(Database.ReadOnly);
		while (results.hasMore()) 
        {
			SiteNode siteNode = (SiteNode)results.next();
			siteNodeList.add(siteNode);
        }
		
		results.close();
		oql.close();

		return siteNodeList;
	}

	public SiteNode getSmallSiteNodeWithId(Integer siteNodeId, Database db) throws SystemException, Bug
	{
		return (SiteNode) getObjectWithId(SmallSiteNodeImpl.class, siteNodeId, db);
	}
	
	/**
	 * This method gets the siteNodeVO with the given id
	 */
	public SiteNodeVO getSiteNodeVOWithId(Integer siteNodeId) throws SystemException, Bug
	{
		return (SiteNodeVO) getVOWithId(SiteNodeImpl.class, siteNodeId);
	}

	/**
	 * This method gets the siteNodeVO with the given id
	 */
	 
    public static SiteNodeVO getSiteNodeVOWithId(Integer siteNodeId, Database db) throws SystemException, Bug
    {
		return (SiteNodeVO) getVOWithId(SiteNodeImpl.class, siteNodeId, db);
    }

    /**
	 * This method gets the siteNodeVO with the given id
	 */
	 
    public static SiteNodeVO getSmallSiteNodeVOWithId(Integer siteNodeId, Database db) throws SystemException, Bug
    {
		return (SiteNodeVO) getVOWithId(SmallSiteNodeImpl.class, siteNodeId, db);
    }


    public SiteNode getSiteNodeWithId(Integer siteNodeId, Database db) throws SystemException, Bug
    {
        return getSiteNodeWithId(siteNodeId, db, false);
    }

    public SiteNodeVersion getSiteNodeVersionWithId(Integer siteNodeVersionId, Database db) throws SystemException, Bug
    {
		return (SiteNodeVersion) getObjectWithId(SiteNodeVersionImpl.class, siteNodeVersionId, db);
    }

    public static SiteNode getSiteNodeWithId(Integer siteNodeId, Database db, boolean readOnly) throws SystemException, Bug
    {
        SiteNode siteNode = null;
        try
        {
        	if(readOnly)
	            siteNode = (SiteNode)db.load(org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl.class, siteNodeId, Database.ReadOnly);
    		else
    		{
                logger.info("Loading " + siteNodeId + " in read/write mode.");
	            siteNode = (SiteNode)db.load(org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl.class, siteNodeId);
    		}
    	}
        catch(Exception e)
        {
            throw new SystemException("An error occurred when we tried to fetch the SiteNode. Reason:" + e.getMessage(), e);    
        }
    
        if(siteNode == null)
        {
            throw new Bug("The SiteNode with id [" + siteNodeId + "] was not found in SiteNodeHelper.getSiteNodeWithId. This should never happen.");
        }
    
        return siteNode;
	}

	public void deleteSiteNodesInRepository(Integer repositoryId, InfoGluePrincipal infogluePrincipal, DeleteSiteNodeParams params, Database db) throws Exception
	{
		SiteNode siteNode = getRootSiteNode(repositoryId, db);
		params.setExcludeReferencesInSite(true);
		delete(siteNode.getValueObject(), infogluePrincipal, params, db);
	}

	public static class DeleteSiteNodeParams
	{
		private boolean skipServiceBindings = false;
		private boolean forceDelete = false;
		private boolean excludeReferencesInSite = false;
		private boolean notifyContactPersons = CmsPropertyHandler.getNotifyResponsibleOnReferenceChange();
		private Map<String, List<ReferenceBean>> contactPersons = null;
		private int totalSiteNodes;

		private ProcessBean processBean = null;
		private String stepKey;
		private int deleteCounter = 0;

		public void setSkipServiceBindings(boolean skipServiceBindings)
		{
			this.skipServiceBindings = skipServiceBindings;
		}
		public void setForceDelete(boolean forceDelete)
		{
			this.forceDelete = forceDelete;
		}
		public void setExcludeReferencesInSite(boolean excludeReferencesInSite)
		{
			this.excludeReferencesInSite = excludeReferencesInSite;
		}
		public void setNotifyContactPersons(boolean notifyContactPersons)
		{
			this.notifyContactPersons = notifyContactPersons;
		}
		public void setContactPersons(Map<String, List<ReferenceBean>> contactPersons)
		{
			this.contactPersons = contactPersons;
		}
		public void setProcessBean(ProcessBean processBean)
		{
			this.processBean = processBean;
		}
		public void setTotalSiteNodes(Integer totalSiteNodes)
		{
			this.totalSiteNodes = totalSiteNodes;
		}
		
		public void incrementProcessCounter()
		{
			if (processBean != null)
			{
				processBean.updateCurrentEvent(stepKey, ++deleteCounter, totalSiteNodes);
			}
		}
		
		public void changeStep(String eventKey)
		{
			if (processBean != null)
			{
				this.stepKey = eventKey;
				processBean.updateProcess(this.stepKey, 0, totalSiteNodes);
			}
		}
	}

	public void delete(SiteNodeVO siteNodeVO, InfoGluePrincipal infogluePrincipal, DeleteSiteNodeParams params) throws ConstraintException, SystemException
	{
		if (params == null)
		{
			params = new DeleteSiteNodeParams();
		}
		Map<String, List<ReferenceBean>> contactPersons = new HashMap<String, List<ReferenceBean>>();
		params.contactPersons = contactPersons;
		Database db = CastorDatabaseService.getThreadDatabase();
		try
		{
			delete(siteNodeVO, infogluePrincipal, params, db);
		
			commitThreadTransaction();
		}
		catch(ConstraintException ce)
		{
			logger.warn("An error occurred so we should not complete the transaction:" + ce, ce);
			rollbackThreadTransaction();
			throw ce;
		}
		catch(Throwable tr)
		{
			logger.error("An error occurred so we should not complete the transaction:" + tr, tr);
			rollbackThreadTransaction();
			throw new SystemException(tr.getMessage());
		}

		notifyContactPersonsForSiteNode(contactPersons);
	}

	public void delete(SiteNodeVO siteNodeVO, InfoGluePrincipal infogluePrincipal, DeleteSiteNodeParams params, Database db) throws ConstraintException, SystemException, Exception
	{
		params.changeStep("tool.managementtool.repository.delete.process.deleteSiteNodes");
		SiteNode siteNode = getSiteNodeWithId(siteNodeVO.getSiteNodeId(), db);
		SiteNode parent = siteNode.getParentSiteNode();
		if (siteNodeVO.getParentSiteNodeId() != null)
		{
			@SuppressWarnings("unchecked")
			Collection<SiteNode> children = parent.getChildSiteNodes(); //getSmallSiteNodeChildrenList(siteNodeVO.getParentSiteNodeId(), db);
			Iterator<SiteNode> childSiteNodeIterator = children.iterator();
			while(childSiteNodeIterator.hasNext())
			{
				SiteNode candidate = (SiteNode)childSiteNodeIterator.next();
				if(candidate.getId().equals(siteNode.getSiteNodeId()))
				{
					deleteRecursive(siteNode, childSiteNodeIterator, infogluePrincipal, params, db);
				}
			}
		}
		else
		{
			deleteRecursive(siteNode, null, infogluePrincipal, params, db);
		}
	}

	/**
	 * Recursively deletes all siteNodes and their versions.
	 * This method is a mess as we had a problem with the lazy-loading and transactions. 
	 * We have to begin and commit all the time...
	 */
	private void deleteRecursive(SiteNode siteNode, Iterator<SiteNode> parentIterator, InfoGluePrincipal infogluePrincipal, DeleteSiteNodeParams params, Database db) throws ConstraintException, SystemException, Exception
	{
		try
		{
			List<ReferenceBean> referenceBeanList = RegistryController.getController().getReferencingObjectsForSiteNode(siteNode.getSiteNodeId(), -1, params.excludeReferencesInSite, db);
	
			if(referenceBeanList != null && referenceBeanList.size() > 0 && !params.forceDelete)
				throw new ConstraintException("SiteNode.stateId", "3405");
	
			@SuppressWarnings("unchecked")
			Collection<SiteNode> children = siteNode.getChildSiteNodes(); //getSmallSiteNodeChildrenList(siteNode.getSiteNodeId(), db);
			Iterator<SiteNode> childIterator = children.iterator();
			while(childIterator.hasNext())
			{
				SiteNode childSiteNode = childIterator.next();
				deleteRecursive(childSiteNode, childIterator, infogluePrincipal, params, db);
			}
			siteNode.setChildSiteNodes(new ArrayList<SiteNode>());
	
			if(params.forceDelete || getIsDeletable(siteNode, infogluePrincipal, db))
			{
				SiteNodeVersionController.getController().deleteVersionsForSiteNode(siteNode, db, infogluePrincipal);
				if (!params.skipServiceBindings)
				{
					ServiceBindingController.deleteServiceBindingsReferencingSiteNode(siteNode, db);
				}
	
				if (!params.notifyContactPersons)
				{
					RegistryController.getController().cleanAllForSiteNode(siteNode.getSiteNodeId(), infogluePrincipal, db);
				}
				if (params.notifyContactPersons)
				{
					if (referenceBeanList != null && params.contactPersons != null)
					{
						logger.info("Found " + referenceBeanList.size() + " people to notify about SiteNode removal. SiteNode.id: " + siteNode.getSiteNodeId());
						params.contactPersons.put(getSiteNodePath(siteNode.getSiteNodeId(), false, true, db), referenceBeanList);
					}
				}
	
				if(parentIterator != null) 
					parentIterator.remove();
	
				db.remove(siteNode);
				params.incrementProcessCounter();
			}
			else
			{
				throw new ConstraintException("SiteNodeVersion.stateId", "3400");
			}
		}
		catch (Throwable tr)
		{
			/* This catch's purpose is to add some useful data to the process bean. It is not responsible for logging
			 * the exception nor halting the execution of the delete operation.
			 * A brief INFO-level logging is included since the catch may obscures the exception type and line number.
			 */
			logger.info("Error when deleting SiteNode. Message: " + tr.getMessage() + ". Type: " + tr.getClass());
			try
			{
				params.processBean.setError("tool.managementtool.repository.delete.process.errorSiteNode", getSiteNodePath(siteNode.getSiteNodeId(), false, true, db), siteNode.getSiteNodeId());
			}
			catch (Exception processEx)
			{
				logger.warn("Failed to gather error information for process bean. Message: " + processEx.getMessage() + ". Type: " + processEx.getClass());
				params.processBean.setError("tool.managementtool.repository.delete.process.errorSiteNode.noInfo");
			}
			throw new SystemException(tr);
		}
	}

	/**
	 * This method returns true if the sitenode does not have any published siteNodeversions or 
	 * are restricted in any other way.
	 */
	private static boolean getIsDeletable(SiteNode siteNode, InfoGluePrincipal infogluePrincipal, Database db) throws SystemException, Exception
	{
		boolean isDeletable = true;

		SiteNodeVersionVO latestSiteNodeVersionVO = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersionVO(db, siteNode.getId());
		if(latestSiteNodeVersionVO != null && latestSiteNodeVersionVO.getIsProtected().equals(SiteNodeVersionVO.YES))
		{
			boolean hasAccess = AccessRightController.getController().getIsPrincipalAuthorized(db, infogluePrincipal, "SiteNodeVersion.DeleteSiteNode", "" + latestSiteNodeVersionVO.getId());
			if (!hasAccess)
				return false;
		}

		List<SiteNodeVersion> siteNodeVersions = SiteNodeVersionController.getController().getSiteNodeVersionListForSiteNode(siteNode.getSiteNodeId(), db);
		if(siteNodeVersions != null)
		{
			Iterator<SiteNodeVersion> versionIterator = siteNodeVersions.iterator();
			while (versionIterator.hasNext()) 
			{
				SiteNodeVersion siteNodeVersion = versionIterator.next();
				if(siteNodeVersion.getStateId().intValue() == SiteNodeVersionVO.PUBLISHED_STATE.intValue() && siteNodeVersion.getIsActive().booleanValue() == true)
				{
					logger.warn("The siteNode had a published version so we cannot delete it..");
					isDeletable = false;
					break;
				}
			}
		}

		return isDeletable;	
	}

	
	public SiteNodeVO create(Integer parentSiteNodeId, Integer siteNodeTypeDefinitionId, InfoGluePrincipal infoGluePrincipal, Integer repositoryId, SiteNodeVO siteNodeVO) throws ConstraintException, SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
		ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

		SiteNode siteNode = null;

		beginTransaction(db);

		try
		{
			//Here you might want to add some validate functonality?
			siteNode = create(db, parentSiteNodeId, siteNodeTypeDefinitionId, infoGluePrincipal, repositoryId, siteNodeVO);
             
			//If any of the validations or setMethods reported an error, we throw them up now before create.
			ceb.throwIfNotEmpty();
            
			commitTransaction(db);
		}
		catch(ConstraintException ce)
		{
			logger.warn("An error occurred so we should not complete the transaction:" + ce, ce);
			//rollbackTransaction(db);
			throw ce;
		}
		catch(Exception e)
		{
			logger.error("An error occurred so we should not complete the transaction:" + e, e);
			//rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}

		return siteNode.getValueObject();
	}     
    
    public SiteNode create(Database db, Integer parentSiteNodeId, Integer siteNodeTypeDefinitionId, InfoGluePrincipal infoGluePrincipal, Integer repositoryId, SiteNodeVO siteNodeVO) throws SystemException, Exception
    {
	    SiteNode siteNode = null;

        logger.info("******************************************");
        logger.info("parentSiteNode:" + parentSiteNodeId);
        logger.info("siteNodeTypeDefinition:" + siteNodeTypeDefinitionId);
        logger.info("repository:" + repositoryId);
        logger.info("******************************************");
        
        //Fetch related entities here if they should be referenced        
        
        SiteNode parentSiteNode = null;
      	SiteNodeTypeDefinition siteNodeTypeDefinition = null;

        if(parentSiteNodeId != null)
        {
       		parentSiteNode = getSiteNodeWithId(parentSiteNodeId, db);
			if(repositoryId == null)
				repositoryId = parentSiteNode.getRepository().getRepositoryId();	
        }		
        
        if(siteNodeTypeDefinitionId != null)
        	siteNodeTypeDefinition = SiteNodeTypeDefinitionController.getController().getSiteNodeTypeDefinitionWithId(siteNodeTypeDefinitionId, db);

        Repository repository = RepositoryController.getController().getRepositoryWithId(repositoryId, db);

        
        siteNode = new SiteNodeImpl();
        siteNode.setValueObject(siteNodeVO);
        siteNode.setParentSiteNode((SiteNodeImpl)parentSiteNode);
        siteNode.setRepository((RepositoryImpl)repository);
        siteNode.setSiteNodeTypeDefinition((SiteNodeTypeDefinitionImpl)siteNodeTypeDefinition);
        siteNode.setCreator(infoGluePrincipal.getName());

        db.create(siteNode);
        
        if(parentSiteNode != null)
        	parentSiteNode.getChildSiteNodes().add(siteNode);
        
        //commitTransaction(db);
        //siteNode = (SiteNode) createEntity(siteNode, db);
        
        //No siteNode is an island (humhum) so we also have to create an siteNodeVersion for it. 
        SiteNodeVersionController.createInitialSiteNodeVersion(db, siteNode, infoGluePrincipal);
                    
        return siteNode;
    }

	/**
	 * This method creates a new SiteNode and an siteNodeVersion. It does not commit the transaction however.
	 * 
	 * @param db
	 * @param parentSiteNodeId
	 * @param siteNodeTypeDefinitionId
	 * @param userName
	 * @param repositoryId
	 * @param siteNodeVO
	 * @return
	 * @throws SystemException
	 */
	
	public SiteNode createNewSiteNode(Database db, Integer parentSiteNodeId, Integer siteNodeTypeDefinitionId, InfoGluePrincipal infoGluePrincipal, Integer repositoryId, SiteNodeVO siteNodeVO) throws SystemException
	{
		SiteNode siteNode = null;

		try
		{
			logger.info("******************************************");
			logger.info("parentSiteNode:" + parentSiteNodeId);
			logger.info("siteNodeTypeDefinition:" + siteNodeTypeDefinitionId);
			logger.info("repository:" + repositoryId);
			logger.info("******************************************");
            
        	//Fetch related entities here if they should be referenced        
			
			SiteNode parentSiteNode = null;
			SiteNodeTypeDefinition siteNodeTypeDefinition = null;

			if(parentSiteNodeId != null)
			{
				parentSiteNode = getSiteNodeWithId(parentSiteNodeId, db);
				if(repositoryId == null)
					repositoryId = parentSiteNode.getRepository().getRepositoryId();	
			}		
			
			if(siteNodeTypeDefinitionId != null)
				siteNodeTypeDefinition = SiteNodeTypeDefinitionController.getController().getSiteNodeTypeDefinitionWithId(siteNodeTypeDefinitionId, db);
			
			Repository repository = RepositoryController.getController().getRepositoryWithId(repositoryId, db);

			siteNode = new SiteNodeImpl();
			siteNode.setValueObject(siteNodeVO);
			siteNode.setParentSiteNode((SiteNodeImpl)parentSiteNode);
			siteNode.setRepository((RepositoryImpl)repository);
			siteNode.setSiteNodeTypeDefinition((SiteNodeTypeDefinitionImpl)siteNodeTypeDefinition);
			siteNode.setCreator(infoGluePrincipal.getName());

			//siteNode = (SiteNode) createEntity(siteNode, db);
			db.create((SiteNode)siteNode);
		
			//No siteNode is an island (humhum) so we also have to create an siteNodeVersion for it.
			SiteNodeVersion siteNodeVersion = SiteNodeVersionController.createInitialSiteNodeVersion(db, siteNode, infoGluePrincipal);
		
			List siteNodeVersions = new ArrayList();
			siteNodeVersions.add(siteNodeVersion);
			siteNode.setSiteNodeVersions(siteNodeVersions);
		}
		catch(Exception e)
		{
		    throw new SystemException("An error occurred when we tried to create the SiteNode in the database. Reason:" + e.getMessage(), e);    
		}
        
		return siteNode;
	}


	/**
	 * This method returns the value-object of the parent of a specific siteNode. 
	 */
	
    public static SiteNodeVO getParentSiteNode(Integer siteNodeId) throws SystemException, Bug
    {
    	Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
		SiteNodeVO parentSiteNodeVO = null;
		
        beginTransaction(db);

        try
        {
			SiteNode parent = getParentSiteNode(siteNodeId, db);
			if(parent != null)
				parentSiteNodeVO = parent.getValueObject();
            
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
		return parentSiteNodeVO;    	
    }
    
	/**
	 * This method returns the value-object of the parent of a specific siteNode. 
	 */
	
	public static SiteNode getParentSiteNode(Integer siteNodeId, Database db) throws SystemException, Bug
	{
		SiteNode siteNode = (SiteNode) getObjectWithId(SiteNodeImpl.class, siteNodeId, db);
		SiteNode parent = siteNode.getParentSiteNode();

		return parent;    	
	}

	public List<SiteNodeVO> getSiteNodeChildrenVOList(Integer parentSiteNodeId) throws ConstraintException, SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
		List<SiteNodeVO> childrenVOList = null;
		beginTransaction(db);
		try
		{
			childrenVOList = getSiteNodeChildrenVOList(parentSiteNodeId, db);
			commitTransaction(db);
		}
		catch(ConstraintException ce)
		{
			logger.warn("An error occurred so we should not complete the transaction:" + ce, ce);
			rollbackTransaction(db);
			throw ce;
		}
		catch(Exception e)
		{
			logger.error("An error occurred so we should not complete the transaction:" + e, e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}

		return childrenVOList;
	}

	public List<SiteNodeVO> getSiteNodeChildrenVOList(Integer parentSiteNodeId, Database db) throws SystemException, Exception
	{
		if(parentSiteNodeId == null)
		{
			return null;
		}

		List<SiteNodeVO> siteNodeVOList = new ArrayList<SiteNodeVO>();

		OQLQuery oql = db.getOQLQuery( "SELECT s FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl s WHERE s.parentSiteNode.siteNodeId = $1 ORDER BY s.siteNodeId");
		oql.bind(parentSiteNodeId);

		QueryResults results = oql.execute(Database.ReadOnly);

		while (results.hasMore()) 
		{
			SiteNode siteNode = (SiteNode)results.next();
			siteNodeVOList.add(siteNode.getValueObject());
		}

		results.close();
		oql.close();

		return siteNodeVOList;
	}

	public List<SiteNode> getSmallSiteNodeChildrenList(Integer parentSiteNodeId) throws SystemException
	{
		Database db = CastorDatabaseService.getDatabase();

		List<SiteNode> childList = null;

		try
		{
			beginTransaction(db);

			childList = getSmallSiteNodeChildrenList(parentSiteNodeId, db);

			commitTransaction(db);
		}
		catch(Exception ex)
		{
			logger.error("An error occurred so we should not complete the transaction. Message: " + ex);
			logger.warn("An error occurred so we should not complete the transaction.", ex);
			rollbackTransaction(db);
			throw new SystemException(ex.getMessage());
		}

		return childList;
	}

	public List<SiteNode> getSmallSiteNodeChildrenList(Integer parentSiteNodeId, Database db) throws SystemException, Exception
	{
		if (parentSiteNodeId == null)
		{
			return null;
		}

		List<SiteNode> siteNodeList = new ArrayList<SiteNode>();

		OQLQuery oql = db.getOQLQuery( "SELECT s FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl s WHERE s.parentSiteNode.siteNodeId = $1 ORDER BY s.siteNodeId");
		oql.bind(parentSiteNodeId);

		QueryResults results = oql.execute();

		while (results.hasMore()) 
		{
			SiteNode siteNode = (SiteNode)results.next();
			siteNodeList.add(siteNode);
		}

		results.close();
		oql.close();

		return siteNodeList;
	}

	/**
	 * This method returns a list of the children a siteNode has.
	 */
	public List getSiteNodeChildren(Integer parentSiteNodeId) throws ConstraintException, SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
		ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

		List childrenVOList = null;

		beginTransaction(db);

		try
		{
			SiteNode siteNode = getSiteNodeWithId(parentSiteNodeId, db);
			Collection children = siteNode.getChildSiteNodes();
			childrenVOList = SiteNodeController.toVOList(children);

			//If any of the validations or setMethods reported an error, we throw them up now before create.
			ceb.throwIfNotEmpty();

			commitTransaction(db);
		}
		catch(ConstraintException ce)
		{
			logger.warn("An error occurred so we should not complete the transaction:" + ce, ce);
			rollbackTransaction(db);
			throw ce;
		}
		catch(Exception e)
		{
			logger.error("An error occurred so we should not complete the transaction:" + e, e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}

		return childrenVOList;
	}
	
    
    /**
	 * This method is sort of a sql-query-like method where you can send in arguments in form of a list
	 * of things that should match. The input is a Hashmap with a method and a List of HashMaps.
	 */
	
    public List getSiteNodeVOList(HashMap argumentHashMap) throws SystemException, Bug
    {
    	List siteNodes = null;
    	
    	String method = (String)argumentHashMap.get("method");
    	logger.info("method:" + method);
    	
    	if(method.equalsIgnoreCase("selectSiteNodeListOnIdList"))
    	{
			siteNodes = new ArrayList();
			List arguments = (List)argumentHashMap.get("arguments");
			logger.info("Arguments:" + arguments.size());  
			Iterator argumentIterator = arguments.iterator();
			while(argumentIterator.hasNext())
			{ 		
				HashMap argument = (HashMap)argumentIterator.next(); 
				logger.info("argument:" + argument.size());
				 
				Iterator iterator = argument.keySet().iterator();
			    while ( iterator.hasNext() )
			       logger.info( "   " + iterator.next() );


				Integer siteNodeId = new Integer((String)argument.get("siteNodeId"));
				logger.info("Getting the siteNode with Id:" + siteNodeId);
				siteNodes.add(getSiteNodeVOWithId(siteNodeId));
			}
    	}
        
        return siteNodes;
    }

    /**
	 * This method is sort of a sql-query-like method where you can send in arguments in form of a list
	 * of things that should match. The input is a Hashmap with a method and a List of HashMaps.
	 */
	
    public static List getSiteNodeVOList(HashMap argumentHashMap, Database db) throws SystemException, Bug
    {
    	List siteNodes = null;
    	
    	String method = (String)argumentHashMap.get("method");
    	logger.info("method:" + method);
    	
    	if(method.equalsIgnoreCase("selectSiteNodeListOnIdList"))
    	{
			siteNodes = new ArrayList();
			List arguments = (List)argumentHashMap.get("arguments");
			logger.info("Arguments:" + arguments.size());  
			Iterator argumentIterator = arguments.iterator();
			while(argumentIterator.hasNext())
			{ 		
				HashMap argument = (HashMap)argumentIterator.next(); 
				logger.info("argument:" + argument.size());
				 
				Iterator iterator = argument.keySet().iterator();
			    while ( iterator.hasNext() )
			       logger.info( "   " + iterator.next() );


				Integer siteNodeId = new Integer((String)argument.get("siteNodeId"));
				logger.info("Getting the siteNode with Id:" + siteNodeId);
				siteNodes.add(getSmallSiteNodeVOWithId(siteNodeId, db));
			}
    	}
        
        return siteNodes;
    }
	/**
	 * This method fetches the root siteNode for a particular repository.
	 */
	        
   	public SiteNodeVO getRootSiteNodeVO(Integer repositoryId) throws ConstraintException, SystemException
   	{
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        SiteNode siteNode = null;

        beginTransaction(db);

        try
        {
        	siteNode = getRootSiteNode(repositoryId, db);
			
			commitTransaction(db);

            //If any of the validations or setMethods reported an error, we throw them up now before create. 
            ceb.throwIfNotEmpty();
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return (siteNode == null) ? null : siteNode.getValueObject();
   	}

	/**
	 * This method fetches the root siteNode for a particular repository within a certain transaction.
	 */
	        
	public SiteNode getRootSiteNode(Integer repositoryId, Database db) throws ConstraintException, SystemException, Exception
	{
		SiteNode siteNode = null;
		
		OQLQuery oql = db.getOQLQuery( "SELECT s FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl s WHERE is_undefined(s.parentSiteNode) AND s.repository.repositoryId = $1");
		oql.bind(repositoryId);
		
		QueryResults results = oql.execute();
		logger.info("Fetching entity in read/write mode" + repositoryId);

		if (results.hasMore()) 
		{
			siteNode = (SiteNode)results.next();
		}

		results.close();
		oql.close();

		return siteNode;
	}

	public SiteNode getRootSmallSiteNode(Integer repositoryId, Database db) throws ConstraintException, SystemException, Exception
	{
		SiteNode siteNode = null;
		
		OQLQuery oql = db.getOQLQuery( "SELECT s FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl s WHERE is_undefined(s.parentSiteNode) AND s.repositoryId = $1");
		oql.bind(repositoryId);
		
		QueryResults results = oql.execute();
		
		if (results.hasMore()) 
		{
			siteNode = (SiteNode)results.next();
		}
		
		results.close();
		oql.close();
		
		return siteNode;
	}


	/**
	 * This method moves a siteNode after first making a couple of controls that the move is valid.
	 */
	
    public void moveSiteNode(SiteNodeVO siteNodeVO, Integer newParentSiteNodeId, InfoGluePrincipal principal) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        SiteNode siteNode          = null;
		SiteNode newParentSiteNode = null;
		SiteNode oldParentSiteNode = null;
		
        beginTransaction(db);

        try
        {
            //Validation that checks the entire object
            siteNodeVO.validate();
            
            if(newParentSiteNodeId == null)
            {
            	logger.warn("You must specify the new parent-siteNode......");
            	throw new ConstraintException("SiteNode.parentSiteNodeId", "3403");
            }

            if(siteNodeVO.getId().intValue() == newParentSiteNodeId.intValue())
            {
            	logger.warn("You cannot have the siteNode as it's own parent......");
            	throw new ConstraintException("SiteNode.parentSiteNodeId", "3401");
            }
            
            siteNode          = getSiteNodeWithId(siteNodeVO.getSiteNodeId(), db);
            oldParentSiteNode = siteNode.getParentSiteNode();
            newParentSiteNode = getSiteNodeWithId(newParentSiteNodeId, db);
            
            if(oldParentSiteNode.getId().intValue() == newParentSiteNodeId.intValue())
            {
            	logger.warn("You cannot specify the same node as it originally was located in......");
            	throw new ConstraintException("SiteNode.parentSiteNodeId", "3404");
            }

			SiteNode tempSiteNode = newParentSiteNode.getParentSiteNode();
			while(tempSiteNode != null)
			{
				if(tempSiteNode.getId().intValue() == siteNode.getId().intValue())
				{
					logger.warn("You cannot move the node to a child under it......");
            		throw new ConstraintException("SiteNode.parentSiteNodeId", "3402");
				}
				tempSiteNode = tempSiteNode.getParentSiteNode();
			}	
			
            logger.info("Setting the new Parent siteNode:" + siteNode.getSiteNodeId() + " " + newParentSiteNode.getSiteNodeId());
            siteNode.setParentSiteNode((SiteNodeImpl)newParentSiteNode);
            
            Integer metaInfoContentId = siteNode.getMetaInfoContentId();
            //System.out.println("metaInfoContentId:" + metaInfoContentId);
            if(!siteNode.getRepository().getId().equals(newParentSiteNode.getRepository().getId()) && metaInfoContentId != null)
            {
            	Content metaInfoContent = ContentController.getContentController().getContentWithId(metaInfoContentId, db);
            	Content newParentContent = ContentController.getContentController().getContentWithPath(newParentSiteNode.getRepository().getId(), "Meta info folder", true, principal, db);
            	if(metaInfoContent != null && newParentContent != null)
            	{
            		//System.out.println("Moving:" + metaInfoContent.getName() + " to " + newParentContent.getName());
            		newParentContent.getChildren().add(metaInfoContent);
            		Content previousParentContent = metaInfoContent.getParentContent();
            		metaInfoContent.setParentContent((ContentImpl)newParentContent);
            		previousParentContent.getChildren().remove(metaInfoContent);

            		changeRepositoryRecursiveForContent(metaInfoContent, newParentSiteNode.getRepository());
				}
            }
            
            changeRepositoryRecursive(siteNode, newParentSiteNode.getRepository());
            //siteNode.setRepository(newParentSiteNode.getRepository());
			newParentSiteNode.getChildSiteNodes().add(siteNode);
			oldParentSiteNode.getChildSiteNodes().remove(siteNode);
			
            //If any of the validations or setMethods reported an error, we throw them up now before create.
            ceb.throwIfNotEmpty();
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
            logger.warn("An error occurred so we should not complete the transaction:" + ce, ce);
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

    }       
    
	/**
	 * Recursively sets the sitenodes repositoryId.
	 * @param sitenode
	 * @param newRepository
	 */

	private void changeRepositoryRecursive(SiteNode siteNode, Repository newRepository)
	{
	    if(siteNode.getRepository().getId().intValue() != newRepository.getId().intValue())
	    {
	        siteNode.setRepository((RepositoryImpl)newRepository);
		    Iterator ChildSiteNodesIterator = siteNode.getChildSiteNodes().iterator();
		    while(ChildSiteNodesIterator.hasNext())
		    {
		        SiteNode childSiteNode = (SiteNode)ChildSiteNodesIterator.next();
		        changeRepositoryRecursive(childSiteNode, newRepository);
		    }
	    }
	}
	
	/**
	 * Recursively sets the sitenodes repositoryId.
	 * @param sitenode
	 * @param newRepository
	 */

	private void changeRepositoryRecursiveForContent(Content content, Repository newRepository)
	{
	    if(content.getRepository() == null || content.getRepository().getId().intValue() != newRepository.getId().intValue())
	    {
	    	content.setRepository((RepositoryImpl)newRepository);
	    	if(content.getChildren() != null)
	    	{
			    Iterator childContentsIterator = content.getChildren().iterator();
			    while(childContentsIterator.hasNext())
			    {
			    	Content childContent = (Content)childContentsIterator.next();
			    	changeRepositoryRecursiveForContent(childContent, newRepository);
			    }
			}
	    }
	}
	
	/**
	 * This is a method that gives the user back an newly initialized ValueObject for this entity that the controller
	 * is handling.
	 */

	public BaseEntityVO getNewVO()
	{
		return new SiteNodeVO();
	}

	/**
	 * This method returns a list of all siteNodes in a repository.
	 */

	public List getRepositorySiteNodes(Integer repositoryId, Database db) throws SystemException, Exception
    {
		List siteNodes = new ArrayList();
		
		OQLQuery oql = db.getOQLQuery("SELECT sn FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl sn WHERE sn.repository.repositoryId = $1");
    	oql.bind(repositoryId);
    	
    	QueryResults results = oql.execute(Database.ReadOnly);
		
		while(results.hasMore()) 
        {
        	SiteNode siteNode = (SiteNodeImpl)results.next();
        	siteNodes.add(siteNode);
        }
		
		results.close();
		oql.close();

		return siteNodes;    	
    }
	
	/**
	 * This method creates a meta info content for the new sitenode.
	 * 
	 * @param db
	 * @param path
	 * @param newSiteNode
	 * @throws SystemException
	 * @throws Bug
	 * @throws Exception
	 * @throws ConstraintException
	 */
	
    public Content createSiteNodeMetaInfoContent(Database db, SiteNode newSiteNode, Integer repositoryId, InfoGluePrincipal principal, Integer pageTemplateContentId) throws SystemException, Bug, Exception, ConstraintException
    {
        Content content = null;
        
        String basePath = "Meta info folder";
        String path = "";
        
        SiteNode parentSiteNode = newSiteNode.getParentSiteNode();
        while(parentSiteNode != null)
        {
            path = "/" + parentSiteNode.getName() + path;
            parentSiteNode = parentSiteNode.getParentSiteNode();
        }
        path = basePath + path;
        
        SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getLatestSiteNodeVersion(db, newSiteNode.getId(), false);
        Language masterLanguage 		= LanguageController.getController().getMasterLanguage(db, repositoryId);
  	   
        ServiceDefinitionVO singleServiceDefinitionVO 	= null;
        
        Integer metaInfoContentTypeDefinitionId = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName("Meta info", db).getId();
        Integer availableServiceBindingId = AvailableServiceBindingController.getController().getAvailableServiceBindingVOWithName("Meta information", db).getId();
        
        List serviceDefinitions = AvailableServiceBindingController.getController().getServiceDefinitionVOList(db, availableServiceBindingId);
        if(serviceDefinitions == null || serviceDefinitions.size() == 0)
        {
            ServiceDefinition serviceDefinition = ServiceDefinitionController.getController().getServiceDefinitionWithName("Core content service", db, false);
            String[] values = {serviceDefinition.getId().toString()};
            AvailableServiceBindingController.getController().update(availableServiceBindingId, values, db);
            singleServiceDefinitionVO = serviceDefinition.getValueObject();
        }
        else if(serviceDefinitions.size() == 1)
        {
        	singleServiceDefinitionVO = (ServiceDefinitionVO)serviceDefinitions.get(0);	    
        }
        
        ContentVO parentFolderContentVO = null;
        
        Content rootContent = ContentControllerProxy.getController().getRootContent(db, repositoryId, principal.getName(), true);
        if(rootContent != null)
        {
            ContentVO parentFolderContent = ContentController.getContentController().getContentVOWithPath(repositoryId, path, true, principal, db);
            
        	ContentVO contentVO = new ContentVO();
        	contentVO.setCreatorName(principal.getName());
        	contentVO.setIsBranch(new Boolean(false));
        	contentVO.setName(newSiteNode.getName() + " Metainfo");
        	contentVO.setRepositoryId(repositoryId);

        	content = ContentControllerProxy.getController().create(db, parentFolderContent.getId(), metaInfoContentTypeDefinitionId, repositoryId, contentVO);
        	
        	newSiteNode.setMetaInfoContentId(contentVO.getId());
        	
        	String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components></components>";
        	if(pageTemplateContentId != null)
        	{
        	    Integer masterLanguageId = LanguageController.getController().getMasterLanguage(db, repositoryId).getId();
        		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(pageTemplateContentId, masterLanguageId, db);
        		
        	    componentStructure = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO, "ComponentStructure", false);
        	
    			Document document = XMLHelper.readDocumentFromByteArray(componentStructure.getBytes("UTF-8"));
    			String componentXPath = "//component";
    			NodeList componentNodes = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
    			for(int i=0; i < componentNodes.getLength(); i++)
    			{
    				Element element = (Element)componentNodes.item(i);
    				String componentId = element.getAttribute("id");
    				String componentContentId = element.getAttribute("contentId");
    				
    				ComponentController.getController().checkAndAutoCreateContents(db, newSiteNode.getId(), masterLanguageId, masterLanguageId, null, new Integer(componentId), document, new Integer(componentContentId), principal);
    				componentStructure = XMLHelper.serializeDom(document, new StringBuffer()).toString();
    			}
        	}

        	//Create initial content version also... in masterlanguage
        	String versionValue = "<?xml version='1.0' encoding='UTF-8'?><article xmlns=\"x-schema:ArticleSchema.xml\"><attributes><Title><![CDATA[" + newSiteNode.getName() + "]]></Title><NavigationTitle><![CDATA[" + newSiteNode.getName() + "]]></NavigationTitle><NiceURIName><![CDATA[" + new VisualFormatter().replaceNiceURINonAsciiWithSpecifiedChars(newSiteNode.getName(), CmsPropertyHandler.getNiceURIDefaultReplacementCharacter()) + "]]></NiceURIName><Description><![CDATA[" + newSiteNode.getName() + "]]></Description><MetaInfo><![CDATA[" + newSiteNode.getName() + "]]></MetaInfo><ComponentStructure><![CDATA[" + componentStructure + "]]></ComponentStructure></attributes></article>";
        	ContentVersionVO contentVersionVO = new ContentVersionVO();
        	contentVersionVO.setVersionComment("Autogenerated version");
        	contentVersionVO.setVersionModifier(principal.getName());
        	contentVersionVO.setVersionValue(versionValue);
        	ContentVersionController.getContentVersionController().create(contentVO.getId(), masterLanguage.getId(), contentVersionVO, null, db);

        	//Also created a version in the local master language for this part of the site if any
        	LanguageVO localMasterLanguageVO = getInitialLanguageVO(db, parentFolderContent.getId(), repositoryId);
        	if(localMasterLanguageVO.getId().intValue() != masterLanguage.getId().intValue())
        	{
	        	String versionValueLocalMaster = "<?xml version='1.0' encoding='UTF-8'?><article xmlns=\"x-schema:ArticleSchema.xml\"><attributes><Title><![CDATA[" + newSiteNode.getName() + "]]></Title><NavigationTitle><![CDATA[" + newSiteNode.getName() + "]]></NavigationTitle><NiceURIName><![CDATA[" + new VisualFormatter().replaceNiceURINonAsciiWithSpecifiedChars(newSiteNode.getName(), CmsPropertyHandler.getNiceURIDefaultReplacementCharacter()) + "]]></NiceURIName><Description><![CDATA[" + newSiteNode.getName() + "]]></Description><MetaInfo><![CDATA[" + newSiteNode.getName() + "]]></MetaInfo><ComponentStructure><![CDATA[]]></ComponentStructure></attributes></article>";
	            ContentVersionVO contentVersionVOLocalMaster = new ContentVersionVO();
	        	contentVersionVOLocalMaster.setVersionComment("Autogenerated version");
	        	contentVersionVOLocalMaster.setVersionModifier(principal.getName());
	        	contentVersionVOLocalMaster.setVersionValue(versionValueLocalMaster);
	        	ContentVersionController.getContentVersionController().create(contentVO.getId(), localMasterLanguageVO.getId(), contentVersionVOLocalMaster, null, db);
        	}
        	
        	ServiceBindingVO serviceBindingVO = new ServiceBindingVO();
        	serviceBindingVO.setName(newSiteNode.getName() + " Metainfo");
        	serviceBindingVO.setPath("/None specified/");
        
        	String qualifyerXML = "<?xml version='1.0' encoding='UTF-8'?><qualifyer><contentId>" + contentVO.getId() + "</contentId></qualifyer>";
        
        	ServiceBindingController.getController().create(db, serviceBindingVO, qualifyerXML, availableServiceBindingId, siteNodeVersion.getId(), singleServiceDefinitionVO.getId());	
        }

        return content;
    }

	public LanguageVO getInitialLanguageVO(Database db, Integer contentId, Integer repositoryId) throws Exception
	{
		Map args = new HashMap();
	    args.put("globalKey", "infoglue");
	    PropertySet ps = PropertySetManager.getInstance("jdbc", args);

	    String initialLanguageId = ps.getString("content_" + contentId + "_initialLanguageId");
	    Content content = ContentController.getContentController().getContentWithId(contentId, db);
	    Content parentContent = content.getParentContent(); 
	    while((initialLanguageId == null || initialLanguageId.equalsIgnoreCase("-1")) && parentContent != null)
	    {
	        initialLanguageId = ps.getString("content_" + parentContent.getId() + "_initialLanguageId");
		    parentContent = parentContent.getParentContent(); 
	    }
	    
	    if(initialLanguageId != null && !initialLanguageId.equals("") && !initialLanguageId.equals("-1"))
	        return LanguageController.getController().getLanguageVOWithId(new Integer(initialLanguageId));
	    else
	        return LanguageController.getController().getMasterLanguage(repositoryId);
	}

	/**
	 * Recursive methods to get all sitenodes under the specific sitenode.
	 */ 
	
    public List getSiteNodeVOWithParentRecursive(Integer siteNodeId) throws ConstraintException, SystemException
	{
		return getSiteNodeVOWithParentRecursive(siteNodeId, new ArrayList());
	}
	
	private List getSiteNodeVOWithParentRecursive(Integer siteNodeId, List resultList) throws ConstraintException, SystemException
	{
		// Get the versions of this content.
		resultList.add(getSiteNodeVOWithId(siteNodeId));
		
		// Get the children of this content and do the recursion
		List childSiteNodeList = SiteNodeController.getController().getSiteNodeChildren(siteNodeId);
		Iterator cit = childSiteNodeList.iterator();
		while (cit.hasNext())
		{
		    SiteNodeVO siteNodeVO = (SiteNodeVO) cit.next();
			getSiteNodeVOWithParentRecursive(siteNodeVO.getId(), resultList);
		}
	
		return resultList;
	}


    public void setMetaInfoContentId(Integer siteNodeId, Integer metaInfoContentId) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
        	setMetaInfoContentId(siteNodeId, metaInfoContentId, db);
        	
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

    }       

    public void setMetaInfoContentId(Integer siteNodeId, Integer metaInfoContentId, Database db) throws ConstraintException, SystemException
    {
        SiteNode siteNode = getSiteNodeWithId(siteNodeId, db);
        siteNode.setMetaInfoContentId(metaInfoContentId);
    }       
    
    
    public List getSiteNodeVOListWithoutMetaInfoContentId() throws ConstraintException, SystemException
    {
		List siteNodeVOList = new ArrayList();

		Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
            List siteNodes = getSiteNodesWithoutMetaInfoContentId(db);
            siteNodeVOList = toVOList(siteNodes);
            
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
        
        return siteNodeVOList;
    }       

    public List getSiteNodesWithoutMetaInfoContentId(Database db) throws ConstraintException, SystemException, Exception
    {
		List siteNodes = new ArrayList();

		OQLQuery oql = db.getOQLQuery("SELECT sn FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl sn WHERE sn.metaInfoContentId = $1");
    	oql.bind(new Integer(-1));
    	
    	QueryResults results = oql.execute();
		
		while(results.hasMore()) 
        {
        	SiteNode siteNode = (SiteNodeImpl)results.next();
        	siteNodes.add(siteNode);
        }

		results.close();
		oql.close();

		return siteNodes;
    }


    public SiteNodeVO getSiteNodeVOWithMetaInfoContentId(Integer contentId) throws ConstraintException, SystemException
    {
		SiteNodeVO siteNodeVO = null;

		Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
        	SiteNode siteNode = getSiteNodeWithMetaInfoContentId(db, contentId);
        	siteNodeVO = siteNode.getValueObject();
        	
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
        
        return siteNodeVO;
    }       

    public SiteNode getSiteNodeWithMetaInfoContentId(Database db, Integer contentId) throws ConstraintException, SystemException, Exception
    {
		SiteNode siteNode = null;

		OQLQuery oql = db.getOQLQuery("SELECT sn FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl sn WHERE sn.metaInfoContentId = $1");
    	oql.bind(contentId);
    	
    	QueryResults results = oql.execute();
		
		if(results.hasMore()) 
        {
			siteNode = (SiteNodeImpl)results.next();
        }

		results.close();
		oql.close();

		return siteNode;
    }
    
	/**
	 * This method returns true if the if the siteNode in question is protected.
	 */
    
	public Integer getProtectedSiteNodeVersionId(Integer siteNodeId)
	{
		Integer protectedSiteNodeVersionId = null;
	
		try
		{
			SiteNodeVersionVO siteNodeVersionVO = SiteNodeVersionController.getController().getLatestSiteNodeVersionVO(siteNodeId);

			if(siteNodeVersionVO.getIsProtected() != null)
			{	
				if(siteNodeVersionVO.getIsProtected().intValue() == NO.intValue())
					protectedSiteNodeVersionId = null;
				else if(siteNodeVersionVO.getIsProtected().intValue() == YES.intValue())
					protectedSiteNodeVersionId = siteNodeVersionVO.getId();
				else if(siteNodeVersionVO.getIsProtected().intValue() == INHERITED.intValue())
				{
					SiteNodeVO parentSiteNodeVO = getParentSiteNode(siteNodeId);
					if(parentSiteNodeVO != null)
						protectedSiteNodeVersionId = getProtectedSiteNodeVersionId(parentSiteNodeVO.getId()); 
				}
			}
		}
		catch(Exception e)
		{
			logger.warn("An error occurred trying to get which (if any) site node is protected:" + e.getMessage(), e);
		}
			
		return protectedSiteNodeVersionId;
	}

	public String getSiteNodePath(Integer siteNodeId) throws Exception
	{
		String result;
		Database db = CastorDatabaseService.getDatabase();
        try
        {
        	beginTransaction(db);
			result = getSiteNodePath(siteNodeId, db);
            commitTransaction(db);
        }
        catch(Exception ex)
        {
            logger.error("An error occurred so we should not complete the transaction:" + ex);
            logger.warn("An error occurred so we should not complete the transaction", ex);
            rollbackTransaction(db);
            throw new SystemException(ex.getMessage());
        }
		return result;
	}

	public String getSiteNodePath(Integer siteNodeId, Database db) throws Exception
	{
		return getSiteNodePath(siteNodeId, true, false, db);
	}

	public String getSiteNodePath(Integer siteNodeId, boolean includeRootSiteNode, boolean includeRepositoryName, Database db) throws Exception
	{
		SiteNodeVO siteNodeVO = getSiteNodeVOWithId(siteNodeId, db);
		return getSiteNodePath(siteNodeVO, includeRootSiteNode, includeRepositoryName, db);
	}

	public String getSiteNodePath(SiteNodeVO siteNodeVO, boolean includeRootSiteNode, boolean includeRepositoryName, Database db) throws Exception
	{
		StringBuffer sb = new StringBuffer();

		RepositoryVO repositoryVO = RepositoryController.getController().getRepositoryVOWithId(siteNodeVO.getRepositoryId(), db);
		while(siteNodeVO != null)
		{
			if (includeRootSiteNode || siteNodeVO.getParentSiteNodeId() != null)
			{
				sb.insert(0, "/" + siteNodeVO.getName());
			}
			if(siteNodeVO.getParentSiteNodeId() != null)
				siteNodeVO = getSiteNodeVOWithId(siteNodeVO.getParentSiteNodeId(), db);
			else
				siteNodeVO = null;
		}

		if (includeRepositoryName)
		{
			if(repositoryVO != null)
				sb.insert(0, repositoryVO.getName() + " - ");
		}

		return sb.toString();
	}

	/**
	 * Gets the given SiteNode's path as a comma separated list (e.g. 123,654,999) where the right most
	 * id is the provided SiteNode-id and the the next id, to the left, is its parent and so on up to
	 * the ancestor of the given SiteNode-id that has no parent SiteNode.
	 * @param siteNodeId
	 * @return
	 * @throws SystemException
	 */
	public String getSiteNodeIdsAsCommaSeperatedString(Integer siteNodeId) throws SystemException
	{
		String siteNodePath = null;
		Database db = null;

		try
		{
			db = CastorDatabaseService.getDatabase();
			beginTransaction(db);
			siteNodePath = getSiteNodeIdsAsCommaSeperatedString(siteNodeId, db);
			commitTransaction(db);
		}
		catch(Exception ex)
		{
			logger.error("An error occurred when computing the siteNode-id path so we should not complete the transaction. Message: " + ex.getMessage());
			logger.warn("An error occurred when computing the siteNode-id path so we should not complete the transaction.", ex);
			rollbackTransaction(db);
			return null;
		}

		return siteNodePath;
	}

	@SuppressWarnings("static-access")
	public String getSiteNodeIdsAsCommaSeperatedString(Integer siteNodeId, Database db) throws Exception
	{
		StringBuffer sb = new StringBuffer();

		SiteNodeVO siteNodeVO = getSiteNodeVOWithId(siteNodeId, db);

		sb.insert(0, siteNodeVO.getSiteNodeId());

		while (siteNodeVO.getParentSiteNodeId() != null)
		{
			siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeVO.getParentSiteNodeId(), db);
			sb.insert(0, siteNodeVO.getSiteNodeId() + ",");
		}

		return sb.toString();
	}

	public List<SiteNodeVO> getUpcomingExpiringSiteNodes(int numberOfWeeks) throws Exception
	{
		List<SiteNodeVO> siteNodeVOList = new ArrayList<SiteNodeVO>();

		Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
    		OQLQuery oql = db.getOQLQuery("SELECT sn FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl sn WHERE " +
    				"sn.expireDateTime > $1 AND sn.expireDateTime < $2 AND sn.publishDateTime < $3");

        	Calendar now = Calendar.getInstance();
        	Date currentDate = now.getTime();
        	oql.bind(currentDate);
        	now.add(Calendar.DAY_OF_YEAR, numberOfWeeks);
        	Date futureDate = now.getTime();
           	oql.bind(futureDate);
           	oql.bind(currentDate);

        	QueryResults results = oql.execute(Database.ReadOnly);
    		while(results.hasMore()) 
            {
    			SiteNode siteNode = (SiteNodeImpl)results.next();
    			siteNodeVOList.add(siteNode.getValueObject());
            }

    		results.close();
    		oql.close();
        	
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
        
        return siteNodeVOList;
	}

	/**
	 * This method are here to return all content versions that are x number of versions behind the current active version. This is for cleanup purposes.
	 * 
	 * @param numberOfVersionsToKeep
	 * @return
	 * @throws SystemException 
	 */
	
	public int cleanSiteNodeVersions(int numberOfVersionsToKeep, boolean keepOnlyOldPublishedVersions, long minimumTimeBetweenVersionsDuringClean, boolean deleteVersions) throws SystemException 
	{
		int cleanedVersions = 0;
		
		int batchLimit = 20;

		List<SiteNodeVersionVO> siteNodeVersionVOList = getSiteNodeVersionVOList(numberOfVersionsToKeep, keepOnlyOldPublishedVersions, minimumTimeBetweenVersionsDuringClean);
			
		logger.info("Deleting " + siteNodeVersionVOList.size() + " versions");
		int maxIndex = (siteNodeVersionVOList.size() > batchLimit ? batchLimit : siteNodeVersionVOList.size());
		List partList = siteNodeVersionVOList.subList(0, maxIndex);
		while(partList.size() > 0)
		{
			if(deleteVersions)
				cleanVersions(numberOfVersionsToKeep, partList);
			cleanedVersions = cleanedVersions + partList.size();
			partList.clear();
			maxIndex = (siteNodeVersionVOList.size() > batchLimit ? batchLimit : siteNodeVersionVOList.size());
			partList = siteNodeVersionVOList.subList(0, maxIndex);
		}

		return cleanedVersions;
	}
	
	
	/**
	 * This method are here to return all content versions that are x number of versions behind the current active version. This is for cleanup purposes.
	 * 
	 * @param numberOfVersionsToKeep
	 * @return
	 * @throws SystemException 
	 */
	
	public List<SiteNodeVersionVO> getSiteNodeVersionVOList(int numberOfVersionsToKeep, boolean keepOnlyOldPublishedVersions, long minimumTimeBetweenVersionsDuringClean) throws SystemException 
	{
		logger.info("numberOfVersionsToKeep:" + numberOfVersionsToKeep);

		Database db = CastorDatabaseService.getDatabase();
    	
    	List<SiteNodeVersionVO> siteNodeVersionsIdList = new ArrayList();

        beginTransaction(db);

        try
        {
            OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeVersionImpl snv ORDER BY snv.siteNodeId, snv.siteNodeVersionId desc");
        	
        	QueryResults results = oql.execute(Database.ReadOnly);
			
        	int numberOfLaterVersions = 0;
        	Integer previousSiteNodeId = null;
        	Date previousDate = null;
        	long difference = -1;
        	List keptSiteNodeVersionVOList = new ArrayList();
        	List potentialSiteNodeVersionVOList = new ArrayList();
        	List versionInitialSuggestions = new ArrayList();
        	List versionNonPublishedSuggestions = new ArrayList();

        	while (results.hasMore())
            {
        		SmallSiteNodeVersionImpl version = (SmallSiteNodeVersionImpl)results.next();
				if(previousSiteNodeId != null && previousSiteNodeId.intValue() != version.getSiteNodeId().intValue())
				{
					logger.info("previousSiteNodeId:" + previousSiteNodeId);
					if(minimumTimeBetweenVersionsDuringClean != -1 && versionInitialSuggestions.size() > numberOfVersionsToKeep)
					{						
						Iterator potentialSiteNodeVersionVOListIterator = potentialSiteNodeVersionVOList.iterator();
						while(potentialSiteNodeVersionVOListIterator.hasNext())
						{
							SiteNodeVersionVO potentialSiteNodeVersionVO = (SiteNodeVersionVO)potentialSiteNodeVersionVOListIterator.next();
							
							SiteNodeVersionVO firstInitialSuggestedSiteNodeVersionVO = null;
							Iterator versionInitialSuggestionsIterator = versionInitialSuggestions.iterator();
							while(versionInitialSuggestionsIterator.hasNext())
							{
								SiteNodeVersionVO initialSuggestedSiteNodeVersionVO = (SiteNodeVersionVO)versionInitialSuggestionsIterator.next();
								if(initialSuggestedSiteNodeVersionVO.getStateId().equals(ContentVersionVO.PUBLISHED_STATE))
								{
									firstInitialSuggestedSiteNodeVersionVO = initialSuggestedSiteNodeVersionVO;
									break;
								}
							}
							
							if(firstInitialSuggestedSiteNodeVersionVO != null)
							{
								keptSiteNodeVersionVOList.remove(potentialSiteNodeVersionVO);
								keptSiteNodeVersionVOList.add(firstInitialSuggestedSiteNodeVersionVO);
								versionInitialSuggestions.remove(firstInitialSuggestedSiteNodeVersionVO);
								versionInitialSuggestions.add(potentialSiteNodeVersionVO);
							}
						}
					}
					
					siteNodeVersionsIdList.addAll(versionNonPublishedSuggestions);
					siteNodeVersionsIdList.addAll(versionInitialSuggestions);
					potentialSiteNodeVersionVOList.clear();
					versionInitialSuggestions.clear();
					versionNonPublishedSuggestions.clear();
					keptSiteNodeVersionVOList.clear();
					
					numberOfLaterVersions = 0;
					previousDate = null;
					difference = -1;
					potentialSiteNodeVersionVOList = new ArrayList();
				}
				else if(previousDate != null)
				{
					difference = previousDate.getTime() - version.getModifiedDateTime().getTime();
				}
				
				if(numberOfLaterVersions > numberOfVersionsToKeep || (keepOnlyOldPublishedVersions && numberOfLaterVersions > 0 && !version.getStateId().equals(ContentVersionVO.PUBLISHED_STATE)))
            	{
					if(version.getStateId().equals(ContentVersionVO.PUBLISHED_STATE))
					{
						versionInitialSuggestions.add(version.getValueObject());
					}
					else
					{
						versionNonPublishedSuggestions.add(version.getValueObject());
					}
            	}
				else if(previousDate != null && difference != -1 && difference < minimumTimeBetweenVersionsDuringClean)
				{
					keptSiteNodeVersionVOList.add(version.getValueObject());
					potentialSiteNodeVersionVOList.add(version.getValueObject());		
					numberOfLaterVersions++;
				}
				else
				{
					keptSiteNodeVersionVOList.add(version.getValueObject());
					previousDate = version.getModifiedDateTime();
					numberOfLaterVersions++;
				}

				previousSiteNodeId = version.getSiteNodeId();
            }
            
			results.close();
			oql.close();

			commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
        
		return siteNodeVersionsIdList;
	}

	/**
	 * Cleans the list of versions - even published ones. Use with care only for cleanup purposes.
	 * 
	 * @param numberOfVersionsToKeep
	 * @param contentVersionVOList
	 * @throws SystemException
	 */
	
	private void cleanVersions(int numberOfVersionsToKeep, List siteNodeVersionVOList) throws SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
    	
        beginTransaction(db);

        try
        {
			Iterator<SiteNodeVersionVO> siteNodeVersionVOListIterator = siteNodeVersionVOList.iterator();
			while(siteNodeVersionVOListIterator.hasNext())
			{
				SiteNodeVersionVO siteNodeVersionVO = siteNodeVersionVOListIterator.next();
				SiteNodeVersion siteNodeVersion = getSiteNodeVersionWithId(siteNodeVersionVO.getId(), db);
				logger.info("Deleting the siteNodeVersion " + siteNodeVersion.getId() + " on siteNode " + siteNodeVersion.getOwningSiteNode());
				delete(siteNodeVersion, db, true);
			}

			commitTransaction(db);

			Thread.sleep(1000);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
        finally
        {
        	closeDatabase(db);
        }
	}

	/**
	 * This method deletes an contentversion and notifies the owning content.
	 */
	
 	public void delete(SiteNodeVersion siteNodeVersion, Database db, boolean forceDelete) throws ConstraintException, SystemException, Exception
	{
		if (!forceDelete && siteNodeVersion.getStateId().intValue() == ContentVersionVO.PUBLISHED_STATE.intValue() && siteNodeVersion.getIsActive().booleanValue() == true)
		{
			throw new ConstraintException("SiteNodeVersion.stateId", "3300", siteNodeVersion.getOwningSiteNode().getName());
		}
		
		ServiceBindingController.deleteServiceBindingsReferencingSiteNodeVersion(siteNodeVersion, db);

		SiteNode siteNode = siteNodeVersion.getOwningSiteNode();

		if(siteNode != null)
			siteNode.getSiteNodeVersions().remove(siteNodeVersion);

		db.remove(siteNodeVersion);
	}

	private Map<String, List<ReferenceBean>> groupByContactPerson(List<ReferenceBean> contactPersons)
	{
		Map<String, List<ReferenceBean>> result = new HashMap<String, List<ReferenceBean>>();

		for (ReferenceBean referenceBean : contactPersons)
		{
			if (referenceBean.getContactPersonEmail() != null && !referenceBean.getContactPersonEmail().equals(""))
			{
				logger.debug("Found contact person on reference for " + referenceBean.getPath() + ". Person: " + referenceBean.getContactPersonEmail());
				addReferenceToContactPersonList(result, referenceBean, referenceBean.getContactPersonEmail());
			}
			for (ReferenceVersionBean referenceVersion : referenceBean.getVersions())
			{
				if (referenceVersion.getContactPersonEmail() != null && !referenceVersion.getContactPersonEmail().equals(""))
				{
					logger.debug("Found contact person on reference version for " + referenceBean.getPath() + ". Person: " + referenceVersion.getContactPersonEmail());
					addReferenceToContactPersonList(result, referenceBean, referenceVersion.getContactPersonEmail());
					break; // Don't add the ReferenceBean for every version.
				}
			}
			for (String concernedPerson : referenceBean.getConcernedPeople())
			{
				logger.debug("Found concerned person for " + referenceBean.getPath() + ". Person: " + concernedPerson);
				addReferenceToContactPersonList(result, referenceBean, concernedPerson);
			}
		}

		return result;
	}

 	private void addReferenceToContactPersonList(Map<String, List<ReferenceBean>> result, ReferenceBean referenceBean, String person)
	{
		List<ReferenceBean> personsList = result.get(person);
		if (personsList == null)
		{
			logger.debug("First reference for person: " + person);
			personsList = new ArrayList<ReferenceBean>();
			result.put(person, personsList);
		}
		personsList.add(referenceBean);
	}

	private Map<String, Map<String, List<ReferenceBean>>> groupByContactPerson(Map<String, List<ReferenceBean>> contactPersons)
	{
		Map<String, Map<String, List<ReferenceBean>>> result = new HashMap<String, Map<String,  List<ReferenceBean>>>();
		for (Map.Entry<String, List<ReferenceBean>> entry : contactPersons.entrySet())
		{
			String siteNodePath = entry.getKey();
			Map<String, List<ReferenceBean>> referencesByContact = groupByContactPerson(entry.getValue());
			for (Map.Entry<String, List<ReferenceBean>> contactsForSiteNode : referencesByContact.entrySet())
			{
				String contactPerson = contactsForSiteNode.getKey();
				Map<String, List<ReferenceBean>> value = result.get(contactPerson);
				if (value == null)
				{
					logger.debug("First instance of contact person: " + contactPerson);
					value = new HashMap<String, List<ReferenceBean>>();
					result.put(contactPerson, value);
				}
				if (logger.isDebugEnabled())
				{
					logger.debug("Putting value: " + contactsForSiteNode.getValue() + " for contact person: " + contactPerson);
				}
				value.put(siteNodePath, contactsForSiteNode.getValue());
			}
		}
		return result;
	}

	/*default*/ void notifyContactPersonsForSiteNode(String siteNodePath, List<ReferenceBean> contacts, Database db) throws SystemException, Exception
	{
		notifyContactPersonsForSiteNode(Collections.singletonMap(siteNodePath, contacts), db);
	}

	/*default*/ void notifyContactPersonsForSiteNode(Map<String, List<ReferenceBean>> contactPersons) throws SystemException
	{
		if (contactPersons.size() > 0)
		{
			Database contactDb = CastorDatabaseService.getDatabase();
			try
			{
				beginTransaction(contactDb);
				notifyContactPersonsForSiteNode(contactPersons, contactDb);
				commitTransaction(contactDb);
			}
			catch(Throwable tr)
			{
				rollbackTransaction(contactDb);
				logger.error("An error occurred so we should not contact people about SiteNode removal. Message: " + tr.getMessage());
				logger.warn("An error occurred so we should not contact people about SiteNode removal.", tr);
				throw new SystemException(tr.getMessage());
			}
		}
	}

	/*default*/ void notifyContactPersonsForSiteNode(Map<String, List<ReferenceBean>> contacts, Database db) throws SystemException, Exception
    {
    	if (contacts.size() == 0)
    	{
    		return;
    	}
    	
    	Map<String, Map<String, List<ReferenceBean>>> contactMap = groupByContactPerson(contacts);

    	if (logger.isInfoEnabled())
    	{
    		logger.info("Will notify people about registry change. " + contactMap);
    	}

    	String registryContactMailLanguage = CmsPropertyHandler.getRegistryContactMailLanguage();
    	Locale locale = new Locale(registryContactMailLanguage);

		try
		{
			String from = CmsPropertyHandler.getSystemEmailSender();
    		String subject = getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.subject");
			// This loop iterate once for each contact person
			for (Map.Entry<String, Map<String, List<ReferenceBean>>> entry : contactMap.entrySet())
			{
				String contactPersonEmail = entry.getKey();
				Set<String> siteNodesForPerson = entry.getValue().keySet();
				Map<String, List<ReferenceBean>> affectedNodes = entry.getValue();

	    		StringBuilder mailContent = new StringBuilder();

	    		mailContent.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.intro"));
	    		mailContent.append("<p style=\"color:black !important;\">");
	    		mailContent.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.siteNodeLabel"));
	    		mailContent.append("<ul>");
	    		for (String siteNodePath : siteNodesForPerson)
	    		{
					mailContent.append("<li>");
	    			mailContent.append(siteNodePath);
	    			mailContent.append("</li>");
	    		}
				mailContent.append("</ul>");
				mailContent.append("</p>");

				boolean hasInformation = false;
		    	for (Map.Entry<String, List<ReferenceBean>> affectedNode : affectedNodes.entrySet())
		    	{
					StringBuilder sb = new StringBuilder();
					sb.append("<h3 style=\"margin-top:30px;color:black !important;\">");
					sb.append("<a style=\"color:black;\">");
					sb.append(affectedNode.getKey());
					sb.append("</a></h3>");

					String path;
					String url;
					StringBuilder siteNodeBuilder = new StringBuilder();
					StringBuilder contentBuilder = new StringBuilder();
					for (ReferenceBean reference : affectedNode.getValue())
					{
						if (reference.getPath() != null && !reference.getPath().equals(""))
						{
							path = reference.getPath();
						}
						else
						{
							path = reference.getName();
						}

						if (reference.getReferencingCompletingObject().getClass().getName().indexOf("Content") != -1)
						{
							contentBuilder.append("<li><h4><a style=\"color:black !important;\">");
							contentBuilder.append(path);
							contentBuilder.append("</a></h4>");
							contentBuilder.append("<ul>");
							for (ReferenceVersionBean referenceVersion : reference.getVersions())
							{
								// If-logic: If the person is responsible for this version OR if the person is responsible for a SiteNode that uses this Content
								if (referenceVersion.getContactPersonEmail().equals(contactPersonEmail) || reference.getConcernedPeople().contains(contactPersonEmail))
								{
									if (referenceVersion.getReferencingObject() instanceof ContentVersionVO)
									{
										ContentVersionVO contentVersionVO = (ContentVersionVO)referenceVersion.getReferencingObject();
										url = CmsPropertyHandler.getCmsFullBaseUrl() + "/ViewContentVersion!standalone.action?contentId=" + ((ContentVO)reference.getReferencingCompletingObject()).getContentId() + "&languageId=" + contentVersionVO.getLanguageId();
										contentBuilder.append("<li>");
										contentBuilder.append("<a href=\"" + url + "\">");
										LanguageVO contentVersionLanguageVO = LanguageController.getController().getLanguageVOWithId(contentVersionVO.getLanguageId(), db);
										Locale contentVersionLocale = new Locale(contentVersionLanguageVO.getLanguageCode());
										contentBuilder.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.editLanguageVersion", contentVersionLocale.getDisplayLanguage(locale)));
										contentBuilder.append("</a>");
										if (reference.getConcernedPeople().size() > 0)
										{
											contentBuilder.append("<p>").append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.concerendPeople")).append("<ul>");
											if (!referenceVersion.getContactPersonEmail().equals(contactPersonEmail))
											{
												contentBuilder.append("<li>");
												contentBuilder.append(referenceVersion.getContactPersonEmail());
												contentBuilder.append("</li>");
											}
											for (String concernedPerson : reference.getConcernedPeople())
											{
												if (!concernedPerson.equals(contactPersonEmail))
												{
													contentBuilder.append("<li>");
													contentBuilder.append(concernedPerson);
													contentBuilder.append("</li>");
												}
											}
											contentBuilder.append("</ul></p>");

										}
										contentBuilder.append("</li>");
									}
								}
								else
								{
									if (logger.isDebugEnabled())
									{
										logger.debug("The current person is not concerned with the current version of the Content. ContentVersion: " + referenceVersion.getReferencingObject());
									}
								}
							}
							contentBuilder.append("</ul>");
							contentBuilder.append("</li>");
						}
						else
						{
							url = CmsPropertyHandler.getCmsFullBaseUrl() + "/DeleteContent!fixPage.action?siteNodeId=" + ((SiteNodeVO)reference.getReferencingCompletingObject()).getSiteNodeId() + "&contentId=-1";
							siteNodeBuilder.append("<li><a href=\"" + url + "\">" + path + "</a></li>");
						}
					}
					if (contentBuilder.length() > 0)
					{
						hasInformation = true;
						sb.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.listHeader.content"));
						sb.append("<ul>");
						sb.append(contentBuilder);
						sb.append("</ul>");
					}
					if (siteNodeBuilder.length() > 0)
					{
						hasInformation = true;
						sb.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.listHeader.siteNode"));
						sb.append("<ul>");
						sb.append(siteNodeBuilder);
						sb.append("</ul>");
					}
					sb.append("</p>");
					mailContent.append(sb);
				} // end loop: one SiteNode for one contact person

		    	mailContent.append(getLocalizedString(locale, "tool.structuretool.registry.notificationEmail.footer"));
				if (hasInformation)
				{
					logger.debug("Sending notification email to: " + contactPersonEmail);
					MailServiceFactory.getService().sendEmail("text/html", from, contactPersonEmail, null, null, subject, mailContent.toString(), "utf-8");
				}
				else
				{
					logger.warn("No Contents or SiteNodes were found for the given person. This is very strange. Contact person: " + contactPersonEmail + ", SiteNode.ids: " + contacts.keySet());
				}
			} // end-loop: contact person
    	}
    	catch (Exception ex)
    	{
    		logger.error("Failed to generate email for contact person notfication. Message: " + ex.getMessage() + ". Type: " + ex.getClass());
			logger.warn("Failed to generate email for contact person notfication.", ex);
			throw ex;
    	}
    }
}
 
