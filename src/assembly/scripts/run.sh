#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"

java --module-path "$DIR/lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing \
     -cp "$DIR/app.jar" \
     cz.bliksoft.javautils.fx.test.FxTests
