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
 *
 * $Id: RegistryController.java,v 1.37.4.2 2013/05/07 13:03:45 ss Exp $
 */

package org.infoglue.cms.controllers.kernel.impl.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.databeans.ReferenceBean;
import org.infoglue.cms.applications.databeans.ReferenceVersionBean;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Registry;
import org.infoglue.cms.entities.management.RegistryVO;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.entities.management.impl.simple.RegistryImpl;
import org.infoglue.cms.entities.structure.Qualifyer;
import org.infoglue.cms.entities.structure.ServiceBinding;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.SiteNodeVersionVO;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;

import com.sun.star.lang.IllegalArgumentException;


/**
 * The RegistryController manages the registry-parts of InfoGlue. 
 * The Registry is metadata for how things are related - especially to handle bindings and inline links etc
 * when looking them up in the model is to slow.
 *
 * @author Mattias Bogeblad
 */

public class RegistryController extends BaseController
{
    private final static Logger logger = Logger.getLogger(RegistryController.class.getName());

	private static final RegistryController instance = new RegistryController();
	
	public static RegistryController getController()
	{ 
	    return instance; 
	}

	private RegistryController()
	{
	}
	
    @SuppressWarnings("unchecked")
	public List<RegistryVO> getRegistryVOList() throws SystemException, Bug
    {
        return getAllVOObjects(RegistryImpl.class, "registryId");
    }

    @SuppressWarnings("unchecked")
	public List<RegistryVO> getRegistryVOList(Database db) throws SystemException, Bug
    {
        return getAllVOObjects(RegistryImpl.class, "registryId", db);
    }
    
	/**
	 * This method return a RegistryVO
	 */
	
	public RegistryVO getRegistryVOWithId(Integer registryId) throws SystemException, Exception
	{
		RegistryVO registryVO = (RegistryVO)getVOWithId(RegistryImpl.class, registryId);

		return registryVO;
	}

	public RegistryVO getRegistryVOWithId(Integer registryId, Database db) throws SystemException, Exception
	{
		RegistryVO registryVO = (RegistryVO)getVOWithId(RegistryImpl.class, registryId, db);

		return registryVO;
	}

	
	/**
	 * This method creates a registry entity in the db.
	 * @param valueObject
	 * @return
	 * @throws ConstraintException
	 * @throws SystemException
	 */
	
    public RegistryVO create(RegistryVO valueObject, Database db) throws ConstraintException, SystemException, Exception
    {
        Registry registry = new RegistryImpl();
        registry.setValueObject(valueObject);
        db.create(registry);
        return registry.getValueObject();
    }     

    /**
     * This method updates a registry entry
     * @param vo
     * @return
     * @throws ConstraintException
     * @throws SystemException
     */
    
    public RegistryVO update(RegistryVO valueObject, Database db) throws ConstraintException, SystemException
    {
    	return (RegistryVO) updateEntity(RegistryImpl.class, (BaseEntityVO) valueObject, db);
    }    
    
    
    /**
     * This method deletes a registry entry
     * @return registryId
     * @throws ConstraintException
     * @throws SystemException
     */
    
    public void delete(Integer registryId) throws ConstraintException, SystemException
    {
    	deleteEntity(RegistryImpl.class, registryId);
    }

	public void delete(Integer registryId, Database db) throws ConstraintException, SystemException
	{
    	deleteEntity(RegistryImpl.class, registryId, db);
	}

    public List<ReferenceBean> delete(String[] registryIds, InfoGluePrincipal principal, boolean clean) throws ConstraintException, SystemException
    {
    	Database db = CastorDatabaseService.getDatabase();
		List<ReferenceBean> references = null;
		try
		{
			beginTransaction(db);

			references = delete(registryIds, principal, clean, db);

		    commitTransaction(db);
		}
		catch (Throwable e)
		{
		    logger.error("Failed to delete registries in list: " + Arrays.toString(registryIds) + ". Message: " + e.getMessage());
		    rollbackTransaction(db);
		}
		return references;
    }

