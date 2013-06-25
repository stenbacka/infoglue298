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

package org.infoglue.cms.applications.structuretool.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.databeans.ReferenceBean;
import org.infoglue.cms.controllers.kernel.impl.simple.RegistryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController.DeleteSiteNodeParams;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeControllerProxy;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.util.CmsPropertyHandler;


/**
 * This action removes a siteNode from the system.
 * 
 * @author Mattias Bogeblad
 */

public class DeleteSiteNodeAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 1L;
    private final static Logger logger = Logger.getLogger(DeleteSiteNodeAction.class.getName());

	private SiteNodeVO siteNodeVO;
//	private Integer siteNodeId;
	private Integer parentSiteNodeId;
	private Integer changeTypeId;
	private Integer repositoryId;
	private String[] registryId;

	//Used for the relatedPages control
	private Integer contentId;

	private List<ReferenceBean> referenceBeanList = new ArrayList<ReferenceBean>();

	public DeleteSiteNodeAction()
	{
		this(new SiteNodeVO());
	}

	public DeleteSiteNodeAction(SiteNodeVO siteNodeVO) 
	{
		this.siteNodeVO = siteNodeVO;
	}

	protected String executeAction(boolean forceDelete) throws Exception
	{
		if (!forceDelete)
		{
			this.referenceBeanList = RegistryController.getController().getReferencingObjectsForSiteNode(this.siteNodeVO.getSiteNodeId());
		}
		if(!forceDelete && this.referenceBeanList != null && this.referenceBeanList.size() > 0)
		{
			return "showRelations";
		}
		else
		{
			try
			{
				this.parentSiteNodeId = SiteNodeController.getParentSiteNode(this.siteNodeVO.getSiteNodeId()).getSiteNodeId();
			}
			catch(Exception e)
			{
				logger.info("The siteNode must have been a root-siteNode because we could not find a parent.");
			}

			DeleteSiteNodeParams params = new DeleteSiteNodeParams();
			params.setForceDelete(forceDelete);
			SiteNodeControllerProxy.getSiteNodeControllerProxy().acDelete(this.getInfoGluePrincipal(), this.siteNodeVO, params);

			return "success";
		}
	}

	protected String doExecute() throws Exception 
	{
		return executeAction(false);
	}

	public String doDeleteReference() throws Exception 
	{
		RegistryController.getController().delete(registryId, this.getInfoGluePrincipal(), true);

	    return executeAction(false);
	}

	public String doDeleteAllReferences() throws Exception
	{
		return executeAction(true);
	}

	public String doFixPage() throws Exception 
	{
	    return "fixPage";
	}

	public String doFixPageHeader() throws Exception 
	{
	    return "fixPageHeader";
	}

	public void setSiteNodeId(Integer siteNodeId)
	{
		this.siteNodeVO.setSiteNodeId(siteNodeId);
	}

	public Integer getOriginalSiteNodeId()
	{
		return this.siteNodeVO.getSiteNodeId();
	}

	public void setParentSiteNodeId(Integer parentSiteNodeId)
	{
		this.parentSiteNodeId = parentSiteNodeId;
	}

	public void setChangeTypeId(Integer changeTypeId)
	{
		this.changeTypeId = changeTypeId;
	}

	public Integer getSiteNodeId()
	{
		return this.parentSiteNodeId;
	}

	public Integer getUnrefreshedSiteNodeId()
	{
		return this.parentSiteNodeId;
	}

	public Integer getChangeTypeId()
	{
		return this.changeTypeId;
	}

	public String getErrorKey()
	{
		return "SiteNodeVersion.stateId";
	}

	public String getReturnAddress()
	{
		return "ViewSiteNode.action?siteNodeId=" + this.siteNodeVO.getId() + "&repositoryId=" + this.siteNodeVO.getRepositoryId();
	}

    public Integer getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Integer repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Integer getContentId()
    {
        return contentId;
    }

    public void setContentId(Integer contentId)
    {
        this.contentId = contentId;
    }

    public List<ReferenceBean> getReferenceBeanList()
    {
        return referenceBeanList;
    }

    public String[] getRegistryId()
    {
        return registryId;
    }

    public void setRegistryId(String[] registryId)
    {
        this.registryId = registryId;
    }

    public boolean getNotifyResponsibleOnReferenceChange()
    {
    	return CmsPropertyHandler.getNotifyResponsibleOnReferenceChange();
    }
}
