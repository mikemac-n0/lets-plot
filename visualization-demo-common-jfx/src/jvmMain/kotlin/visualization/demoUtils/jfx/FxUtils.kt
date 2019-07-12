package jetbrains.datalore.visualization.demoUtils.jfx

import javafx.application.Platform

fun runOnFxThread(runnable: () -> Unit) {
    if (Platform.isFxApplicationThread()) {
        runnable.invoke()
    } else {
        Platform.runLater(runnable)
    }
}

internal fun assertFxThread() {
    if (!Platform.isFxApplicationThread()) {
        throw IllegalStateException("Not JFX Application Thread ")
    }
}