    public List<ReferenceBean> delete(String[] registryIds, InfoGluePrincipal principal, boolean clean, Database db) throws Throwable
    {
    	if (clean)
    	{
	    	Map<ContentVersionVO, RegistryVO> contentVersionRegistryPair = extractContentVersionsFromRegistryList(registryIds, db);
			InconsistenciesController.getController().removeContentReferences(contentVersionRegistryPair, principal, db);
			Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = extractSiteNodesFromRegistryList(registryIds, db);
			InconsistenciesController.getController().removeSiteNodeReferences(siteNodeRegistryPair, principal, db);
    	}
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();
		List<ReferenceBean> references = new ArrayList<ReferenceBean>();
		Map<String,Boolean> checkedLanguageVersions = new HashMap<String, Boolean>();
		for (String registryIdString : registryIds)
    	{
			RegistryVO registryVO = getRegistryVOWithId(new Integer(registryIdString), db);
			if (logger.isInfoEnabled())
			{
				logger.info("About to remove registry bean. Referencing-type: " + registryVO.getReferencingEntityName() + ", referencing-id: " + registryVO.getReferencingEntityId());
			}
			ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, checkedLanguageVersions, db);
			if (referenceBean != null)
			{
				references.add(referenceBean);
			}
			if (db.isPersistent(registryVO))
			{
				delete(registryVO.getRegistryId(), db);
			}
    	}
		return references;
    }

    private Map<ContentVersionVO, RegistryVO> extractContentVersionsFromRegistryList(List<RegistryVO> registryVOs, Database db) throws Exception
    {
		Map<ContentVersionVO, RegistryVO> versionRegistryPair = new HashMap<ContentVersionVO, RegistryVO>();
    	for (RegistryVO registryVO : registryVOs)
		{
			extractContentVersionFromRegistry(versionRegistryPair, registryVO, db);
		}
    	if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + versionRegistryPair.size() + " ContentVersions from " + registryVOs.size() + " registry entries");
		}
    	return versionRegistryPair;
    }

	private Map<ContentVersionVO, RegistryVO> extractContentVersionsFromRegistryList(String[] registryIds, Database db) throws Throwable
	{
		Map<ContentVersionVO, RegistryVO> versionRegistryPair = new HashMap<ContentVersionVO, RegistryVO>();

		Integer registryId;
		RegistryVO registryVO;
		for (String registryIdString : registryIds)
		{
			registryId = new Integer(registryIdString);
			registryVO = RegistryController.getController().getRegistryVOWithId(registryId, db);
			extractContentVersionFromRegistry(versionRegistryPair, registryVO, db);
		}

		if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + versionRegistryPair.size() + " ContentVersions from " + registryIds.length + " registry entries");
		}
		return versionRegistryPair;
	}

	private void extractContentVersionFromRegistry(Map<ContentVersionVO, RegistryVO> versionRegistryPair, RegistryVO registryVO, Database db) throws SystemException, Bug, Exception
	{
		String referencingEntityName = registryVO.getReferencingEntityName();
		String referencingEntityCompletingName = registryVO.getReferencingEntityCompletingName();
		if (referencingEntityCompletingName.equals(Content.class.getName()) && referencingEntityName.equals(ContentVersion.class.getName()))
		{
			Integer referencingEntityId = new Integer(registryVO.getReferencingEntityId());
			try
			{
				ContentVersionVO currentCVVO = ContentVersionController.getContentVersionController().getContentVersionVOWithId(referencingEntityId, db);
				ContentVersionVO latestContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(currentCVVO.getContentId(), currentCVVO.getLanguageId(), db);
				if (currentCVVO.equals(latestContentVersion))
				{
					versionRegistryPair.put(currentCVVO, registryVO);
				}
			}
			catch (SystemException ex)
			{
				logger.warn("Error when getting ContentVersion. Will not be added to the list. ID: " + referencingEntityId + ". Message: " + ex.getMessage());
			}
		}
	}

	private Map<SiteNodeVO, RegistryVO> extractSiteNodesFromRegistryList(List<RegistryVO> registryVOs, Database db) throws Exception
	{
		Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = new HashMap<SiteNodeVO, RegistryVO>();
    	for (RegistryVO registryVO : registryVOs)
		{
    		extractSiteNodeFromRegistry(siteNodeRegistryPair, registryVO, db);
		}
    	if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + siteNodeRegistryPair.size() + " ContentVersions from " + registryVOs.size() + " registry entries");
		}
    	return siteNodeRegistryPair;
	}

	private Map<SiteNodeVO, RegistryVO> extractSiteNodesFromRegistryList(String[] registryIds, Database db) throws Exception
	{
		Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = new HashMap<SiteNodeVO, RegistryVO>();

		Integer registryId;
		RegistryVO registryVO;
		for (String registryIdString : registryIds)
		{
			registryId = new Integer(registryIdString);
			registryVO = RegistryController.getController().getRegistryVOWithId(registryId, db);
			extractSiteNodeFromRegistry(siteNodeRegistryPair, registryVO, db);
		}

		if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + siteNodeRegistryPair.size() + " ContentVersions from " + registryIds.length + " registry entries");
		}
		return siteNodeRegistryPair;
	}

	private void extractSiteNodeFromRegistry(Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair, RegistryVO registryVO, Database db) throws SystemException, Bug, Exception
	{
		String referencingEntityCompletingName = registryVO.getReferencingEntityCompletingName();
		if (referencingEntityCompletingName.equals(SiteNode.class.getName()))
		{
			try
			{
				SiteNodeVO siteNodeVO = SiteNodeController.getSiteNodeVOWithId(new Integer(registryVO.getReferencingEntityCompletingId()), db);
				siteNodeRegistryPair.put(siteNodeVO, registryVO);
			}
			catch (SystemException ex)
			{
				logger.warn("Error when getting SiteNode. Will not add to registry list. ID: " + registryVO.getReferencingEntityCompletingId() + ". Message: " + ex.getMessage());
			}
		}
	}

    public void cleanAllForSiteNode(Integer siteNodeId, InfoGluePrincipal principal) throws SystemException
    {
    	Database db = CastorDatabaseService.getDatabase();
    	try
    	{
    		beginTransaction(db);

    		cleanAllForSiteNode(siteNodeId, principal, db);

    		commitTransaction(db);
    	}
    	catch (Throwable ex)
    	{
    		logger.error("Failed to delete a SiteNode's references. Message: " + ex.getMessage() + ". Type: " + ex.getClass());
    		logger.warn("Failed to delete a SiteNode's references.", ex);
    		rollbackTransaction(db);
    	}
    }

    public void cleanAllForSiteNode(Integer siteNodeId, InfoGluePrincipal principal, Database db) throws Exception
    {
		List<RegistryVO> registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), -1, db);
    	Map<ContentVersionVO, RegistryVO> contentVersionRegistryPair = extractContentVersionsFromRegistryList(registryEntires, db);
		InconsistenciesController.getController().removeContentReferences(contentVersionRegistryPair, principal, db);
		Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = extractSiteNodesFromRegistryList(registryEntires, db);
		InconsistenciesController.getController().removeSiteNodeReferences(siteNodeRegistryPair, principal, db);
    }

	public void cleanAllForContent(Integer contentId, InfoGluePrincipal principal) throws SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
		try
		{
			beginTransaction(db);

			cleanAllForContent(contentId, principal, db);

			commitTransaction(db);
		}
		catch (Throwable ex)
		{
			logger.error("Failed to clean a Content. Message: " + ex.getMessage() + ". Type: " + ex.getClass());
			logger.warn("Failed to clean a Content.", ex);
			rollbackTransaction(db);
		}
    }

	public void cleanAllForContent(Integer contentId, InfoGluePrincipal principal, Database db) throws Exception
	{
		List<RegistryVO> registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), -1, db); // , db
		Map<ContentVersionVO, RegistryVO> contentVersionRegistryPair = extractContentVersionsFromRegistryList(registryEntires, db);
		InconsistenciesController.getController().removeContentReferences(contentVersionRegistryPair, principal, db);
		Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = extractSiteNodesFromRegistryList(registryEntires, db);
		InconsistenciesController.getController().removeSiteNodeReferences(siteNodeRegistryPair, principal, db);
    }

	/**
	 * This method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	public void updateContentVersion(ContentVersionVO contentVersionVO) throws ConstraintException, SystemException
	{
		// This call propably do nothing but left here since it was written by someone for some reason at one point
	    contentVersionVO.getVersionValue();

	    Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
			updateContentVersion(contentVersion, db);

			commitTransaction(db);
		}
		catch (Exception e)
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);
		}
	}

	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersion contentVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    String versionValue = contentVersion.getVersionValue();
	    
	    ContentVersion oldContentVersion = contentVersion; //ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
	    Content oldContent = oldContentVersion.getOwningContent();
	    
	    if(oldContent.getContentTypeDefinition().getName().equalsIgnoreCase("Meta info"))
	    {
	        logger.info("It was a meta info so lets check it for other stuff as well");
		    
	        SiteNodeVersion siteNodeVersion = getLatestActiveSiteNodeVersionWhichUsesContentVersionAsMetaInfo(oldContentVersion, db);
		    if(siteNodeVersion != null)
		    {
		        logger.info("Going to use " + siteNodeVersion.getId() + " as reference");
		        clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersion.getId().toString(), db);
			    
			    getComponents(siteNodeVersion, versionValue, db);
			    getComponentBindings(siteNodeVersion, versionValue, db);
			    getPageBindings(siteNodeVersion, db);
		    }
	        
		    getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }
	    else
	    {
	        clearRegistryVOList(ContentVersion.class.getName(), oldContentVersion.getContentVersionId().toString(), db);
	        getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }		
	}
	
	
	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersion contentVersion, SiteNodeVersion siteNodeVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    String versionValue = contentVersion.getVersionValue();
	    
	    ContentVersion oldContentVersion = contentVersion; //ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
	    Content oldContent = oldContentVersion.getOwningContent();
	    
	    if(oldContent.getContentTypeDefinition().getName().equalsIgnoreCase("Meta info"))
	    {
	        logger.info("It was a meta info so lets check it for other stuff as well");
		    
		    if(siteNodeVersion != null)
		    {
		        logger.info("Going to use " + siteNodeVersion.getId() + " as reference");
			    clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersion.getId().toString(), db);
		        
			    getComponents(siteNodeVersion, versionValue, db);
			    getComponentBindings(siteNodeVersion, versionValue, db);
			    getPageBindings(siteNodeVersion, db);
		    }
	        
		    getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }
	    else
	    {
	        clearRegistryVOList(ContentVersion.class.getName(), oldContentVersion.getContentVersionId().toString(), db);
	        if(siteNodeVersion != null)
	        	getPageBindings(siteNodeVersion, db);
		    getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }		
	}

	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersion
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateSiteNodeVersion(SiteNodeVersionVO siteNodeVersionVO) throws ConstraintException, SystemException
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
		
			logger.info("Starting RegistryController.updateSiteNodeVersion...");
			SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(siteNodeVersionVO.getId(), db);
			logger.info("Before RegistryController.updateSiteNodeVersion...");
			updateSiteNodeVersion(siteNodeVersion, db);
			logger.info("Before commit RegistryController.updateSiteNodeVersion...");
		    
			commitTransaction(db);
		}
		catch (Exception e)		
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}
	}

	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersion
	 * @throws SystemException
	 * @throws Exception
	 */
	
	@SuppressWarnings({ "rawtypes", "unused" })
	public void updateSiteNodeVersion(SiteNodeVersion siteNodeVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    SiteNodeVersion oldSiteNodeVersion = siteNodeVersion;
	    SiteNode oldSiteNode = oldSiteNodeVersion.getOwningSiteNode();

	    logger.info("Before clearing old registry...");
	    clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersion.getId().toString(), db);
	    logger.info("After clearing old registry...");

		Collection serviceBindings = siteNodeVersion.getServiceBindings();
		Iterator serviceBindingIterator = serviceBindings.iterator();
		while(serviceBindingIterator.hasNext())
		{
		    ServiceBinding serviceBinding = (ServiceBinding)serviceBindingIterator.next();
		    if(serviceBinding.getBindingQualifyers() != null)
		    {
			    Iterator qualifyersIterator = serviceBinding.getBindingQualifyers().iterator();
			    while(qualifyersIterator.hasNext())
			    {
			        Qualifyer qualifyer = (Qualifyer)qualifyersIterator.next();
			        String name = qualifyer.getName();
			        String value = qualifyer.getValue();

	                try
	                {
				        RegistryVO registryVO = new RegistryVO();
				        registryVO.setReferenceType(RegistryVO.PAGE_BINDING);
			            if(name.equalsIgnoreCase("contentId"))
				        {
			                Content content = ContentController.getContentController().getContentWithId(new Integer(value), db);

			                registryVO.setEntityId(value);
				            registryVO.setEntityName(Content.class.getName());

				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId(oldSiteNode.getId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());

				            Language masterLanguage = LanguageController.getController().getMasterLanguage(db, siteNodeVersion.getOwningSiteNode().getRepository().getId());
				            ContentVersion contentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersion(content.getContentId(), masterLanguage.getId(), db);
				            getComponents(siteNodeVersion, contentVersion.getVersionValue(), db);
			                getComponentBindings(siteNodeVersion, contentVersion.getVersionValue(), db);

				            /*
				            Collection contentVersions = content.getContentVersions();
				            Iterator contentVersionIterator = contentVersions.iterator();
				            while(contentVersionIterator.hasNext())
					        {
				                ContentVersion contentVersion = (ContentVersion)contentVersionIterator.next();
				                getComponents(siteNodeVersion, contentVersion.getVersionValue(), db);
				                getComponentBindings(siteNodeVersion, contentVersion.getVersionValue(), db);
				            }
				            */
				        }
			            else if(name.equalsIgnoreCase("siteNodeId"))
				        {
			                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(value), db);

			                registryVO.setEntityId(value);
				            registryVO.setEntityName(SiteNode.class.getName());

				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId(oldSiteNode.getId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        }

			    	    logger.info("Before creating registry entry...");

			            this.create(registryVO, db);
	                }
	                catch(Exception e)
	                {
	                    e.printStackTrace();
	                }
			    }
		    }
		}
	}

	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersion
	 * @throws SystemException
	 * @throws Exception
	 */

	@SuppressWarnings({ "unused", "rawtypes" })
	public void getPageBindings(SiteNodeVersion siteNodeVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    SiteNode oldSiteNode = siteNodeVersion.getOwningSiteNode();

		Collection serviceBindings = siteNodeVersion.getServiceBindings();
		Iterator serviceBindingIterator = serviceBindings.iterator();
		while(serviceBindingIterator.hasNext())
		{
		    ServiceBinding serviceBinding = (ServiceBinding)serviceBindingIterator.next();
		    if(serviceBinding.getBindingQualifyers() != null)
		    {
			    Iterator qualifyersIterator = serviceBinding.getBindingQualifyers().iterator();
			    while(qualifyersIterator.hasNext())
			    {
			        Qualifyer qualifyer = (Qualifyer)qualifyersIterator.next();
			        String name = qualifyer.getName();
			        String value = qualifyer.getValue();

	                try
	                {
				        RegistryVO registryVO = new RegistryVO();
				        registryVO.setReferenceType(RegistryVO.PAGE_BINDING);
			            if(name.equalsIgnoreCase("contentId"))
				        {
			                Content content = ContentController.getContentController().getContentWithId(new Integer(value), db);

			                registryVO.setEntityId(value);
				            registryVO.setEntityName(Content.class.getName());

				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId(oldSiteNode.getId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());

				            /*
				            Collection contentVersions = content.getContentVersions();
				            Iterator contentVersionIterator = contentVersions.iterator();
				            while(contentVersionIterator.hasNext())
				            {
				                ContentVersion contentVersion = (ContentVersion)contentVersionIterator.next();
				                getComponents(siteNodeVersion, contentVersion.getVersionValue(), db);
				                getComponentBindings(siteNodeVersion, contentVersion.getVersionValue(), db);
				            }
				            */
				        }
			            else if(name.equalsIgnoreCase("siteNodeId"))
				        {
			                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(value), db);

			                registryVO.setEntityId(value);
				            registryVO.setEntityName(SiteNode.class.getName());

				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId(oldSiteNode.getId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        }

			    	    logger.info("Before creating registry entry...");

			            this.create(registryVO, db);
	                }
	                catch(Exception e)
	                {
	                    e.printStackTrace();
	                }
			    }
		    }
		}
	}

	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineSiteNodes(ContentVersion contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getPageUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("(");
	        int siteNodeEndIndex = match.indexOf(",");
	        if(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            String siteNodeIdString = match.substring(siteNodeStartIndex + 1, siteNodeEndIndex); 
	            try
	            {
		            if(siteNodeIdString.indexOf("templateLogic.siteNodeId") == -1)
		            {
		            	siteNodeId = new Integer(siteNodeIdString);
			            logger.info("siteNodeId:" + siteNodeId);
			            RegistryVO registryVO = new RegistryVO();
			            registryVO.setEntityId(siteNodeId.toString());
			            registryVO.setEntityName(SiteNode.class.getName());
			            registryVO.setReferenceType(RegistryVO.INLINE_LINK);
			            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
			            registryVO.setReferencingEntityName(ContentVersion.class.getName());
			            registryVO.setReferencingEntityCompletingId(contentVersion.getOwningContent().getContentId().toString());
			            registryVO.setReferencingEntityCompletingName(Content.class.getName());
			            
			            this.create(registryVO, db);
		            }
	            }
	            catch(Exception e)
	            {
	                logger.warn("Tried to register inline sitenodes with exception as result:" + e.getMessage(), e);
	            }
	        }
	    }
	}
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineSiteNodes(String versionValue, Set<Integer> boundSiteNodeIds, Set<Integer> boundContentIds) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getPageUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("(");
	        int siteNodeEndIndex = match.indexOf(",");
	        if(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            String siteNodeIdString = match.substring(siteNodeStartIndex + 1, siteNodeEndIndex); 
	            try
	            {
		            if(siteNodeIdString.indexOf("templateLogic.siteNodeId") == -1)
		            {
		            	siteNodeId = new Integer(siteNodeIdString);
			            logger.info("siteNodeId:" + siteNodeId);
			            boundSiteNodeIds.add(siteNodeId);
		            }
	            }
	            catch(Exception e)
	            {
	                logger.warn("Tried to register inline sitenodes with exception as result:" + e.getMessage(), e);
	            }
	        }
	    }
	}
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineContents(ContentVersion contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getInlineAssetUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("(");
	        int contentEndIndex = match.indexOf(",");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 1, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            RegistryVO registryVO = new RegistryVO();
	            registryVO.setEntityId(contentId.toString());
	            registryVO.setEntityName(Content.class.getName());
	            registryVO.setReferenceType(RegistryVO.INLINE_ASSET);
	            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
	            registryVO.setReferencingEntityName(ContentVersion.class.getName());
	            registryVO.setReferencingEntityCompletingId(contentVersion.getOwningContent().getContentId().toString());
	            registryVO.setReferencingEntityCompletingName(Content.class.getName());
	            
	            this.create(registryVO, db);
	        }
	    }
	}
	

	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getRelationSiteNodes(ContentVersion contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("<qualifyer entity='SiteNode'>.*?</qualifyer>");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("<id>");
	        int siteNodeEndIndex = match.indexOf("</id>");
	        while(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            siteNodeId = new Integer(match.substring(siteNodeStartIndex + 4, siteNodeEndIndex));
	            logger.info("siteNodeId:" + siteNodeId);
	            RegistryVO registryVO = new RegistryVO();
	            registryVO.setEntityId(siteNodeId.toString());
	            registryVO.setEntityName(SiteNode.class.getName());
	            registryVO.setReferenceType(RegistryVO.INLINE_SITE_NODE_RELATION);
	            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
	            registryVO.setReferencingEntityName(ContentVersion.class.getName());
	            registryVO.setReferencingEntityCompletingId(contentVersion.getOwningContent().getContentId().toString());
	            registryVO.setReferencingEntityCompletingName(Content.class.getName());
	            
	            this.create(registryVO, db);
	            
	            siteNodeStartIndex = match.indexOf("<id>", siteNodeEndIndex);
		        siteNodeEndIndex = match.indexOf("</id>", siteNodeStartIndex);
	        }
	    }
	}
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getRelationContents(ContentVersion contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("<qualifyer entity='Content'>.*?</qualifyer>");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("<id>");
	        int contentEndIndex = match.indexOf("</id>");
	        while(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 4, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            RegistryVO registryVO = new RegistryVO();
	            registryVO.setEntityId(contentId.toString());
	            registryVO.setEntityName(Content.class.getName());
	            registryVO.setReferenceType(RegistryVO.INLINE_CONTENT_RELATION);
	            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
	            registryVO.setReferencingEntityName(ContentVersion.class.getName());
	            registryVO.setReferencingEntityCompletingId(contentVersion.getOwningContent().getContentId().toString());
	            registryVO.setReferencingEntityCompletingName(Content.class.getName());
	            
	            this.create(registryVO, db);
	            
	            contentStartIndex = match.indexOf("<id>", contentEndIndex);
	            contentEndIndex = match.indexOf("</id>", contentStartIndex);
	        }
	    }
	}
	                
	
	/**
	 * This method fetches all components and adds entries to the registry.
	 */
	
	public void getComponents(SiteNodeVersion siteNodeVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    List<Integer> foundComponents = new ArrayList<Integer>();
	    
	    Pattern pattern = Pattern.compile("contentId=\".*?\"");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("\"");
	        int contentEndIndex = match.lastIndexOf("\"");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 1, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            if(!foundComponents.contains(contentId))
	            {
		            RegistryVO registryVO = new RegistryVO();
		            registryVO.setEntityId(contentId.toString());
		            registryVO.setEntityName(Content.class.getName());
		            registryVO.setReferenceType(RegistryVO.PAGE_COMPONENT);
		            registryVO.setReferencingEntityId(siteNodeVersion.getSiteNodeVersionId().toString());
		            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
		            registryVO.setReferencingEntityCompletingId(siteNodeVersion.getOwningSiteNode().getSiteNodeId().toString());
		            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
		            
		            this.create(registryVO, db);
		            
		            foundComponents.add(contentId);
	            }
	        }
	    }
	}

	/**
	 * This method fetches all components and adds entries to the registry.
	 */

	public void getComponentBindings(SiteNodeVersion siteNodeVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
		List<String> foundComponents = new ArrayList<String>();

		Pattern pattern = Pattern.compile("<binding.*?entity=\".*?\" entityId=\".*?\">");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        String entityName;
	        String entityId;
	        
	        int entityNameStartIndex = match.indexOf("entity=\"");
	        int entityNameEndIndex = match.indexOf("\"", entityNameStartIndex + 8);
	        logger.info("entityNameStartIndex:" + entityNameStartIndex);
	        logger.info("entityNameEndIndex:" + entityNameEndIndex);
	        if(entityNameStartIndex > 0 && entityNameEndIndex > 0 && entityNameEndIndex > entityNameStartIndex)
	        {
	            entityName = match.substring(entityNameStartIndex + 8, entityNameEndIndex);
	            logger.info("entityName:" + entityName);

		        int entityIdStartIndex = match.indexOf("entityId=\"", entityNameEndIndex + 1);
		        int entityIdEndIndex = match.indexOf("\"", entityIdStartIndex + 10);
		        logger.info("entityIdStartIndex:" + entityIdStartIndex);
		        logger.info("entityIdEndIndex:" + entityIdEndIndex);
		        if(entityIdStartIndex > 0 && entityIdEndIndex > 0 && entityIdEndIndex > entityIdStartIndex)
		        {
		            entityId = match.substring(entityIdStartIndex + 10, entityIdEndIndex);
		            logger.info("entityId:" + entityId);

		            String key = entityName + ":" + entityId;
		            if(!foundComponents.contains(key))
		            {	        
			            RegistryVO registryVO = new RegistryVO();
			            if(entityName.indexOf("Content") > -1)
			                registryVO.setEntityName(Content.class.getName());
			            else
			                registryVO.setEntityName(SiteNode.class.getName());
			                
			            registryVO.setEntityId(entityId);
			            registryVO.setReferenceType(RegistryVO.PAGE_COMPONENT_BINDING);
			            registryVO.setReferencingEntityId(siteNodeVersion.getSiteNodeVersionId().toString());
			            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
			            registryVO.setReferencingEntityCompletingId(siteNodeVersion.getOwningSiteNode().getSiteNodeId().toString());
			            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
			            
			            this.create(registryVO, db);

			            foundComponents.add(key);
		            }
		        }
	        }
	    }
	}

	/**
	 * Implemented for BaseController
	 */
	public BaseEntityVO getNewVO()
	{
		return new CategoryVO();
	}

    /**
     * This method gets all referencing content versions
     * 
     * @param contentId
     * @return
     */
	/*
    public List getReferencingObjectsForContent(Integer contentId) throws SystemException
    {
        List referenceBeanList = new ArrayList();
        
		Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			Map entries = new HashMap();
			
	        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), db);
	        Iterator registryEntiresIterator = registryEntires.iterator();
	        while(registryEntiresIterator.hasNext())
	        {
	            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
	            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
	            
	            ReferenceBean referenceBean = new ReferenceBean();
	            	            
	            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
	            {
		            ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("contentVersion:" + contentVersion.getContentVersionId());
		    		referenceBean.setName(contentVersion.getOwningContent().getName());
		    		referenceBean.setReferencingObject(contentVersion.getValueObject());
	            }
	            else
	            {
	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		referenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
		    		referenceBean.setReferencingObject(siteNodeVersion.getValueObject());
	            }
	            
	            String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
	            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
	            if(existingReferenceBean == null)
	            {
		            List registryVOList = new ArrayList();
		            registryVOList.add(registryVO);
		            referenceBean.setRegistryVOList(registryVOList);
		            logger.info("Adding referenceBean to entries with key:" + key);
		            entries.put(key, referenceBean);
		            referenceBeanList.add(referenceBean);
	            }
	            else
	            {
	                logger.info("Found referenceBean in entries with key:" + key);
	                existingReferenceBean.getRegistryVOList().add(registryVO);
	            }
	        }

	        commitTransaction(db);
		}
		catch ( Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}

        return referenceBeanList;
    }
    */

	public Set<SiteNodeVO> getReferencingSiteNodesForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
    {
        Set<SiteNodeVO> referenceBeanList = new HashSet<SiteNodeVO>();

        List<RegistryVO> registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
        //t.printElapsedTime("registryEntires:" + registryEntires.size());
        logger.info("registryEntires:" + registryEntires.size());
        Iterator<RegistryVO> registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
        	RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
        	if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
        		continue;

        	logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());

