package cz.bliksoft.javautils.fx.controls.dialogs.busy;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Example extends Application {
	@Override
	public void start(Stage stage) {
		BusyDialogModel model = new BusyDialogModel();

		VBox content = new VBox(10);
		Button run = new Button("Run background task");
		content.getChildren().add(run);

		StackPane root = new StackPane(content);

		BusyOverlay overlay = new BusyOverlay(model);
		root.getChildren().add(overlay);

		// Example frames (optional):
		// model.getFrames().setAll(
		// new Image(getClass().getResource("/busy/frame1.png").toExternalForm()),
		// new Image(getClass().getResource("/busy/frame2.png").toExternalForm()),
		// ...
		// );

//		run.setOnAction(e -> {
//			Task<Void> task = new Task<>() {
//				@Override
//				protected Void call() throws Exception {
//					model.setShowingFx(true);
//					model.setTitleFx("Importing data");
//					model.setIndeterminateFx(false);
//					model.setProgressFx(0, 100);
//
//					for (int i = 1; i <= 100; i++) {
//						if (isCancelled())
//							break;
//						model.setDetailFx("Processing item " + i);
//						model.setProgressFx(i, 100);
//						Thread.sleep(30);
//					}
//					return null;
//				}
//
//				@Override
//				protected void succeeded() {
//					model.setShowingFx(false);
//				}
//
//				@Override
//				protected void failed() {
//					model.setDetailFx(getException().toString());
//					model.setShowingFx(false);
//				}
//
//				@Override
//				protected void cancelled() {
//					model.setShowingFx(false);
//				}
//			};
//			Thread t = new Thread(task, "worker");
//			t.setDaemon(true);
//			t.start();
//		});

		test(model);

		Scene scene = new Scene(root, 900, 600);
		stage.setScene(scene);
		stage.setTitle("Busy Overlay Demo");
		stage.show();
	}

	public static void test(BusyDialogModel model) {

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Importing data");
				updateMessage("Preparing…");

				// Indeterminate:
				updateProgress(-1, 1);
				Thread.sleep(500);

				int n = 200;
				for (int i = 1; i <= n; i++) {
					if (isCancelled())
						break;

					updateTitle("Importing data");
					updateMessage("Processing item " + i + " of " + n);

					// Determinate:
					updateProgress(i, n);

					Thread.sleep(20);
				}
				updateMessage("Finishing…");
				Thread.sleep(300);
				return null;
			}
		};

		// Hook overlay to task:
		model.showWhileRunning(task);

		// Start it:
		Thread t = new Thread(task, "import-worker");
		t.setDaemon(true);
		t.start();
	}

}
