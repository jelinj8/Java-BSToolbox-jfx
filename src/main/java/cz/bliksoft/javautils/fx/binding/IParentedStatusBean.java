package cz.bliksoft.javautils.fx.binding;

public interface IParentedStatusBean<T extends IStatusBean> extends IParentedBean<T> {
	T getParent();
}
