package org.infoglue.cms.entities.content.impl.simple;

import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.kernel.IBaseEntity;

public class DeleteFriendlyDigitalAssetImpl implements IBaseEntity
{
	private DigitalAssetVO valueObject = new DigitalAssetVO();
	private java.util.Collection<DeleteFriendlyContentVersionImpl> contentVersions;

	public Integer getId()
	{
		return getDigitalAssetId();
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
		setValueObject((DigitalAssetVO)valueObject);
	}

	public DigitalAssetVO getValueObject()
	{
		return this.valueObject;
	}

	public void setValueObject(DigitalAssetVO valueObject)
	{
		this.valueObject = valueObject;
	}

	public java.lang.Integer getDigitalAssetId()
	{
		return this.valueObject.getDigitalAssetId();
	}

	public void setDigitalAssetId(java.lang.Integer digitalAssetId)
	{
		this.valueObject.setDigitalAssetId(digitalAssetId);
	}

	public java.lang.String getAssetKey()
	{
		return this.valueObject.getAssetKey();
	}

	public void setAssetKey(java.lang.String assetKey)
	{
		this.valueObject.setAssetKey(assetKey);
	}

	public java.util.Collection<DeleteFriendlyContentVersionImpl> getContentVersions()
	{
		return contentVersions;
	}

	public void setContentVersions(java.util.Collection<DeleteFriendlyContentVersionImpl> contentVersions)
	{
		this.contentVersions = contentVersions;
	}
}