//            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            try
            {
                @SuppressWarnings("static-access")
				SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(new Integer(registryVO.getReferencingEntityCompletingId()), db);
                //t.printElapsedTime("siteNodeVersion 1");
	    		referenceBeanList.add(siteNodeVO);
            }
            catch(Exception e)
            {
                logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
            }
        }

		logger.info("referenceBeanList:" + referenceBeanList.size());

        return referenceBeanList;
    }

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId) throws SystemException
    {
		return getReferencingObjectsForContent(contentId, -1, true, false);
    }

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows) throws SystemException
	{
		return getReferencingObjectsForContent(contentId, maxRows, true, false);
	}

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, boolean excludeInternalContentReferences, boolean excludeRepositoryInternalReferences) throws SystemException
    {
		List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

		Database db = CastorDatabaseService.getDatabase();

		try
		{
			beginTransaction(db);

			referenceBeanList = getReferencingObjectsForContent(contentId, maxRows, excludeInternalContentReferences, excludeRepositoryInternalReferences, db);

	        commitTransaction(db);
		}
		catch (Exception e)
		{
		    logger.warn("One of the references was not found which is bad but not critical:" + e.getMessage(), e);
		    rollbackTransaction(db);
			//throw new SystemException("An error occurred when we tried to fetch a list of roles in the repository. Reason:" + e.getMessage(), e);			
		}

		logger.info("referenceBeanList:" + referenceBeanList.size());

        return referenceBeanList;
    }

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
	{
		return getReferencingObjectsForContent(contentId, maxRows, true, false, db);
	}

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, boolean excludeInternalContentReferences, boolean excludeRepositoryInternalReferences, Database db) throws SystemException, Exception
	{
		List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

		Map<String,Boolean> checkedLanguageVersions = new HashMap<String,Boolean>();
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();

		List<RegistryVO> registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
		Iterator<RegistryVO> registryEntiresIterator = registryEntires.iterator();
		Integer repositoryId = null;
		if (excludeRepositoryInternalReferences)
		{
			ContentVO contentVO = ContentController.getContentController().getContentVOWithId(contentId, db);
			RepositoryVO repositoryVO = RepositoryController.getController().getRepositoryVOWithId(contentVO.getRepositoryId(), db);
			repositoryId = repositoryVO.getRepositoryId();
		}
		while(registryEntiresIterator.hasNext())
		{
            RegistryVO registryVO = registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, checkedLanguageVersions, repositoryId, db);
            if (referenceBean != null)
            {
            	referenceBeanList.add(referenceBean);
            }
        }

        Iterator<ReferenceBean> i = referenceBeanList.iterator();
        while(i.hasNext())
        {
            ReferenceBean referenceBean = i.next();
            if(referenceBean.getVersions().size() == 0)
                i.remove();
        }

        return referenceBeanList;
    }

