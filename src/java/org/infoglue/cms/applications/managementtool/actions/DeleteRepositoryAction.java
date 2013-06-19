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

package org.infoglue.cms.applications.managementtool.actions;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;

/**
 * This action removes a repository from the system.
 * 
 * @author Mattias Bogeblad
 */

public class DeleteRepositoryAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 5317181899049947323L;
	private static final Logger logger = Logger.getLogger(DeleteRepositoryAction.class);

	private RepositoryVO repositoryVO;

	private String returnAddress = null;
	private String processId;
	private String processName;

	public DeleteRepositoryAction()
	{
		this(new RepositoryVO());
	}

	public DeleteRepositoryAction(RepositoryVO repositoryVO) 
	{
		this.repositoryVO = repositoryVO;
	}

	private synchronized String execute(boolean forced) throws SystemException, ConstraintException
	{
		boolean hasAccessToManagementTool = hasAccessTo("ManagementTool.Read");
		if(!hasAccessToManagementTool)
			throw new AccessConstraintException("Repository.delete", "1003");

		this.repositoryVO.setRepositoryId(this.getRepositoryId());
		try
		{
			String processName = DeleteRepositoryAction.class.getName();
			String processId = "Repository_Delete_" + this.getRepositoryId();

			ProcessBean processBean = ProcessBean.getProcessBean(processName, processId, null);

			if (processBean != null)
			{
				logger.info("Delete already in progress for repository.id: " + this.getRepositoryId());
			}
			else
			{
				logger.info("Will start delete for repository.id: " + this.getRepositoryId());
				processBean = ProcessBean.createProcessBean(processName, processId, getInfoGluePrincipal());
				processBean.setAutoRemoveOnSuccess(true);
				RepositoryController.getController().delete(this.repositoryVO, this.getInfoGluePrincipal().getName(), forced, this.getInfoGluePrincipal(), processBean);
			}

			this.returnAddress = "ViewListRepository.action";
			this.processId = processId;
			this.processName = processName;
			return "success";
		}
		catch(ConstraintException ce)
		{
			returnAddress = "ViewRepository.action?repositoryId=" + this.repositoryVO.getId();
			if(ce.getErrorCode().equals("3300") && ce.getFieldName().equals("ContentVersion.stateId"))
				throw new ConstraintException("ContentVersion.stateId", "3307", ce.getExtraInformation());
			else if(ce.getErrorCode().equals("3400") && ce.getFieldName().equals("SiteNodeVersion.stateId"))
				throw new ConstraintException("ContentVersion.stateId", "3406", ce.getExtraInformation());
			else
				throw ce;
		}
	}

	public String doExecute() throws ConstraintException, Exception 
	{
		return execute(false);
	}

	public String doExecuteByForce() throws ConstraintException, Exception 
	{
		return execute(true);
	}

	public void setRepositoryId(Integer repositoryId) throws SystemException
	{
		this.repositoryVO.setRepositoryId(repositoryId);
	}

    public java.lang.Integer getRepositoryId()
    {
        return this.repositoryVO.getRepositoryId();
    }

	public void setReturnAddress(String returnAddress)
	{
		this.returnAddress = returnAddress;
	}

	public String getReturnAddress()
	{
		return this.returnAddress;
	}

	public String getProcessId()
	{
		return processId;
	}

	public String getProcessName()
	{
		return processName;
	}

}
