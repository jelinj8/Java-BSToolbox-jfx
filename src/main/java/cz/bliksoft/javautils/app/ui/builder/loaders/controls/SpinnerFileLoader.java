package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.collections.FXCollections;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;

public class SpinnerFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		Spinner<?> sp;

		// 1) List spinner: items="a;b;c" (nebo čárky)
		String items = file.getAttribute("items", null);
		if (items != null && !items.isBlank()) {
			List<String> vals = splitItems(items);
			ListSpinnerValueFactory<String> vf = new SpinnerValueFactory.ListSpinnerValueFactory<>(
					FXCollections.observableArrayList(vals));

			String value = file.getAttribute("value", null);
			if (value != null && vals.contains(value)) {
				vf.setValue(value);
			} else if (!vals.isEmpty()) {
				vf.setValue(vals.get(0));
			}

			Spinner<String> s = new Spinner<>();
			s.setValueFactory(vf);
			sp = s;

		} else {
			// 2) Integer range spinner: min/max/value/step
			Integer min = file.getInteger("min", null);
			Integer max = file.getInteger("max", null);
			Integer value = file.getInteger("value", null);
			Integer step = file.getInteger("step", null);

			if (min != null && max != null) {
				int st = (step != null ? step : 1);
				int v = (value != null ? value : min);

				IntegerSpinnerValueFactory vf = new IntegerSpinnerValueFactory(min, max, v, st);
				Spinner<Integer> s = new Spinner<>();
				s.setValueFactory(vf);
				sp = s;
			} else {
				// fallback – bez value factory (můžeš později doplnit)
				sp = new Spinner<>();
			}
		}

		return sp;
	}

	private static List<String> splitItems(String items) {
		// podporuj "a;b;c" i "a,b,c"
		String norm = items.replace('\n', ';').replace('\r', ';');
		String[] parts = norm.contains(";") ? norm.split(";") : norm.split(",");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			String t = p.trim();
			if (!t.isEmpty())
				out.add(t);
		}
		return out;
	}

	@Override
	public String getSupportedType() {
		return "Spinner";
	}
}