//	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, boolean excludeInternalContentReferences, Database db) throws SystemException, Exception
//    {
//        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();
//
//        Map entries = new HashMap();
//		
//        Map<String,Boolean> checkedLanguageVersions = new HashMap<String,Boolean>();
//
//        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
//        logger.info("registryEntires:" + registryEntires.size());
//        Iterator registryEntiresIterator = registryEntires.iterator();
//        while(registryEntiresIterator.hasNext())
//        {
//            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
//            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
//            boolean add = true;
//            
//            String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
//            //String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
//            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
//            if(existingReferenceBean == null)
//            {
//                
//                existingReferenceBean = new ReferenceBean();
//	            logger.info("Adding referenceBean to entries with key:" + key);
//	            entries.put(key, existingReferenceBean);
//	            referenceBeanList.add(existingReferenceBean);
//	        }
//
//            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
//            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
//            {
//                try
//                {
////                    ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
//                	ContentVersionVO contentVersion = ContentVersionController.getContentVersionController().getContentVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
//                    Boolean hasVersion = checkedLanguageVersions.get("" + contentVersion.getContentId() + "_" + contentVersion.getLanguageId());
//                    if (hasVersion != null)
//                    {
//                    	continue;
//                    	//referenceBeanList.remove(existingReferenceBean);
//                    }
//                    else if(excludeInternalContentReferences && contentVersion.getContentId().equals(contentId))
//		    		{
//		    			logger.info("Skipping internal reference " + contentId + " had on itself.");
//		    			referenceBeanList.remove(existingReferenceBean);
//		    		}
//		    		else
//		    		{
//		    			ContentVO contentVO = ContentController.getContentController().getContentVOWithId(contentVersion.getContentId(), db);
//		    			existingReferenceBean.setName(contentVO.getName());
//		    			existingReferenceBean.setReferencingCompletingObject(contentVO);
//			    		existingReferenceBean.setPath(ContentController.getContentController().getContentPath(contentVO.getContentId(), false, true, db));
//			    		try
//			    		{
//			    			String userName = contentVersion.getVersionModifier();
//			    			if(userName == null || userName.equals(""))
//			    				userName = contentVO.getCreatorName();
//
//				    		InfoGluePrincipal user = UserControllerProxy.getController().getUser(userName);
//				    		if(user != null)
//				    			existingReferenceBean.setContactPersonEmail(user.getEmail());
//				    		else
//				    			existingReferenceBean.setContactPersonEmail(userName);
//			    		}
//			    		catch (Exception e)
//			    		{
//			    			logger.warn("Problem getting version modifier email: " + e.getMessage());
//						}
//		    			referenceVersionBean.setReferencingObject(contentVersion);
//		    			referenceVersionBean.getRegistryVOList().add(registryVO);
//
//			    		checkedLanguageVersions.put("" + contentVersion.getContentId() + "_" + contentVersion.getLanguageId(), new Boolean(true));
//		    		}
//                }
//                catch(Exception e)
//                {
//                    add = false;
//                    logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
//                }
//            }
//            else
//            {
//                try
//                {
//	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
//		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
//		    		logger.info("siteNode:" + siteNodeVersion.getOwningSiteNode().getId());
//		    		existingReferenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
//		    		existingReferenceBean.setReferencingCompletingObject(siteNodeVersion.getOwningSiteNode().getValueObject());
//		    		existingReferenceBean.setPath(SiteNodeController.getController().getSiteNodePath(siteNodeVersion.getValueObject().getSiteNodeId(), false, true, db));
//		    		try
//		    		{
//		    			String userName = siteNodeVersion.getVersionModifier();
//		    			if(userName == null || userName.equals(""))
//		    				userName = siteNodeVersion.getOwningSiteNode().getCreator();
//
//			    		InfoGluePrincipal user = UserControllerProxy.getController().getUser(userName);
//			    		if(user != null)
//			    		{
//			    			existingReferenceBean.setContactPersonEmail(user.getEmail());
//			    		}
//		    			existingReferenceBean.setContactPersonUsername(userName);
//		    		}
//		    		catch (Exception e) 
//		    		{
//		    			logger.warn("Problem getting version modifier email: " + e.getMessage());
//					}
//		    		referenceVersionBean.setReferencingObject(siteNodeVersion.getValueObject());
//		    		referenceVersionBean.getRegistryVOList().add(registryVO);
//                }
//                catch(Exception e)
//                {
//                    add = false;
//                    logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
//                }
//            }
//            
//            if(add)
//            {
//                boolean exists = false;
//                ReferenceVersionBean existingReferenceVersionBean = null;
//	            Iterator versionsIterator = existingReferenceBean.getVersions().iterator();
//	            while(versionsIterator.hasNext())
//	            {
//	                existingReferenceVersionBean = (ReferenceVersionBean)versionsIterator.next();
//	                if(existingReferenceVersionBean == null || existingReferenceVersionBean.getReferencingObject() == null || referenceVersionBean.getReferencingObject() == null || referenceVersionBean == null || existingReferenceVersionBean.getReferencingObject().equals(referenceVersionBean.getReferencingObject()))
//	                {
//	                    exists = true;
//	                    break;
//	                }
//	            }
//
//	            if(!exists)
//	                existingReferenceBean.getVersions().add(referenceVersionBean);
//	            else
//	                existingReferenceVersionBean.getRegistryVOList().add(registryVO);
//
//            }
//            
//        }
//        
//        Iterator i = referenceBeanList.iterator();
//        while(i.hasNext())
//        {
//            ReferenceBean referenceBean = (ReferenceBean)i.next();
//            if(referenceBean.getVersions().size() == 0)
//                i.remove();
//        }
//	    
//		logger.info("referenceBeanList:" + referenceBeanList.size());
//		
//        return referenceBeanList;
//    }
    
    
    /**
     * This method gets all referencing sitenode versions
     * 
     * @param siteNodeId
     * @return
     */
	/*
    public List getReferencingObjectsForSiteNode(Integer siteNodeId) throws SystemException, Exception
    {
        List referenceBeanList = new ArrayList();
        
        Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			Map entries = new HashMap();
			
			List registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), db);
	        Iterator registryEntiresIterator = registryEntires.iterator();
	        while(registryEntiresIterator.hasNext())
	        {
	            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
	            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
	            
	            ReferenceBean referenceBean = new ReferenceBean();
	           
	            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
	            {
                    ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("contentVersion:" + contentVersion.getContentVersionId());
		    		referenceBean.setName(contentVersion.getOwningContent().getName());
		    		referenceBean.setReferencingObject(contentVersion.getValueObject());
		    	}
	            else
	            {
	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		referenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
		    		referenceBean.setReferencingObject(siteNodeVersion.getValueObject());
	            }
	            
	            String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
	            //String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
	            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
	            if(existingReferenceBean == null)
	            {
		            List registryVOList = new ArrayList();
		            registryVOList.add(registryVO);
		            referenceBean.setRegistryVOList(registryVOList);
		            logger.info("Adding referenceBean to entries with key:" + key);
		            entries.put(key, referenceBean);
		            referenceBeanList.add(referenceBean);
	            }
	            else
	            {
	                logger.info("Found referenceBean in entries with key:" + key);
	                existingReferenceBean.getRegistryVOList().add(registryVO);
	            }
	        }
	        
	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
			//throw new SystemException("An error occurred when we tried to fetch a list of roles in the repository. Reason:" + e.getMessage(), e);			
		}
		
        return referenceBeanList;
    }
    */

	protected String getContactPersonEmail(ContentVO contentVO, Database db) throws SystemException, Exception
	{
		InfoGluePrincipal user = UserControllerProxy.getController(db).getUser(contentVO.getCreatorName());

		if (user == null)
		{
			return "" + contentVO.getCreatorName();
		}
		else
		{
			return user.getEmail();
		}
	}

	/**
	 * Returns the email address of the content version's version modifier.
	 * @param contentVersionVO
	 * @param db Transaction to use when looking for version modifier entity
	 * @return An email address as a String or null if the given content versions modifier's email address cannot be found.
	 */
	protected String getContactPersonEmail(ContentVersionVO contentVersionVO, Database db)
	{
		try
		{
			InfoGluePrincipal user = UserControllerProxy.getController(db).getUser(contentVersionVO.getVersionModifier());
			if (user != null)
			{
				return user.getEmail();
			}
		}
		catch (Exception ex)
		{
			logger.warn("Version modifier was not found in the system. Cannot find email for: " + contentVersionVO.getVersionModifier());
			return null;
		}
		return null;
	}

	/**
     * Attempts to get an email address to the contact person of the given SiteNode.
     *
     * First the Meta info is examined for a value of an attribute specified by {@link CmsPropertyHandler#getContactPersonEmailMetaInfoAttribute()}
     * and if nothing is found the SiteNode creator's email is used. Null is returned if no value could be found.
     */
    protected String getContactPersonEmail(SiteNodeVO siteNodeVO, Database db) throws SystemException, Exception
	{
    	String contactPersonEmailMetaInfoAttribute = CmsPropertyHandler.getContactPersonEmailMetaInfoAttribute();

    	if (contactPersonEmailMetaInfoAttribute != null && !contactPersonEmailMetaInfoAttribute.equals(""))
    	{
	    	LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId(), db);
			if (masterLanguage != null)
			{
				try
				{
		    		ContentVersionVO metaInfoContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNodeVO.getMetaInfoContentId(), masterLanguage.getId(), db);
		    		if (metaInfoContentVersion != null)
		    		{
		    			String contactPersonEmail = ContentVersionController.getContentVersionController().getAttributeValue(metaInfoContentVersion, contactPersonEmailMetaInfoAttribute, false);
		    			if (contactPersonEmail != null && !contactPersonEmail.equals(""))
		    			{
			    			if (logger.isDebugEnabled())
			    			{
			    				logger.debug("Reading contact person email from: SiteNode, meta info, master language. Id: " + siteNodeVO.getId());
			    			}
			    			return contactPersonEmail;
		    			}
					}
				}
				catch (SystemException ex)
				{
					logger.info("Error getting contact person from meta-info content. Maybe the content has been deleted. Message: " + ex.getMessage());
				}
			}

			@SuppressWarnings("unchecked")
			List<LanguageVO> repositoryLanguages = LanguageController.getController().getLanguageVOList(siteNodeVO.getRepositoryId(), db);
			for (LanguageVO language : repositoryLanguages)
			{
				try
				{
					ContentVersionVO metaInfoContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNodeVO.getMetaInfoContentId(), language.getId(), db);
		    		if (metaInfoContentVersion != null)
		    		{
		    			String contactPersonEmail = ContentVersionController.getContentVersionController().getAttributeValue(metaInfoContentVersion, contactPersonEmailMetaInfoAttribute, false);
		    			if (contactPersonEmail != null && !contactPersonEmail.equals(""))
		    			{
			    			if (logger.isDebugEnabled() && contactPersonEmail != null)
			    			{
			    				if (logger.isDebugEnabled())
				    			{
			    					logger.debug("Reading contact person email from: SiteNode, meta info, language. Language-id: " + language.getId() + ", Id: " + siteNodeVO.getId());
				    			}
			    				return contactPersonEmail;
			    			}
			    		}
		    		}
				}
				catch (SystemException ex)
				{
					logger.info("Error getting contact person from meta-info content for language: '" + language.getLanguageId() + "'. Maybe the content has been deleted. Message: " + ex.getMessage());
				}
			}
		}

		InfoGluePrincipal user = null;
		try
		{
			user = UserControllerProxy.getController(db).getUser(siteNodeVO.getCreatorName());
		}
		catch (SystemException ex)
		{
			logger.info("Error getting contact person from principal. Maybe the principal has been deleted. Message: " + ex.getMessage());
		}
		if (user != null)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Reading contact person email from: SiteNode, system user. User: " + user.getName() + ", Id: " + siteNodeVO.getId());
			}
			return user.getEmail();
		}
		else
		{
			logger.debug("Could not find a contact person email for SiteNode.id: " + siteNodeVO.getSiteNodeId());
			return null;
		}
	}

    public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId) throws SystemException, Exception
    {
    	return getReferencingObjectsForSiteNode(siteNodeId, -1);
    }

	public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows) throws SystemException, Exception
	{
        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

        Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

		    referenceBeanList = getReferencingObjectsForSiteNode(siteNodeId, maxRows, db);

		    commitTransaction(db);
		}
		catch (Exception e)
		{
		    logger.warn("One of the references was not found which is bad but not critical:" + e.getMessage(), e);
		    rollbackTransaction(db);
		}

        return referenceBeanList;
    }

	public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows, Database db) throws SystemException, Exception
	{
		return getReferencingObjectsForSiteNode(siteNodeId, maxRows, false, db);
	}

    public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows, boolean excludeReferenceInSite, Database db) throws SystemException, Exception
    {
        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

        Map<String,Boolean> checkedLanguageVersions = new HashMap<String,Boolean>();
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();
		Integer excludeRespositoryId = null;
		if (excludeReferenceInSite)
		{
			@SuppressWarnings("static-access")
			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId, db);
			RepositoryVO repositoryVO = RepositoryController.getController().getRepositoryVOWithId(siteNodeVO.getRepositoryId(), db);
			excludeRespositoryId = repositoryVO.getRepositoryId();
		}

		List<RegistryVO> registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), maxRows, db);
        Iterator<RegistryVO> registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
            RegistryVO registryVO = registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, checkedLanguageVersions, excludeRespositoryId, db);
            if (referenceBean != null)
            {
            	referenceBeanList.add(referenceBean);
            }
        }

        Iterator<ReferenceBean> i = referenceBeanList.iterator();
        while(i.hasNext())
        {
            ReferenceBean referenceBean = i.next();
            if(referenceBean.getVersions().size() == 0)
                i.remove();
        }

        return referenceBeanList;
	}

    /**
     * Calls {@link #getReferenceBeanFromRegistryVO(RegistryVO, Map, Map, Integer, Database)} without any repository to exclude.
     */
	private ReferenceBean getReferenceBeanFromRegistryVO(RegistryVO registryVO, Map<String, ReferenceBean> entries, Map<String,Boolean> checkedLanguageVersions, Database db)
	{
		return getReferenceBeanFromRegistryVO(registryVO, entries, checkedLanguageVersions, null, db);
	}

	/**
	 * 
	 * @param registryVO
	 * @param entries
	 * @param checkedLanguageVersions
	 * @param repositoryToExclude Referencing objects in this repository will not be added. Use null to include all repositories.
	 * @param db
	 */
    private ReferenceBean getReferenceBeanFromRegistryVO(RegistryVO registryVO, Map<String, ReferenceBean> entries, Map<String,Boolean> checkedLanguageVersions, Integer repositoryToExclude, Database db)
    {
    	ReferenceBean result = null;
        boolean add = true;

        String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
        ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
        if(existingReferenceBean == null)
        {
            existingReferenceBean = new ReferenceBean();
            logger.info("Adding referenceBean to entries with key:" + key);
            entries.put(key, existingReferenceBean);
            result = existingReferenceBean;
        }
        else if (logger.isDebugEnabled())
        {
        	logger.debug("Already had ReferenceBean for key: " + key);
        }

        ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();

        if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
        {
            try
            {
				logger.debug("RegistryVO references Content");
				ContentVersionVO contentVersionVO = null;
				try
				{
					contentVersionVO = ContentVersionController.getContentVersionController().getContentVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
				}
				catch(Exception ex)
				{
					add = false;
					result = null;
					logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping.. Message: " + ex.getMessage());
				}
				if (contentVersionVO != null)
				{
					if (!contentVersionVO.getIsActive())
					{
						logger.debug("ContentVersion was not active. Will not add to reference list. ContentVersion.id: " + contentVersionVO.getContentVersionId());
						add = false;
					}
					else
					{
						Boolean hasVersion = checkedLanguageVersions.get("" + contentVersionVO.getContentId() + "_" + contentVersionVO.getLanguageId());
						if(hasVersion != null)
	                    {
							logger.debug("Will not add reference bean version to reference bean since there already is a version for this language. Content-id: " + contentVersionVO.getContentId() + ". Language-id: " + contentVersionVO.getLanguageId());
	                    	add = false;
	                    }
						else
		                {
							ContentVersionVO latestContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVersionVO.getContentId(), contentVersionVO.getLanguageId(), db);

		                	if (logger.isDebugEnabled())
		                	{
		                		logger.debug("Latest version for content.id: " + contentVersionVO.getContentId() + ". latestContentVersion.id: " + (latestContentVersion == null ? "null" : latestContentVersion.getContentVersionId()));
		                	}

		                	if (latestContentVersion != null && latestContentVersion.getContentVersionId().intValue() != contentVersionVO.getContentVersionId().intValue())
		                	{
		                		logger.debug("ContentVersion was not latest version. Will not add. ContentVersion-Id: " + contentVersionVO.getId());
								add = false;
		                	}
		                }
						ContentVO contentVO = ContentController.getContentController().getContentVOWithId(contentVersionVO.getContentId(), db);
						if (repositoryToExclude != null && contentVO.getRepositoryId().equals(repositoryToExclude))
						{
							add = false;
							result = null;
						}
		                if (add)
		                {
				    		existingReferenceBean.setName(contentVO.getName());
				    		existingReferenceBean.setPath(ContentController.getContentController().getContentPath(contentVO.getContentId(), false, true, db));
				    		existingReferenceBean.setReferencingCompletingObject(contentVO);

				    		/*
				    		 * Note about contact person and concerned people:
				    		 * Concerned people are SiteNode responsible that are concerned with this content. Since binding from SiteNodes are
				    		 * language independent we cannot know which version they are concerned with so they will be tagged for all versions.
				    		 * Contact persons are language dependent however so we separated them and do NOT add them to the concerned persons list.
				    		 */
							referenceVersionBean.setContactPersonUsername(contentVersionVO.getVersionModifier());
							addConcernedPersonsToReferenceVersion(existingReferenceBean, contentVO, db);
							String contactPersonEmail = getContactPersonEmail(contentVO, db);
							referenceVersionBean.setContactPersonEmail(contactPersonEmail);

							contentVersionVO.setLanguageName(LanguageController.getController().getLanguageVOWithId(contentVersionVO.getLanguageId(), db).getName());
				    		referenceVersionBean.setReferencingObject(contentVersionVO);
				    		referenceVersionBean.getRegistryVOList().add(registryVO);

				    		checkedLanguageVersions.put("" + contentVersionVO.getContentId() + "_" + contentVersionVO.getLanguageId(), new Boolean(true));
						}
					}
				}
            }
			catch(Exception ex)
			{
				add = false;
				result = null;
				logger.error("Error when creating reference bean from Registry entry for Content. Message: " + ex.getMessage() + ". Class: " + ex.getClass());
				// With registries there is a chance that the referencing object is gone. No need to print a stack trace for those cases.
				if (ex instanceof SystemException)
				{
					logger.warn("Error when creating reference bean from Registry entry for Content.");
				}
				else
				{
					logger.warn("Error when creating reference bean from Registry entry for Content.", ex);
				}
            }
        }
        else
        {
            try
            {
            	logger.debug("RegistryVO references SiteNode");
            	SiteNodeVersionVO siteNodeVersion = null;
				try
                {
					siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
                }
				catch (Exception ex)
				{
					add = false;
					result = null;
					logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
				}
				if (siteNodeVersion != null)
				{
	            	if (!siteNodeVersion.getIsActive())
					{
						add = false;
						logger.debug("SiteNodeVersion was not active. Will not add to reference list. SiteNodeVersion.id: " + siteNodeVersion.getSiteNodeVersionId());
					}
					else
					{
						SiteNodeVO siteNodeVO = SiteNodeController.getSiteNodeVOWithId(siteNodeVersion.getSiteNodeId(), db);
	            		SiteNodeVersionVO latestSiteNodeVersion = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersionVO(db, siteNodeVersion.getSiteNodeId());
	            		if (latestSiteNodeVersion != null && latestSiteNodeVersion.getSiteNodeVersionId() != siteNodeVersion.getSiteNodeVersionId())
	                	{
	                		logger.debug("SiteNode was not latest version. Will not add. SiteNodeVersion-Id: " + siteNodeVersion.getId());
							add = false;
	                	}

						if (repositoryToExclude != null && siteNodeVO.getRepositoryId().equals(repositoryToExclude))
						{
							add = false;
							result = null;
						}
		            	if (add)
		            	{
				    		existingReferenceBean.setName(siteNodeVO.getName());
				    		existingReferenceBean.setPath(SiteNodeController.getController().getSiteNodePath(siteNodeVO, false, true, db));
				    		existingReferenceBean.setReferencingCompletingObject(siteNodeVO);
				    		referenceVersionBean.setReferencingObject(siteNodeVersion);

				    		existingReferenceBean.setContactPersonUsername(siteNodeVO.getCreatorName());
							String contactPersonEmail = getContactPersonEmail(siteNodeVO, db);
							existingReferenceBean.setContactPersonEmail(contactPersonEmail);

				    		referenceVersionBean.getRegistryVOList().add(registryVO);
		                }
					}
				}
            }
            catch(Exception ex)
            {
                add = false;
                result = null;
				logger.error("Error when creating reference bean from Registry entry for SiteNode. Message: " + ex.getMessage() + ". Class: " + ex.getClass());
				logger.warn("Error when creating reference bean from Registry entry for SiteNode.", ex);
            }
        }

        if(add)
        {
            boolean exists = false;
            ReferenceVersionBean existingReferenceVersionBean = null;
            Iterator<ReferenceVersionBean> versionsIterator = existingReferenceBean.getVersions().iterator();
            while(versionsIterator.hasNext())
            {
                existingReferenceVersionBean = (ReferenceVersionBean)versionsIterator.next();
                if(existingReferenceVersionBean.getReferencingObject().equals(referenceVersionBean.getReferencingObject()))
                {
                    exists = true;
                    break;
                }
            }

            if(!exists)
                existingReferenceBean.getVersions().add(referenceVersionBean);
            else
                existingReferenceVersionBean.getRegistryVOList().add(registryVO);

            logger.debug("Number of versions: " + existingReferenceBean.getVersions());
        }

        return result;
    }

