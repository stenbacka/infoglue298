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
import org.infoglue.cms.entities.management.SystemUser;
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
import org.infoglue.deliver.util.Timer;


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
	
    public List getRegistryVOList() throws SystemException, Bug
    {
        return getAllVOObjects(RegistryImpl.class, "registryId");
    }

    public List getRegistryVOList(Database db) throws SystemException, Bug
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

    public List<ReferenceBean> delete(String[] registryIds, InfoGluePrincipal principal, boolean clean, boolean onlyLatest) throws ConstraintException, SystemException
    {
    	Database db = CastorDatabaseService.getDatabase();
		List<ReferenceBean> references = null;
		try
		{
			beginTransaction(db);

			delete(registryIds, principal, clean, onlyLatest, db);

		    commitTransaction(db);
//		    commitRegistryAwareTransaction(db);
		}
		catch (Throwable e)
		{
		    logger.error("Failed to delete registries in list: " + Arrays.toString(registryIds) + ". Message: " + e.getMessage());
		    rollbackTransaction(db);
		}
		return references;
    }

    public List<ReferenceBean> delete(String[] registryIds, InfoGluePrincipal principal, boolean clean, boolean onlyLatest, Database db) throws Throwable
    {
    	if (clean)
    	{
	    	Map<ContentVersionVO, RegistryVO> contentVersionRegistryPair = extractContentVersionsFromRegistryList(registryIds, onlyLatest, db);
			InconsistenciesController.getController().removeContentReferences(contentVersionRegistryPair, principal, db);
			Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = extractSiteNodesFromRegistryList(registryIds, db);
			InconsistenciesController.getController().removeSiteNodeReferences(siteNodeRegistryPair, principal, db);
    	}
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();
		List<ReferenceBean> references = new ArrayList<ReferenceBean>();
		for (String registryIdString : registryIds)
    	{
			RegistryVO registryVO = getRegistryVOWithId(new Integer(registryIdString), db);
			if (logger.isInfoEnabled())
			{
				logger.info("About to remove registry bean. Referencing-type: " + registryVO.getReferencingEntityName() + ", referencing-id: " + registryVO.getReferencingEntityId());
			}
			ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, onlyLatest, db);
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

    private Map<ContentVersionVO, RegistryVO> extractContentVersionsFromRegistryList(List<RegistryVO> registryVOs, boolean onlyLatest, Database db) throws Exception
    {
		Map<ContentVersionVO, RegistryVO> versionRegistryPair = new HashMap<ContentVersionVO, RegistryVO>();
    	for (RegistryVO registryVO : registryVOs)
		{
			extractContentVersionFromRegistry(versionRegistryPair, registryVO, onlyLatest, db);
		}
    	if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + versionRegistryPair.size() + " ContentVersions from " + registryVOs.size() + " registry entries");
		}
    	return versionRegistryPair;
    }

	private Map<ContentVersionVO, RegistryVO> extractContentVersionsFromRegistryList(String[] registryIds, boolean onlyLatest, Database db) throws Throwable
	{
		Map<ContentVersionVO, RegistryVO> versionRegistryPair = new HashMap<ContentVersionVO, RegistryVO>();

		Integer registryId;
		RegistryVO registryVO;
		for (String registryIdString : registryIds)
		{
			registryId = new Integer(registryIdString);
			registryVO = RegistryController.getController().getRegistryVOWithId(registryId, db);
			extractContentVersionFromRegistry(versionRegistryPair, registryVO, onlyLatest, db);
		}

		if (logger.isInfoEnabled())
		{
			logger.info("Extracted " + versionRegistryPair.size() + " ContentVersions from " + registryIds.length + " registry entries");
		}
		return versionRegistryPair;
	}

	private void extractContentVersionFromRegistry(Map<ContentVersionVO, RegistryVO> versionRegistryPair, RegistryVO registryVO, boolean onlyLatest, Database db) throws SystemException, Bug, Exception
	{
		String referencingEntityName = registryVO.getReferencingEntityName();
		String referencingEntityCompletingName = registryVO.getReferencingEntityCompletingName();
		if (referencingEntityCompletingName.equals(Content.class.getName()) && referencingEntityName.equals(ContentVersion.class.getName()))
		{
			Integer referencingEntityId = new Integer(registryVO.getReferencingEntityId());
			try
			{
				ContentVersionVO currentCVVO = ContentVersionController.getContentVersionController().getContentVersionVOWithId(referencingEntityId, db);
				if (!onlyLatest)
				{
					versionRegistryPair.put(currentCVVO, registryVO);
				}
				else
				{
					ContentVersionVO latestContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(currentCVVO.getContentId(), currentCVVO.getLanguageId(), db);
					if (currentCVVO.equals(latestContentVersion))
					{
						versionRegistryPair.put(currentCVVO, registryVO);
					}
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

    public List<ReferenceBean> deleteAllForSiteNode(Integer siteNodeId, InfoGluePrincipal principal) throws SystemException
    {
    	return deleteAllForSiteNode(siteNodeId, principal, false, false);
    }

    public List<ReferenceBean> deleteAllForSiteNode(Integer siteNodeId, InfoGluePrincipal principal, boolean clean, boolean onlyLatest) throws SystemException
    {
    	Database db = CastorDatabaseService.getDatabase();
		List<ReferenceBean> references = null;
    	try
    	{
    		beginTransaction(db);

    		deleteAllForSiteNode(siteNodeId, principal, clean, onlyLatest, db);

    		commitTransaction(db);
    	}
    	catch (Throwable ex)
    	{
    		logger.error("Failed to a SiteNode's references. Message: " + ex.getMessage() + ". Type: " + ex.getClass());
    		logger.warn("Failed to a SiteNode's references.", ex);
    		rollbackTransaction(db);
    	}
    	return references;
    }

    public List<ReferenceBean> deleteAllForSiteNode(Integer siteNodeId, InfoGluePrincipal principal, boolean clean, boolean onlyLatest, Database db) throws Exception
    {
    	@SuppressWarnings("unchecked")
		List<RegistryVO> registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), -1, db);
		if (clean)
		{
	    	Map<ContentVersionVO, RegistryVO> contentVersionRegistryPair = extractContentVersionsFromRegistryList(registryEntires, onlyLatest, db);
			InconsistenciesController.getController().removeContentReferences(contentVersionRegistryPair, principal, db);
			Map<SiteNodeVO, RegistryVO> siteNodeRegistryPair = extractSiteNodesFromRegistryList(registryEntires, db);
			InconsistenciesController.getController().removeSiteNodeReferences(siteNodeRegistryPair, principal, db);
		}
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();
		List<ReferenceBean> references = new ArrayList<ReferenceBean>();
    	for (RegistryVO registryVO : registryEntires)
    	{
    		if (logger.isInfoEnabled())
			{
				logger.info("About to remove registry bean. Referencing-type: " + registryVO.getReferencingEntityName() + ", referencing-id: " + registryVO.getReferencingEntityId());
			}
			ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, onlyLatest, db);
			if (referenceBean != null)
			{
				references.add(referenceBean);
			}
			try
			{
				Registry registry = (Registry) getVOWithId(RegistryImpl.class, registryVO.getRegistryId(), db);
				db.remove(registry);
			}
			catch (Throwable tr)
			{
				logger.info("Could not find registry. Maybe it was deleted already. Registry.id " + registryVO.getRegistryId());
			}
    	}
    	return references;
    }

	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersionVO contentVersionVO) throws ConstraintException, SystemException
	{
	    String versionValue = contentVersionVO.getVersionValue();
	    
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

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId) throws SystemException
    {
		return getReferencingObjectsForContent(contentId, -1, true);
    }

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows) throws SystemException
	{
		return getReferencingObjectsForContent(contentId, maxRows, true);
	}

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, boolean excludeInternalContentReferences) throws SystemException
    {
		List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();
        
		Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			referenceBeanList = getReferencingObjectsForContent(contentId, maxRows, excludeInternalContentReferences, db);
	    
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

	public Set<SiteNodeVO> getReferencingSiteNodesForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
    {
		Timer t = new Timer();
        Set<SiteNodeVO> referenceBeanList = new HashSet<SiteNodeVO>();

        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
        //t.printElapsedTime("registryEntires:" + registryEntires.size());
        logger.info("registryEntires:" + registryEntires.size());
        Iterator registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
        	RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
        	if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
        		continue;
        	
        	logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());

            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            try
            {
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

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
	{
		return getReferencingObjectsForContent(contentId, maxRows, true, db);
	}

	public List<ReferenceBean> getReferencingObjectsForContent(Integer contentId, int maxRows, boolean excludeInternalContentReferences, Database db) throws SystemException, Exception
    {
        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

        Map entries = new HashMap();
		
        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
        logger.info("registryEntires:" + registryEntires.size());
        Iterator registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            boolean add = true;
            
            String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
            //String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
            if(existingReferenceBean == null)
            {
                
                existingReferenceBean = new ReferenceBean();
	            logger.info("Adding referenceBean to entries with key:" + key);
	            entries.put(key, existingReferenceBean);
	            referenceBeanList.add(existingReferenceBean);
	        }

            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
            {
                try
                {
                    ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("contentVersion:" + contentVersion.getContentVersionId());
		    		existingReferenceBean.setName(contentVersion.getOwningContent().getName());
		    		existingReferenceBean.setReferencingCompletingObject(contentVersion.getOwningContent().getValueObject());

		    		referenceVersionBean.setReferencingObject(contentVersion.getValueObject());
		    		referenceVersionBean.getRegistryVOList().add(registryVO);

		    		existingReferenceBean.setPath(ContentController.getContentController().getContentPath(contentVersion.getValueObject().getContentId(), true, true, db));
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            else
            {
                try
                {
	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		logger.info("siteNode:" + siteNodeVersion.getOwningSiteNode().getId());
		    		existingReferenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
		    		existingReferenceBean.setReferencingCompletingObject(siteNodeVersion.getOwningSiteNode().getValueObject());

		    		referenceVersionBean.setReferencingObject(siteNodeVersion.getValueObject());
		    		referenceVersionBean.getRegistryVOList().add(registryVO);

		    		existingReferenceBean.setPath(SiteNodeController.getController().getSiteNodePath(siteNodeVersion.getValueObject().getSiteNodeId(), true, true, db));
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            
            if(add)
            {
                boolean exists = false;
                ReferenceVersionBean existingReferenceVersionBean = null;
	            Iterator versionsIterator = existingReferenceBean.getVersions().iterator();
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

            }
            
        }
        
        Iterator i = referenceBeanList.iterator();
        while(i.hasNext())
        {
            ReferenceBean referenceBean = (ReferenceBean)i.next();
            if(referenceBean.getVersions().size() == 0)
                i.remove();
        }
	    
		logger.info("referenceBeanList:" + referenceBeanList.size());
		
        return referenceBeanList;
    }
    
    
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
		SystemUser su = SystemUserController.getController().getSystemUserWithName(contentVO.getCreatorName(), db);

		if (su == null)
		{
			return "";
		}
		else
		{
			return su.getEmail();
		}
	}

	/**
     * Attempts to get an email address to the contact person of the given SiteNode.
     *
     * First the Meta info is examined for a value of an attribute specified by {@link CmsPropertyHandler#getContactPersonEmailMetaInfoAttribute()}
     * and if nothing is found the SiteNode creator's email is used. An empty String is returned if no value could be found.
     */
    protected String getContactPersonEmail(SiteNodeVO siteNode, Database db) throws SystemException, Exception
	{
    	String contactPersonEmailMetaInfoAttribute = CmsPropertyHandler.getContactPersonEmailMetaInfoAttribute();

    	if (contactPersonEmailMetaInfoAttribute != null && !contactPersonEmailMetaInfoAttribute.equals(""))
    	{
	    	LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNode.getRepositoryId(), db);
			if (masterLanguage != null)
			{
	    		ContentVersionVO metaInfoContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNode.getMetaInfoContentId(), masterLanguage.getId(), db);
	    		if (metaInfoContentVersion != null)
	    		{
	    			String contactPersonEmail = ContentVersionController.getContentVersionController().getAttributeValue(metaInfoContentVersion, contactPersonEmailMetaInfoAttribute, false);
	    			if (contactPersonEmail != null && !contactPersonEmail.equals(""))
	    			{
		    			if (logger.isDebugEnabled())
		    			{
		    				logger.debug("Reading contact person email from: SiteNode, meta info, master language. Id: " + siteNode.getId());
		    			}
		    			return contactPersonEmail;
	    			}
	    		}
			}

			@SuppressWarnings("unchecked")
			List<LanguageVO> repositoryLanguages = LanguageController.getController().getLanguageVOList(siteNode.getRepositoryId(), db);
			for (LanguageVO language : repositoryLanguages)
			{
				ContentVersionVO metaInfoContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNode.getMetaInfoContentId(), language.getId(), db);
	    		if (metaInfoContentVersion != null)
	    		{
	    			String contactPersonEmail = ContentVersionController.getContentVersionController().getAttributeValue(metaInfoContentVersion, contactPersonEmailMetaInfoAttribute, false);
	    			if (contactPersonEmail != null && !contactPersonEmail.equals(""))
	    			{
		    			if (logger.isDebugEnabled() && contactPersonEmail != null)
		    			{
		    				if (logger.isDebugEnabled())
			    			{
		    					logger.debug("Reading contact person email from: SiteNode, meta info, language. Language-id: " + language.getId() + ", Id: " + siteNode.getId());
			    			}
		    				return contactPersonEmail;
		    			}
		    		}
	    		}
			}
    	}

		SystemUser creator = SystemUserController.getController().getSystemUserWithName(siteNode.getCreatorName(), db);
		if (creator != null)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Reading contact person email from: SiteNode, system user. SystemUser: " + creator.getUserName() + ", Id: " + siteNode.getId());
			}
			return creator.getEmail();
		}

		logger.debug("Found no contact person email. SiteNode.id: " + siteNode.getId());
		return "";
	}

    public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId) throws SystemException, Exception
    {
    	return getReferencingObjectsForSiteNode(siteNodeId, -1, false);
    }

	public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, boolean onlyLatestVersion) throws SystemException, Exception
	{
    	return getReferencingObjectsForSiteNode(siteNodeId, -1, onlyLatestVersion);
	}

    public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows) throws SystemException, Exception
    {
    	return getReferencingObjectsForSiteNode(siteNodeId, maxRows, false);
    }

	public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows, boolean onlyLatestVersion) throws SystemException, Exception
	{
        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

        Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

		    referenceBeanList = getReferencingObjectsForSiteNode(siteNodeId, maxRows, onlyLatestVersion, db);

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

    public List<ReferenceBean> getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows, boolean onlyLatestVersion, Database db) throws SystemException, Exception
    {
        List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();
        
		Map<String, ReferenceBean> entries = new HashMap<String, ReferenceBean>();

		@SuppressWarnings("unchecked")
		List<RegistryVO> registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), maxRows, db);
        Iterator<RegistryVO> registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
            RegistryVO registryVO = registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            ReferenceBean referenceBean = getReferenceBeanFromRegistryVO(registryVO, entries, onlyLatestVersion, db);
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

    private ReferenceBean getReferenceBeanFromRegistryVO(RegistryVO registryVO, Map<String, ReferenceBean> entries, boolean onlyLatestVersion, Database db)
    {
    	ReferenceBean result = null;
            boolean add = true;

            String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
            //String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
            if(existingReferenceBean == null)
            {
                existingReferenceBean = new ReferenceBean();
	            logger.info("Adding referenceBean to entries with key:" + key);
	            entries.put(key, existingReferenceBean);
//            referenceBeanList.add(existingReferenceBean);
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
					ContentVersionVO contentVersion = ContentVersionController.getContentVersionController().getContentVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
					if (!contentVersion.getIsActive())
					{
						add = false;
						logger.debug("ContentVersion was not active. Will not add to reference list. ContentVersion.id: " + contentVersion.getContentVersionId());
					}
					else
					{
						ContentVO contentVO = ContentController.getContentController().getContentVOWithId(contentVersion.getContentId(), db);
						if (onlyLatestVersion)
		                {
		                	ContentVersionVO latestContentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVersion.getContentId(), contentVersion.getLanguageId(), db);
		                			//ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVersion.getContentId(), contentVersion.getLanguageId(), ContentVersionVO.WORKING_STATE, db);
		                	if (logger.isDebugEnabled())
		                	{
		                		logger.debug("Latest version in working state for content.id: " + contentVersion.getContentId() + ". latestContentVersion.id: " + (latestContentVersion == null ? "null" : latestContentVersion.getContentVersionId()));
		                	}

		                	if (latestContentVersion != null && latestContentVersion.getContentVersionId().intValue() != contentVersion.getContentVersionId().intValue())
		                	{
		                		logger.debug("ContentVersion was not latest version. Will not add. ContentVersion-Id: " + contentVersion.getId());
								add = false;
		                	}
		                }
		                if (add)
		                {
				    		existingReferenceBean.setName(contentVO.getName());
				    		existingReferenceBean.setPath(ContentController.getContentController().getContentPath(contentVO.getContentId(), true, true, db));
				    		existingReferenceBean.setReferencingCompletingObject(contentVO);

				    		String contactPersonEmail = getContactPersonEmail(contentVO, db);
				    		existingReferenceBean.setContactPersonEmail(contactPersonEmail);

				    		referenceVersionBean.setReferencingObject(contentVersion);
				    		referenceVersionBean.getRegistryVOList().add(registryVO);
						}
					}
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            else
            {
                try
                {
	            	logger.debug("RegistryVO references SiteNode");
					SiteNodeVersionVO siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
					if (!siteNodeVersion.getIsActive())
					{
						add = false;
						logger.debug("SiteNodeVersion was not active. Will not add to reference list. SiteNodeVersion.id: " + siteNodeVersion.getSiteNodeVersionId());
					}
					else
					{
						SiteNodeVO siteNodeVO = SiteNodeController.getSiteNodeVOWithId(siteNodeVersion.getSiteNodeId(), db);
		            	if (onlyLatestVersion)
		            	{
		            		SiteNodeVersionVO latestSiteNodeVersion = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersionVO(db, siteNodeVersion.getSiteNodeId());
		            				//SiteNodeVersionController.getController().getLatestActiveSiteNodeVersionVO(db, siteNodeVersion.getSiteNodeId(), SiteNodeVersionVO.WORKING_STATE)
		            		if (latestSiteNodeVersion != null && latestSiteNodeVersion.getSiteNodeVersionId() != siteNodeVersion.getSiteNodeVersionId())
		                	{
		                		logger.debug("ContentVersion was not latest version. Will not add. ContentVersion-Id: " + siteNodeVersion.getId());
								add = false;
		                	}
		            	}

		            	if (add)
		            	{
				    		existingReferenceBean.setName(siteNodeVO.getName());
				    		existingReferenceBean.setPath(SiteNodeController.getController().getSiteNodePath(siteNodeVO.getSiteNodeId(), true, true, db));
				    		existingReferenceBean.setReferencingCompletingObject(siteNodeVO);
				    		referenceVersionBean.setReferencingObject(siteNodeVersion);

				    		String contactPersonEmail = getContactPersonEmail(siteNodeVO, db);
				    		existingReferenceBean.setContactPersonEmail(contactPersonEmail);

				    		referenceVersionBean.getRegistryVOList().add(registryVO);
		                }
					}
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
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

	/**
	 * Gets matching references
	 */
	
	public List getMatchingRegistryVOList(String entityName, String entityId, int maxRows, Database db) throws SystemException, Exception
	{
	    List matchingRegistryVOList = new ArrayList();
	    
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
	
	
	public List getReferencedObjects(String referencingEntityName, String referencingEntityId) throws SystemException, Exception
	{
	    List result = new ArrayList();
	    
        Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
		
			List registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);
		    
			Iterator i = registryVOList.iterator();
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

	public List getReferencedObjects(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List result = new ArrayList();
	    
		List registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);
	    
		Iterator i = registryVOList.iterator();
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
	
	public List getMatchingRegistryVOListForReferencingEntity(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List matchingRegistryVOList = new ArrayList();

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
	
	public List clearRegistryVOList(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List matchingRegistryVOList = new ArrayList();
	    
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
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
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
	
	public void clearRegistryForReferencingEntityCompletingName(String entityCompletingName, String entityCompletingId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
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
		this.logger.info("Fetching entity in read/write mode");

		if (results.hasMore()) 
        {
		    siteNodeVersion = (SiteNodeVersion)results.next();
        }
    	
		results.close();
		oql.close();

		return siteNodeVersion;		
	}
}
