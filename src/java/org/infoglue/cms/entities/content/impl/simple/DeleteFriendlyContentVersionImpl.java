package org.infoglue.cms.entities.content.impl.simple;

import java.util.ArrayList;

import org.infoglue.cms.entities.content.ContentCategory;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.kernel.IBaseEntity;

public class DeleteFriendlyContentVersionImpl implements IBaseEntity
{
	private ContentVersionVO valueObject = new ContentVersionVO();

	public Integer getId()
	{
		return getContentVersionId();
	}

	public Object getIdAsObject()
	{
		return getId();
	}

	@Override
	public BaseEntityVO getVO()
	{
		return getValueObject();
	}

	@Override
	public void setVO(BaseEntityVO valueObject)
	{
		setValueObject((ContentVersionVO)valueObject);
	}

	public ContentVersionVO getValueObject()
	{
		return this.valueObject;
	}

	public void setValueObject(ContentVersionVO valueObject)
	{
		this.valueObject = valueObject;
	}

	private java.util.Collection<DeleteFriendlyDigitalAssetImpl> digitalAssets = new ArrayList<DeleteFriendlyDigitalAssetImpl>();
	private java.util.Collection<ContentCategory> contentCategories = new ArrayList<ContentCategory>();
	
	public java.lang.Integer getContentVersionId()
	{
		return this.valueObject.getContentVersionId();
	}

	public void setContentVersionId(java.lang.Integer contentVersionId)
	{
		this.valueObject.setContentVersionId(contentVersionId);
	}

	public java.lang.Integer getStateId()
	{
		return this.valueObject.getStateId();
	}

	public void setStateId(java.lang.Integer stateId)
	{
		this.valueObject.setStateId(stateId);
	}

	public java.lang.Boolean getIsActive()
	{
		return this.valueObject.getIsActive();
	}

	public void setIsActive(java.lang.Boolean isActive)
	{
		this.valueObject.setIsActive(isActive);
	}

	public Integer getContentId()
	{
		return this.valueObject.getContentId();
	}

	public void setContentId(Integer contentId)
	{
		this.valueObject.setContentId(contentId);
	}

	public Integer getLanguageId()
	{
		return this.valueObject.getLanguageId();
	}

	public void setLanguageId(Integer languageId)
	{
		this.valueObject.setLanguageId(languageId);
	}

	public String getVersionModifier()
	{
		return this.valueObject.getVersionModifier();
	}

	public void setVersionModifier(String versionModifier)
	{
		this.valueObject.setVersionModifier(versionModifier);
	}

	public java.util.Collection<DeleteFriendlyDigitalAssetImpl> getDigitalAssets()
	{
		return this.digitalAssets;
	}
	
	public void setDigitalAssets(java.util.Collection<DeleteFriendlyDigitalAssetImpl> digitalAssets)
	{
		this.digitalAssets = digitalAssets;
	}  
	
	public java.util.Collection<ContentCategory> getContentCategories()
	{
	    return this.contentCategories;
	}
	
	public void setContentCategories(java.util.Collection<ContentCategory> contentCategories)
	{
		this.contentCategories = contentCategories;
	}
}