//	private void addConcernedPersonsToReferenceVersion(ReferenceVersionBean referenceVersionBean, SiteNodeVO siteNodeVO, Database db) throws SystemException, Exception
//	{
//		List<RegistryVO> registryVOList = getMatchingRegistryVOList(Content.class.getName(), "" + siteNodeVO.getSiteNodeId(), -1, db);
//		logger.debug("Found " + registryVOList.size() + " registry entries for SiteNode when looking for concerned persons. SiteNode-id: " + siteNodeVO.getSiteNodeId());
//		addConcernedPersonsToReferenceVersion(referenceVersionBean, registryVOList, db);
//    }

	private void addConcernedPersonsToReferenceVersion(ReferenceBean referenceBean, ContentVO contentVO, Database db) throws SystemException, Exception
	{
		List<RegistryVO> registryVOList = getMatchingRegistryVOList(Content.class.getName(), "" + contentVO.getContentId(), -1, db);
		logger.debug("Found " + registryVOList.size() + " registry entries for content when looking for concerned persons. Content-id: " + contentVO.getContentId());
		addConcernedPersonsToReference(referenceBean, registryVOList, db);
	}

	private void addConcernedPersonsToReference(ReferenceBean referenceBean, List<RegistryVO> registryVOList, Database db)
	{
		for (RegistryVO registryVO : registryVOList)
		{
			if(registryVO.getReferencingEntityName().indexOf("SiteNode") > -1)
	        {
				try
                {
					SiteNodeVersionVO siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
					@SuppressWarnings("static-access")
					SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeVersion.getSiteNodeId(), db);
					String concernedPersonEmail = getContactPersonEmail(siteNodeVO, db);
					if (concernedPersonEmail != null)
					{
						referenceBean.getConcernedPeople().add(concernedPersonEmail);
					}
                }
				catch (Exception ex)
				{
					logger.info("SiteNode: " + registryVO.getReferencingEntityCompletingId() + " or SiteNodeVersion: " + registryVO.getReferencingEntityId() + " did not exist. Will not add email to concerned persons list");
				}
	        }
		}
    }

