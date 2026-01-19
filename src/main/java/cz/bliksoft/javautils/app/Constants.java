package cz.bliksoft.javautils.app;

import java.io.File;

//import org.apache.logging.log4j.Marker;
//import org.apache.logging.log4j.MarkerManager;

public class Constants {
	public static final File USER_APPDIR = new File(System.getProperty("user.dir"));//$NON-NLS-1$ //$NON-NLS-2$
	public static final File USER_HOMEDIR = new File(System.getProperty("user.home"));//$NON-NLS-1$ //$NON-NLS-2$
		
	public static final String DEFAULT_PROPERTIES_FILENAME = "settings.xml";

//	public static final String WORKPLACE_ID_KEY = "workplaceIdentification"; //$NON-NLS-1$
//
//	//public static final String FULL_DATE_PATTERN = "^((((0?[1-9]|[12]\\d|3[01])[\\.\\,\\-\\/](0?[13578]|1[02])[\\.\\,\\-\\/]((1[6-9]|[2-9]\\d)?\\d{2}))|((0?[1-9]|[12]\\d|30)[\\.\\,\\-\\/](0?[13456789]|1[012])[\\.\\,\\-\\/]((1[6-9]|[2-9]\\d)?\\d{2}))|((0?[1-9]|1\\d|2[0-8])[\\.\\,\\-\\/]0?2[\\.\\,\\-\\/]((1[6-9]|[2-9]\\d)?\\d{2}))|(29[\\.\\,\\-\\/]0?2[\\.\\,\\-\\/]((1[6-9]|[2-9]\\d)?(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)|00)))|(((0[1-9]|[12]\\d|3[01])(0[13578]|1[02])((1[6-9]|[2-9]\\d)?\\d{2}))|((0[1-9]|[12]\\d|30)(0[13456789]|1[012])((1[6-9]|[2-9]\\d)?\\d{2}))|((0[1-9]|1\\d|2[0-8])02((1[6-9]|[2-9]\\d)?\\d{2}))|(2902((1[6-9]|[2-9]\\d)?(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)|00))))$";
//
//	// FIXME lokalizace!!
//	public static final String FULL_DATE_PATTERN = "^(?<day>[0123]?\\d)[\\.\\,\\-\\/](?<month>[01]?\\d)[\\.\\,\\-\\/](?<year>[0-9]{2}|[12][0-9]{3})?$";
//	public static final String EDIT_DATE_PATTERN = "^(?<day>[0123]\\d{1})(?<month>[01]\\d)(?<year>[0-9]{2}|[12][0-9]{3})?$";
//	public static final String EDIT_DATE_RELATIVE = "^(?<direction>[\\+\\-])(?<count>\\d*)(?<step>[dtmrDMTR])?$";
//	public static final String EDIT_DATE_DAY_RELATIVE = "^(?<direction>[\\+\\-])(?<count>\\d*)(?<day>(PO)|(UT)|(ST)|(CT)|(PA)|(SO)|(NE)|(po)|(ut)|(st)|(ct)|(pa)|(so)|(ne))?$";
//	public static final String NID_PATTERN = "^(?<PART1>(?<YEAR>\\d{2})(?<MONTH>\\d{2})(?<DAY>\\d{2}))([\\/])?(?<PART2>\\d{3,4})$";
//
//	/**
//	 * oddělení parametrů u hromadné specifikace getterů
//	 */
//	public static final String PROPERTY_GETTER_SPLITTER = ";;"; //$NON-NLS-1$
//
//	/**
//	 * oddělení getterů (parent.child.subchild...)
//	 */
//	public static final String PROPERTY_DOT_SPLITTER = "."; //$NON-NLS-1$
//
//	//	/**
//	//	 * 
//	//	 */
//	//	public static final String PROPERTY_METHOD_SPLITTER = ";;"; //$NON-NLS-1$
//
//	public static final String CONDITION_PLACEHOLDER = "{CONDITION}";
//
//	public enum ListenerPriority {
//		FIRST, NORMAL, LAST
//	}
//
//	public static final Marker LOG_MARKER_SQL = MarkerManager.getMarker("SQL");
//	public static final Marker LOG_MARKER_ENTITY = MarkerManager.getMarker("ENTITY");
	
}
