package cz.bliksoft.javautils.fx.tools;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PropertyAnimator<T> {
	private static final Logger log = LogManager.getLogger();

	private Method setter;
	private Object subject;

	private T stillValue;
	private T[] blinkValues;

	private final PauseTransition timer;
	private int valueIndex = 0;

	private boolean reverse = false;

	private Integer currentRepeatCount = null;
	/**
	 * repeat == null -> run once (stop when sequence ends) repeat == 0 -> infinite
	 * repeat > 0 -> repeat that many times (approx. like your original logic)
	 */
	private Integer repeat = 0;

	private Runnable doOnStop = null;

	public PropertyAnimator(Object _subject, String _setter, int intervalMillis, T _stillValue, T[] _blinkValues,
			Class<?> type) {
		subject = _subject;
		stillValue = _stillValue;
		blinkValues = _blinkValues;

		Method found = null;
		try {
			found = _subject.getClass().getMethod(_setter, type);
		} catch (NoSuchMethodException | SecurityException e) {
			log.warn("Setter method '{}' not found on class {}", _setter, subject.getClass().getName());
		}
		setter = found;

		timer = new PauseTransition(Duration.millis(intervalMillis));
		timer.setOnFinished(e -> {
			if (cycleIndex()) {
				setValue(blinkValues[valueIndex]);
				timer.playFromStart();
			}
		});

		setValue(stillValue);
	}

	public void start() {
		reset();
		setValue(blinkValues[valueIndex]);
		timer.playFromStart();
	}

	public void startRepeat() {
		reset();
		setRepeat(0);
		setValue(blinkValues[valueIndex]);
		timer.playFromStart();
	}

	public void stop() {
		timer.stop();
		setValue(stillValue);
		currentRepeatCount = null;

		if (doOnStop != null) {
			// In JavaFX, run stop callback on FX thread.
			if (Platform.isFxApplicationThread()) {
				doOnStop.run();
			} else {
				Platform.runLater(doOnStop);
			}
		}
	}

	public void setInterval(int millisecs) {
		timer.setDuration(Duration.millis(millisecs));
	}

	public int getInterval() {
		return (int) Math.round(timer.getDuration().toMillis());
	}

	private boolean cycleIndex() {
		if (reverse) {
			valueIndex--;
		} else {
			valueIndex++;
		}

		if (valueIndex < 0 || valueIndex >= blinkValues.length) {
			if (repeat != null) {
				if (repeat == 0) {
					reset();
					return true; // infinite
				} else {
					if (currentRepeatCount == null) {
						currentRepeatCount = repeat;
					}
					currentRepeatCount--;
					if (currentRepeatCount > 0) {
						reset();
						return true;
					} else {
						stop();
						return false;
					}
				}
			} else {
				stop();
				return false;
			}
		}
		return true;
	}

	private void setValue(T value) {
		if (setter == null) {
			log.warn("Unable to set value - NULL setter.");
			return;
		}

//	    try {
//	        setter.invoke(subject, value);
//	    } catch (Exception e) {
//	        log.warn("Unable to set value.", e);
//	    }

		Runnable r = () -> {
			try {
				setter.invoke(subject, value);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.warn("Unable to set value.", e);
			}
		};

		// Ensure property changes happen on the JavaFX Application Thread.
		if (Platform.isFxApplicationThread()) {
			r.run();
		} else {
			Platform.runLater(r);
		}
	}

	public void reset() {
		valueIndex = (reverse ? blinkValues.length - 1 : 0);
	}

	public void setReverse(Boolean _reverse) {
		reverse = (_reverse != null && _reverse);
	}

	public Boolean getReverse() {
		return reverse;
	}

	public void setRepeat(Integer _repeat) {
		repeat = _repeat;
	}

	public Integer getRepeat() {
		return repeat;
	}

	public T getStillValue() {
		return stillValue;
	}

	public void setStillValue(T _stillValue) {
		stillValue = _stillValue;
	}

	public void startOnceForward(T finalValue) {
		stillValue = finalValue;
		timer.stop();
		setReverse(false);
		setRepeat(null);
		reset();
		start();
	}

	public void startOnceBackward(T finalValue) {
		stillValue = finalValue;
		timer.stop();
		setReverse(true);
		setRepeat(null);
		reset();
		start();
	}

	public void setDoOnStop(Runnable r) {
		this.doOnStop = r;
	}
}