//	public List getMatchingRegistryVOList(String entityName, String entityId, int maxRows, Database db) throws SystemException, Exception
//	{
//        Database db = CastorDatabaseService.getDatabase();
//
//		try
//		{
//			beginTransaction(db);
//
//			List registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);
//
//			Iterator i = registryVOList.iterator();
//			while(i.hasNext())
//			{
//			    RegistryVO registryVO = (RegistryVO)i.next();
//			    if(registryVO.getEntityName().indexOf("Content") > -1)
//	            {
//	                try
//	                {
//	                    Content content = ContentController.getContentController().getContentWithId(new Integer(registryVO.getEntityId()), db);
//			    		logger.info("contentVersion:" + content.getContentId());
//			    		result.add(content.getValueObject());
//	                }
//	                catch(Exception e)
//	                {
//	                    logger.info("content:" + registryVO.getEntityId() + " did not exist - skipping..");
//	                }
//	            }
//	            else if(registryVO.getEntityName().indexOf("SiteNode") > -1)
//	            {
//	                try
//	                {
//		                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(registryVO.getEntityId()), db);
//			    		logger.info("siteNode:" + siteNode.getId());
//			    		result.add(siteNode.getValueObject());
//			    	}
//	                catch(Exception e)
//	                {
//	                    logger.info("siteNode:" + registryVO.getEntityId() + " did not exist - skipping..");
//	                }
//	            }
//			}
//			
//			commitTransaction(db);
//		}
//		catch (Exception e)		
//		{
//		    rollbackTransaction(db);
//			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
//		}
//		
//		return result;
//	}

	/**
	 * Gets matching references
	 */
	public List<RegistryVO> getMatchingRegistryVOList(String entityName, String entityId, int maxRows, Database db) throws SystemException, Exception
	{
	    List<RegistryVO> matchingRegistryVOList = new ArrayList<RegistryVO>();

		OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2 ORDER BY r.registryId");
		oql.bind(entityName);
		oql.bind(entityId);

		QueryResults results = oql.execute(Database.ReadOnly);

		int i = 0;
		while (results.hasMore() && (maxRows == -1 || i < maxRows)) 
        {
            Registry registry = (Registry)results.next();
            RegistryVO registryVO = registry.getValueObject();

            matchingRegistryVOList.add(registryVO);

            i++;
        }

		results.close();
		oql.close();

		return matchingRegistryVOList;
	}

	public List<BaseEntityVO> getReferencedObjects(String referencingEntityName, String referencingEntityId) throws SystemException, Exception
	{
	    List<BaseEntityVO> result = new ArrayList<BaseEntityVO>();

        Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			List<RegistryVO> registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);

			Iterator<RegistryVO> i = registryVOList.iterator();
			while(i.hasNext())
			{
			    RegistryVO registryVO = (RegistryVO)i.next();
			    if(registryVO.getEntityName().indexOf("Content") > -1)
	            {
	                try
	                {
	                    Content content = ContentController.getContentController().getContentWithId(new Integer(registryVO.getEntityId()), db);
			    		logger.info("contentVersion:" + content.getContentId());
			    		result.add(content.getValueObject());
	                }
	                catch(Exception e)
	                {
	                    logger.info("content:" + registryVO.getEntityId() + " did not exist - skipping..");
	                }
	            }
	            else if(registryVO.getEntityName().indexOf("SiteNode") > -1)
	            {
	                try
	                {
		                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(registryVO.getEntityId()), db);
			    		logger.info("siteNode:" + siteNode.getId());
			    		result.add(siteNode.getValueObject());
			    	}
	                catch(Exception e)
	                {
	                    logger.info("siteNode:" + registryVO.getEntityId() + " did not exist - skipping..");
	                }
	            }
			}

			commitTransaction(db);
		}
		catch (Exception e)
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}

		return result;
	}

	public List<BaseEntityVO> getReferencedObjects(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List<BaseEntityVO> result = new ArrayList<BaseEntityVO>();

		List<RegistryVO> registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);

		Iterator<RegistryVO> i = registryVOList.iterator();
		while(i.hasNext())
		{
		    RegistryVO registryVO = (RegistryVO)i.next();
		    if(registryVO.getEntityName().indexOf("Content") > -1)
            {
                try
                {
                    Content content = ContentController.getContentController().getContentWithId(new Integer(registryVO.getEntityId()), db);
		    		logger.info("contentVersion:" + content.getContentId());
		    		result.add(content.getValueObject());
                }
                catch(Exception e)
                {
                    logger.info("content:" + registryVO.getEntityId() + " did not exist - skipping..");
                }
            }
            else if(registryVO.getEntityName().indexOf("SiteNode") > -1)
            {
                try
                {
	                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(registryVO.getEntityId()), db);
		    		logger.info("siteNode:" + siteNode.getId());
		    		result.add(siteNode.getValueObject());
		    	}
                catch(Exception e)
                {
                    logger.info("siteNode:" + registryVO.getEntityId() + " did not exist - skipping..");
                }
            }
		}

		return result;
	}

	/**
	 * Gets matching references
	 */
	public List<RegistryVO> getMatchingRegistryVOListForReferencingEntity(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List<RegistryVO> matchingRegistryVOList = new ArrayList<RegistryVO>();

	    logger.info("referencingEntityName:" + referencingEntityName);
	    logger.info("referencingEntityId:" + referencingEntityId);

	    OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
		oql.bind(referencingEntityName);
		oql.bind(referencingEntityId);

		QueryResults results = oql.execute(Database.ReadOnly);

		while (results.hasMore()) 
        {
            Registry registry = (Registry)results.next();
            RegistryVO registryVO = registry.getValueObject();
    	    logger.info("found match:" + registryVO.getEntityName() + ":" + registryVO.getEntityId());

            matchingRegistryVOList.add(registryVO);
        }

		results.close();
		oql.close();

		return matchingRegistryVOList;
	}

	/**
	 * Gets matching references
	 */

	public List<RegistryVO> clearRegistryVOList(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List<RegistryVO> matchingRegistryVOList = new ArrayList<RegistryVO>();

		OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
		oql.bind(referencingEntityName);
		oql.bind(referencingEntityId);

		QueryResults results = oql.execute();

		while (results.hasMore())
        {
            Registry registry = (Registry)results.next();
            db.remove(registry);
        }

		results.close();
		oql.close();

		return matchingRegistryVOList;
	}

	/**
	 * Gets matching references
	 */

	public void clearRegistryForReferencedEntity(String entityName, String entityId) throws SystemException, Exception
	{
		Database db = null;
		try
		{
			db = CastorDatabaseService.getDatabase();
			beginTransaction(db);

			clearRegistryForReferencedEntity(entityName, entityId, db);

	        commitTransaction(db);
		}
		catch (Exception e)
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	public void clearRegistryForReferencedEntity(String entityName, String entityId, Database db) throws SystemException, Exception
	{
		OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2 ORDER BY r.registryId");
		oql.bind(entityName);
		oql.bind(entityId);

		QueryResults results = oql.execute();

		while (results.hasMore()) 
		{
			Registry registry = (Registry)results.next();
			db.remove(registry);
		}

		results.close();
		oql.close();
	}

	public void clearRegistryForReferencingEntityCompletingName(String entityCompletingName, String entityCompletingId) throws SystemException, Exception
	{
		Database db = null;
		try
		{
			db = CastorDatabaseService.getDatabase();
			beginTransaction(db);

			clearRegistryForReferencingEntityCompletingName(entityCompletingName, entityCompletingId, db);

	        commitTransaction(db);
		}
		catch (Exception e)
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	/**
	 * Gets matching references
	 */

	public void clearRegistryForReferencingEntityCompletingName(String entityCompletingName, String entityCompletingId, Database db) throws SystemException, Exception
	{
		OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityCompletingName = $1 AND r.referencingEntityCompletingId = $2 ORDER BY r.registryId");
		oql.bind(entityCompletingName);
		oql.bind(entityCompletingId);

		QueryResults results = oql.execute();

		while (results.hasMore())
		{
			Registry registry = (Registry)results.next();
			db.remove(registry);
		}

		results.close();
		oql.close();
	}

	/**
	 * Gets matching references
	 */

	public void clearRegistryForReferencingEntityName(String entityName, String entityId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();

		try
		{
			beginTransaction(db);

			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
			oql.bind(entityName);
			oql.bind(entityId);

			QueryResults results = oql.execute();

			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            db.remove(registry);
	        }

			results.close();
			oql.close();

	        commitTransaction(db);
		}
		catch (Exception e)
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	/**
	 * Clears all references to a entity
	 */
/*
	public void clearRegistryForReferencedEntity(String entityName, String entityId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			OQLQuery oql = db.getOQLQuery("DELETE FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2");
			oql.bind(entityName);
			oql.bind(entityId);
			QueryResults results = oql.execute();		
		    
	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}
*/
	
	/**
	 * Gets siteNodeVersions which uses the metainfo
	 */
	/*
	public List getSiteNodeVersionsWhichUsesContentVersionAsMetaInfo(ContentVersion contentVersion, Database db) throws SystemException, Exception
	{
	    List siteNodeVersions = new ArrayList();
	    
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.serviceBindings.availableServiceBinding.name = $1 AND snv.serviceBindings.bindingQualifyers.name = $2 AND snv.serviceBindings.bindingQualifyers.value = $3");
	    oql.bind("Meta information");
		oql.bind("contentId");
		oql.bind(contentVersion.getOwningContent().getId());
		
		QueryResults results = oql.execute();
		this.logger.info("Fetching entity in read/write mode");

		while (results.hasMore()) 
        {
		    SiteNodeVersion siteNodeVersion = (SiteNodeVersion)results.next();
		    siteNodeVersions.add(siteNodeVersion);
		    //logger.info("siteNodeVersion:" + siteNodeVersion.getId());
        }
    	
		results.close();
		oql.close();

		return siteNodeVersions;		
	}
	*/

	/**
	 * Gets siteNodeVersions which uses the metainfo
	 */
	public SiteNodeVersion getLatestActiveSiteNodeVersionWhichUsesContentVersionAsMetaInfo(ContentVersion contentVersion, Database db) throws SystemException, Exception
	{
	    SiteNodeVersion siteNodeVersion = null;
	    
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.owningSiteNode.metaInfoContentId = $1 AND snv.isActive = $2 ORDER BY snv.siteNodeVersionId desc");
	    oql.bind(contentVersion.getValueObject().getContentId());
		oql.bind(new Boolean(true));
		
		/*
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.serviceBindings.availableServiceBinding.name = $1 AND snv.serviceBindings.bindingQualifyers.name = $2 AND snv.serviceBindings.bindingQualifyers.value = $3 AND snv.isActive = $4 ORDER BY snv.siteNodeVersionId desc");
	    oql.bind("Meta information");
		oql.bind("contentId");
		oql.bind(contentVersion.getOwningContent().getId());
		oql.bind(new Boolean(true));
		*/
		
		QueryResults results = oql.execute();
		logger.info("Fetching entity in read/write mode");

		if (results.hasMore()) 
        {
		    siteNodeVersion = (SiteNodeVersion)results.next();
        }
    	
		results.close();
		oql.close();

		return siteNodeVersion;		
	}
}
