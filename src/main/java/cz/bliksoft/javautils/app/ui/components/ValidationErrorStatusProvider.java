// package cz.bliksoft.javautils.app.ui.components;
//
// import java.awt.event.MouseAdapter;
// import java.awt.event.MouseEvent;
// import java.beans.PropertyChangeEvent;
// import java.beans.PropertyChangeListener;
//
// import cz.bliksoft.javautils.app.ui.BSAppUIConstants;
// import cz.bliksoft.javautils.context.AbstractContextListener;
// import cz.bliksoft.javautils.context.Context;
// import cz.bliksoft.javautils.context.ContextChangedEvent;
// import cz.bliksoft.javautils.fx.validation.ValidationResult;
// import javafx.scene.control.Label;
//
// public class ValidationErrorStatusProvider extends Label {
//
// private static final String VALIDATION_KEY = "validationKey"; //$NON-NLS-1$
//
// private ValidationResultModel currentModel = null;
//
// public ValidationErrorStatusProvider() {
// this.getStyleClass().add(BSAppUIConstants.CLASS_STATUSBAR_LABEL);
//
// // this.setOpaque(true);
//
// Context.getSwitchedContext().addContextListener(new AbstractContextListener<ValidationResultModel>(
// ValidationResultModel.class, "ValidationErrorStatusProvider") { // $NON-NLS-1$ //$NON-NLS-1$
//
// @Override
// public void fired(ContextChangedEvent<ValidationResultModel> event) {
// if (ValidationErrorStatusProvider.this.currentModel != null) {
// ValidationErrorStatusProvider.this.currentModel.removePropertyChangeListener(
// ValidationErrorStatusProvider.this.validationResultPCHListenerInstance);
// }
// if (event.isNewNotNull()) {
// ValidationErrorStatusProvider.this.currentModel = (ValidationResultModel) event.getNewValue();
// if (ValidationErrorStatusProvider.this.currentModel != null) {
// ValidationErrorStatusProvider.this.currentModel.addPropertyChangeListener(
// ValidationErrorStatusProvider.this.validationResultPCHListenerInstance);
// ValidationErrorStatusProvider.this
// .updateHint(ValidationErrorStatusProvider.this.currentModel.getResult());
// } else {
// ValidationErrorStatusProvider.this.updateHint(null);
// }
// } else {
// ValidationErrorStatusProvider.this.updateHint(null);
// }
// event.blockEventPropagation();
// }
// }, true);
//
// this.setText("validationErrors"); //$NON-NLS-1$
//
// this.addMouseListener(new MouseAdapter() {
//
// @Override
// public void mouseClicked(MouseEvent e) {
// final Object errorKey = ValidationErrorStatusProvider.this.getClientProperty(VALIDATION_KEY);
// // Logger.getLogger(ValidationErrorStatusProvider.class.getName()).info("Searching
// // for error generating component with key set to "+errorKey.toString());
// ComponentUtils.visitComponentTree(BSFrameworkUI.getMainForm(), component -> {
// Object[] keys = ValidationComponentUtils.getMessageKeys((JComponent) component);
// if (keys != null) {
// for (Object o : keys) {
// if (o.equals(errorKey)) {
// component.requestFocusInWindow();// grabFocus();
// break;
// }
// }
// }
//
// });
// }
// });
// }
//
// void updateHint(ValidationResult res) {
// if (res != null) {
// switch (res.getSeverity()) {
// case ERROR:
// ValidationErrorStatusProvider.this.setText(res.getErrors().get(0).formattedText());
// ValidationErrorStatusProvider.this.setIcon(ValidationResultViewFactory.Icons.ERROR_ICON);
// ValidationErrorStatusProvider.this.putClientProperty(VALIDATION_KEY, res.getErrors().get(0).key());
// ValidationErrorStatusProvider.this.setVisible(true);
// break;
// case WARNING:
// ValidationErrorStatusProvider.this.setText(res.getWarnings().get(0).formattedText());
// ValidationErrorStatusProvider.this.setIcon(ValidationResultViewFactory.Icons.WARNING_ICON);
// ValidationErrorStatusProvider.this.putClientProperty(VALIDATION_KEY, res.getWarnings().get(0).key());
// ValidationErrorStatusProvider.this.setVisible(true);
// break;
// case INFO:
// ValidationErrorStatusProvider.this.setText(res.getInfos().get(0).formattedText());
// ValidationErrorStatusProvider.this.setIcon(ValidationResultViewFactory.Icons.INFO_ICON);
// ValidationErrorStatusProvider.this.putClientProperty(VALIDATION_KEY, res.getInfos().get(0).key());
// ValidationErrorStatusProvider.this.setVisible(true);
// break;
// case OK:
// ValidationErrorStatusProvider.this.setText(null); // $NON-NLS-1$
// ValidationErrorStatusProvider.this.setIcon(ValidationResultViewFactory.Icons.CHECK_ICON);
// ValidationErrorStatusProvider.this.putClientProperty(VALIDATION_KEY, null);
// ValidationErrorStatusProvider.this.setVisible(true);
// break;
// }
// } else {
// ValidationErrorStatusProvider.this.setVisible(false);
// }
// }
//
// private ValidationResultPCHListener validationResultPCHListenerInstance = new ValidationResultPCHListener();
//
// class ValidationResultPCHListener implements PropertyChangeListener {
//
// @Override
// public void propertyChange(PropertyChangeEvent evt) {
// // Logger.getLogger(ValidationErrorStatusProvider.class.getName()).info("Updating
// // by listener");
// if (evt.getSource() instanceof ValidationResultModel) {
// ValidationErrorStatusProvider.this.updateHint(((ValidationResultModel) evt.getSource()).getResult());
// }
// }
// }
// }
