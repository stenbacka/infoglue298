package org.infoglue.cms.applications.databeans;

import java.util.List;

public class RelationReferenceBean extends ReferenceBean
{
	private boolean isDeleted;
	private Integer repositoryId;
	private List<ReferenceBean> references;

	public boolean isDeleted()
	{
		return isDeleted;
	}
	public void setDeleted(boolean isDeleted)
	{
		this.isDeleted = isDeleted;
	}
	public Integer getRepositoryId()
	{
		return repositoryId;
	}
	public void setRepositoryId(Integer repositoryId)
	{
		this.repositoryId = repositoryId;
	}
	public List<ReferenceBean> getReferences()
	{
		return references;
	}
	public void setReferences(List<ReferenceBean> references)
	{
		this.references = references;
	}
}
