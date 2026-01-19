package cz.bliksoft.javautils.fx.context;

import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.fx.context.listeners.BasicContextObjectListener;
import javafx.beans.property.Property;

/**
 * Objekt, poskytující vlastní kontext (obvykle entitu k úpravám/zobrazení)
 * 
 * @param <T> typ kontextu
 */
public interface IContextPropertyProvider<T> /* extends IDefaultObservable , ISingleContextProvider<T> */ {

	Property<T> getContextProperty();
	
//	/**
//	 * nastaví nový obsah kontextu
//	 * 
//	 * @param newContext
//	 */
//	default public void setContextValue(T newContext) {
//		getBeanAdapter().setBean(newContext);
//	}
//
//	/**
//	 * aktuální stav kontextu
//	 * 
//	 * @return
//	 */
//	default public T getContextValue() {
//		return (T) getBeanAdapter().getBean();
//	}
//
//	default BeanAdapter<T> getBeanAdapter() {
//		return BeanAdapter.getDefaultBeanAdapter(this);
//	}
	
	/**
	 * přidá listener do přepínaného kontextu a provede inicializaci
	 * 
	 * @param key                       naslouchací klíč
	 * @param additionalContextListener přídavný listener, který má být notifikován
	 *                                  v případě změny
	 */
	default void addDefaultSwitchedContextListener(Class<?> key, AbstractContextListener<T> additionalContextListener) {
		Context.getSwitchedContext().addContextListener(
				new BasicContextObjectListener<T>(key, this, "default switched", additionalContextListener), true);
	}

	/**
	 * přidá listener do přepínaného kontextu a provede inicializaci
	 * 
	 * @param key vyhledávací klíč
	 */
	default void addDefaultSwitchedContextListener(Class<?> key) {
		addDefaultSwitchedContextListener(key, null);
	}

	/**
	 * přidá listener do kořenového (systémového) kontextu a provede inicializaci
	 * 
	 * @param key
	 * @param additionalContextListener přídavný listener, který má být notifikován
	 *                                  v případě změny
	 */
	default void addDefaultGlobalContextListener(Class<?> key, AbstractContextListener<T> additionalContextListener) {
		Context.getGlobal().addContextListener(
				new BasicContextObjectListener<T>(key, this, "default global", additionalContextListener), true);
	}

	/**
	 * přidá listener do kořenového (systémového) kontextu a provede inicializaci
	 * 
	 * @param key
	 */
	default void addDefaultGlobalContextListener(Class<?> key) {
		Context.getGlobal().addContextListener(new BasicContextObjectListener<T>(key, this, "default global", null),
				true);
	}

}
