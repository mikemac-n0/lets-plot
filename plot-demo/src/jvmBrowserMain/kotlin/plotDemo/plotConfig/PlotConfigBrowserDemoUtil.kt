package jetbrains.datalore.plotDemo.plotConfig

//import jetbrains.datalore.vis.demoUtils.browser.BrowserDemoUtil.BASE_MAPPER_LIBS
//import jetbrains.datalore.vis.demoUtils.browser.BrowserDemoUtil.DEMO_COMMON_LIBS
//import jetbrains.datalore.vis.demoUtils.browser.BrowserDemoUtil.KOTLIN_LIBS
//import jetbrains.datalore.vis.demoUtils.browser.BrowserDemoUtil.PLOT_LIBS
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.jsObject.mapToJsObjectInitializer
import jetbrains.datalore.plot.server.config.PlotConfigServerSide
import jetbrains.datalore.vis.demoUtils.browser.BrowserDemoUtil
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.StringWriter

private const val DEMO_PROJECT = "plot-demo"
private const val CALL_FUN = "jetbrains.datalore.plot.MonolithicJs.buildPlotFromProcessedSpecs"

private const val ROOT_ELEMENT_ID = "root"

private const val JS_DIST_PATH = "js-package/build/dist"
private const val PLOT_LIB = "datalore-plot.js"

object PlotConfigDemoUtil {
    fun show(title: String, plotSpecList: List<MutableMap<String, Any>>, plotSize: DoubleVector) {
        BrowserDemoUtil.openInBrowser(DEMO_PROJECT) {
            getHtml(
                title,
                plotSpecList,
                plotSize
            )
        }
    }

    private fun getPlotLibPath() = "${BrowserDemoUtil.getRootPath()}/$JS_DIST_PATH/$PLOT_LIB"

    private fun getHtml(
        title: String,
        plotSpecList: List<MutableMap<String, Any>>,
        plotSize: DoubleVector
    ): String {

        val plotSpecListJs = StringBuilder("[\n")
        @Suppress("UNCHECKED_CAST")
        var first = true
        for (spec in plotSpecList) {
            @Suppress("NAME_SHADOWING")
            val spec = PlotConfigServerSide.processTransform(spec)
            if (!first) plotSpecListJs.append(',') else first = false
            plotSpecListJs.append(mapToJsObjectInitializer(spec))
        }
        plotSpecListJs.append("\n]")

        val writer = StringWriter().appendHTML().html {
            lang = "en"
            head {
                title(title)
            }
            body {
                script {
                    type = "text/javascript"
                    src = getPlotLibPath()
                }

                div { id = ROOT_ELEMENT_ID }

                script {
                    type = "text/javascript"
                    unsafe {
                        +"""
                        |var plotSpecList=$plotSpecListJs;
                        |plotSpecList.forEach(function (spec, index) {
                        |
                        |   var parentElement = document.createElement('div');
                        |   document.getElementById("root").appendChild(parentElement);
                        |   DatalorePlot.$CALL_FUN(spec, ${plotSize.x}, ${plotSize.y}, parentElement);
                        |});
                    """.trimMargin()

                    }
                }
            }
        }

        return writer.toString()
    }
}