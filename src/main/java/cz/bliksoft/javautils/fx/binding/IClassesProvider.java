package cz.bliksoft.javautils.fx.binding;

import java.util.List;

/**
 * provide a list of CSS classes to add to item
 */
public interface IClassesProvider {
	List<String> getItemClasses();
}
