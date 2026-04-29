// package cz.bliksoft.javautils.app.ui.components;
//
// import java.awt.Component;
// import java.awt.Container;
// import java.awt.KeyboardFocusManager;
// import java.awt.event.HierarchyEvent;
// import java.awt.event.HierarchyListener;
// import java.beans.PropertyChangeEvent;
// import java.beans.PropertyChangeListener;
//
// import javax.swing.JComponent;
//
// import cz.bliksoft.framework.ui.BSFrameworkUI;
// import cz.bliksoft.javautils.StringUtils;
// import cz.bliksoft.javautils.app.ui.BSAppUIConstants;
// import cz.bliksoft.javautils.validation.view.ValidationComponentUtils;
// import javafx.css.PseudoClass;
// import javafx.scene.Node;
// import javafx.scene.Scene;
// import javafx.scene.control.Label;
//
// public class HintStatusProvider extends Label {
// public static final String HINT_KEY = "hintText";
// public static final String REQUIRED_KEY = "hintRequired";
//
// PseudoClass REQUIRED = PseudoClass.getPseudoClass("required");
// PseudoClass ERROR = PseudoClass.getPseudoClass("error");
// PseudoClass WARNING = PseudoClass.getPseudoClass("warning");
//
// protected JComponent currentField;
//
// public HintStatusProvider() {
// this.getStyleClass().add(BSAppUIConstants.CLASS_STATUSBAR_LABEL);
// Scene scene = null; // FIXME main app scene
// scene.focusOwnerProperty().addListener((obs, oldNode, newNode) -> {
// HintInfo hint = (newNode != null) ? findHint(newNode) : null;
//
// if (hint == null) {
// setText("");
// pseudoClassStateChanged(REQUIRED, false);
// pseudoClassStateChanged(ERROR, false);
// pseudoClassStateChanged(WARNING, false);
// return;
// }
//
// setText(hint.text());
// pseudoClassStateChanged(REQUIRED, hint.required());
// pseudoClassStateChanged(ERROR, "error".equalsIgnoreCase(hint.severity()));
// pseudoClassStateChanged(WARNING, "warning".equalsIgnoreCase(hint.severity()));
// });
// }
//
//
// void updateTextField(JComponent field) {
// String focusHint = (String) ValidationComponentUtils.getInputHint(field);
// // if (!StringUtils.hasText(focusHint)) {
// // focusHint = field.getToolTipText();
// // }
// Container container = field.getParent();
// while ((!StringUtils.hasText(focusHint)) && (container != null)) {
// if (container instanceof JComponent) {
// focusHint = (String) ValidationComponentUtils.getInputHint((JComponent) container);
// if (!StringUtils.hasText(focusHint)) {
// focusHint = ((JComponent) container).getToolTipText();
// }
// }
// container = container.getParent();
// }
// if (StringUtils.hasText(focusHint)) {
// HintStatusProvider.this.setVisible(true);
// if (ValidationComponentUtils.isMandatory(field)) {
// HintStatusProvider.this.setText("<HTML><U>" + focusHint); //$NON-NLS-1$
// } else {
// HintStatusProvider.this.setText("<HTML><I>" + focusHint); //$NON-NLS-1$
// }
// } else {
// HintStatusProvider.this.setVisible(false);
// }
// }
//
// private HierarchyListener hierarchyListener = new HierarchyListener() {
//
// @Override
// public void hierarchyChanged(HierarchyEvent arg0) {
// // Logger.getLogger(HintStatusProvider.class.getName()).info("kontrola hintového
// // fokusu");
// if (HintStatusProvider.this.isVisible()) {
// if ((HintStatusProvider.this.currentField != null)
// && (!HintStatusProvider.this.currentField.isShowing())) {
// HintStatusProvider.this.setVisible(false);
// }
// }
// }
// };
//
// static HintInfo findHint(Node n) {
// while (n != null) {
// Object hint = n.getProperties().get("hintText");
// if (hint instanceof String s && !s.isBlank()) {
// boolean required = Boolean.TRUE.equals(n.getProperties().get("hintRequired"));
// String severity = (String) n.getProperties().getOrDefault("hintSeverity", "info");
// return new HintInfo(s, required, severity);
// }
// n = n.getParent();
// }
// return null;
// }
//
// record HintInfo(String text, boolean required, String severity) {
// }
// }
