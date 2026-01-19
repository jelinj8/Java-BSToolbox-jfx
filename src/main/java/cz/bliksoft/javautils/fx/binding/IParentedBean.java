package cz.bliksoft.javautils.fx.binding;

public interface IParentedBean<T> {
	
	T getParent();
	
	void setParent(T parent);
	
}
