@file:Suppress("unused")

package tornadofx

import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventTarget
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import tornadofx.FX.Companion.inheritParamHolder
import tornadofx.FX.Companion.inheritScopeHolder
import tornadofx.FX.Companion.stylesheets
import tornadofx.osgi.impl.getBundleId
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass

open class Scope {
    fun deregister() {
        FX.primaryStages.remove(this)
        FX.applications.remove(this)
        FX.components.remove(this)
    }
}

var DefaultScope = Scope()

class FX {
    companion object {
        internal val inheritScopeHolder = ThreadLocal<Scope>()
        internal val inheritParamHolder = ThreadLocal<Map<String, Any>>()
        internal var ignoreParentForFirstBuilder: Boolean = false
            get() {
                if (field) {
                    field = false
                    return true
                } else {
                    return false
                }
            }

        val icon: Node get() = Pane().apply {
            style {
                shape = "M104.8,49.9c-0.3-2.5-0.7-4.8-1.2-7.1c-1-4.4-2.6-8.9-4.6-13C95.1,22,88.9,15.1,81.7,10c-3.5-2.5-7.1-4.4-11-5.9 c-4.3-1.6-8.7-2.8-13.1-3.4C55,0.2,52.4,0,49.9,0c-0.3,0-0.8,0-1.2,0c-1.8,3.5-3.5,6.9-4.6,10.5c-3.1,9.2-3.6,18.9-2.6,28.3 c0.5,4.6,1.3,9,2.5,13.6s2.5,9,3.9,13.5c2,6.6,3.8,13.1,4.1,19.9c0.3,4.9-0.8,9.9-2.6,14.8c-1.8-3.4-4.4-6.6-8-7.7 c3.3,2.1,4.8,5.8,5.4,9.2c-0.3,0-0.7,0-1,0c-2.6-0.3-5.1-0.7-7.7-1.3c-3.6-1-7.2-2.3-10.5-4.1c-6.9-3.8-12.6-9.2-17.1-15.6 C5.6,73.8,3,64.9,2.6,56.2c-0.3-9,2-17.9,6.4-25.8c4.3-7.4,10.5-13.6,17.9-17.9c2.8-1.6,5.8-3,8.9-3.9c0.7-0.2,1-0.3,1-0.3 S36.5,8.4,36,8.5c-7.6,2.1-14.5,6.2-20.2,11.5C9.4,26.1,4.4,34,2,42.5c-1.3,4.3-2,8.7-2,13.1c0,2.3,0,4.8,0.3,7.1 c0.2,2,0.7,3.8,1,5.8c0.5,2.3,1.2,4.4,2,6.6s1.8,4.3,3,6.4c2,3.3,4.3,6.6,6.9,9.5c3,3.3,6.4,6.2,10.2,8.7 c3.6,2.5,7.6,4.3,11.7,5.7c5.1,1.8,10.3,2.5,15.6,2.8c2.6,0.2,5.3,0,7.9-0.3c2.5-0.3,4.9-0.8,7.2-1.5c8.7-2.3,16.9-7.2,23.3-13.5 c6.6-6.6,11.3-14.6,13.8-23.5c0.8-2.8,1.3-5.6,1.6-8.5c0.2-1.8,0.3-3.6,0.3-5.4C105,53.7,105,51.8,104.8,49.9z M95.8,55.7 c0,2,0,3.9-0.3,5.9c-0.5,4.6-1.6,9-3.6,13.3c-3.8,8.5-10.2,15.8-18.2,20.7c-3.5,2.1-7.1,3.6-11,4.8c-3,0.8-5.9,1.3-9,1.6 c-0.2,0-0.3,0-0.3,0c-0.2,0-0.5,0-0.7,0c1.1-1.8,2.5-3.4,3.9-5.1c-1.5,0.8-3,1.8-4.4,2.8c2.1-4.3,3.8-9,3.9-14 c0.8-11.8-2.3-23-3.8-33.8c-2.3-14.1-0.7-28.1,5.3-39.3c0.3,0,0.7,0.2,1,0.2C67,14.1,74.9,18.2,81.3,24 c6.6,6.2,11.3,14.5,13.1,23.3C95.3,50.3,95.6,52.9,95.8,55.7L95.8,55.7L95.8,55.7z"
                backgroundColor += Color.BLACK
                minWidth = 16.px
                minHeight = 16.px
                maxWidth = 16.px
                maxHeight = 16.px
            }
        }

        val eventbus = EventBus()
        val log = Logger.getLogger("FX")
        val initialized = SimpleBooleanProperty(false)

        internal val primaryStages = HashMap<Scope, Stage>()
        val primaryStage: Stage get() = primaryStages[DefaultScope]!!
        fun getPrimaryStage(scope: Scope = DefaultScope) = primaryStages[scope] ?: primaryStages[DefaultScope]
        fun setPrimaryStage(scope: Scope = DefaultScope, stage: Stage) {
            primaryStages[scope] = stage
        }

        internal val applications = HashMap<Scope, Application>()
        val application: Application get() = applications[DefaultScope]!!
        fun getApplication(scope: Scope = DefaultScope) = applications[scope] ?: applications[DefaultScope]
        fun setApplication(scope: Scope = DefaultScope, application: Application) {
            applications[scope] = application
        }

        val stylesheets = FXCollections.observableArrayList<String>()

        internal val components = HashMap<Scope, HashMap<KClass<out Injectable>, Injectable>>()
        fun getComponents(scope: Scope = DefaultScope) = components.getOrPut(scope) { HashMap() }

        val lock = Any()

        @JvmStatic
        var dicontainer: DIContainer? = null
        var reloadStylesheetsOnFocus = false
        var reloadViewsOnFocus = false
        var dumpStylesheets = false
        var layoutDebuggerShortcut: KeyCodeCombination? = KeyCodeCombination(KeyCode.J, KeyCodeCombination.META_DOWN, KeyCodeCombination.ALT_DOWN)
        var osgiDebuggerShortcut: KeyCodeCombination? = KeyCodeCombination(KeyCode.O, KeyCodeCombination.META_DOWN, KeyCodeCombination.ALT_DOWN)

        val osgiAvailable: Boolean by lazy {
            try {
                Class.forName("org.osgi.framework.FrameworkUtil")
                true
            } catch (ex: Throwable) {
                false
            }
        }

        private val _locale: SimpleObjectProperty<Locale> = object : SimpleObjectProperty<Locale>() {
            override fun invalidated() = loadMessages()
        }
        var locale: Locale get() = _locale.get(); set(value) = _locale.set(value)
        fun localeProperty() = _locale

        private val _messages: SimpleObjectProperty<ResourceBundle> = SimpleObjectProperty()
        var messages: ResourceBundle get() = _messages.get(); set(value) = _messages.set(value)
        fun messagesProperty() = _messages

        /**
         * Load global resource bundle for the current locale. Triggered when the locale changes.
         */
        private fun loadMessages() {
            try {
                messages = ResourceBundle.getBundle("Messages", locale, FXResourceBundleControl.INSTANCE)
            } catch (ex: Exception) {
                log.fine({ "No global Messages found in locale $locale, using empty bundle" })
                messages = EmptyResourceBundle.INSTANCE
            }
        }

        fun installErrorHandler() {
            if (Thread.getDefaultUncaughtExceptionHandler() == null)
                Thread.setDefaultUncaughtExceptionHandler(DefaultErrorHandler())
        }

        init {
            locale = Locale.getDefault()
            inheritScopeHolder.set(DefaultScope)
        }

        fun runAndWait(action: () -> Unit) {
            // run synchronously on JavaFX thread
            if (Platform.isFxApplicationThread()) {
                action()
                return
            }

            // queue on JavaFX thread and wait for completion
            val doneLatch = CountDownLatch(1)
            Platform.runLater {
                try {
                    action()
                } finally {
                    doneLatch.countDown()
                }
            }

            try {
                doneLatch.await()
            } catch (e: InterruptedException) {
                // ignore exception
            }
        }

        @JvmStatic
        fun registerApplication(scope: Scope = DefaultScope, application: Application, primaryStage: Stage) {
            FX.installErrorHandler()
            setPrimaryStage(scope, primaryStage)
            setApplication(scope, application)

            // If custom scope is activated for application itself, change DefaultScope to be the supplised scope
            if (applications[DefaultScope] == null) {
                DefaultScope = scope
            }

            if (application.parameters?.unnamed != null) {
                with(application.parameters.unnamed) {
                    if (contains("--dev-mode")) {
                        reloadStylesheetsOnFocus = true
                        dumpStylesheets = true
                        reloadViewsOnFocus = true
                    }
                    if (contains("--live-stylesheets")) reloadStylesheetsOnFocus = true
                    if (contains("--dump-stylesheets")) dumpStylesheets = true
                    if (contains("--live-views")) reloadViewsOnFocus = true
                }
            }

            if (reloadStylesheetsOnFocus) primaryStage.reloadStylesheetsOnFocus()
            if (reloadViewsOnFocus) primaryStage.reloadViewsOnFocus()
        }

        @JvmStatic
        fun <T : Component> find(componentType: Class<T>, scope: Scope = DefaultScope): T = find(componentType.kotlin, scope)

        fun replaceComponent(obsolete: UIComponent, scope: Scope = DefaultScope) {
            val replacement: UIComponent

            if (obsolete is View) {
                getComponents(scope).remove(obsolete.javaClass.kotlin)
                replacement = find(obsolete.javaClass.kotlin, scope)
            } else {
                val noArgsConstructor = obsolete.javaClass.constructors.filter { it.parameterCount == 0 }.isNotEmpty()
                if (noArgsConstructor) {
                    replacement = obsolete.javaClass.newInstance()
                } else {
                    log.warning("Unable to reload $obsolete because it's missing a no args constructor")
                    return
                }
            }

            replacement.reloadInit = true

            if (obsolete.root.parent is Pane) {
                (obsolete.root.parent as Pane).children.apply {
                    val index = indexOf(obsolete.root)
                    remove(obsolete.root)
                    add(index, replacement.root)
                }
                log.info("Reloaded [Parent] $obsolete")
            } else {
                if (obsolete.properties.containsKey("tornadofx.scene")) {
                    val scene = obsolete.properties["tornadofx.scene"] as Scene
                    replacement.properties["tornadofx.scene"] = scene
                    scene.root = replacement.root
                    log.info("Reloaded [Scene] $obsolete")
                } else {
                    log.warning("Unable to reload $obsolete because it has no parent and no scene attached")
                }
            }
        }

        fun applyStylesheetsTo(scene: Scene) {
            scene.stylesheets.addAll(stylesheets)
            stylesheets.addListener(ListChangeListener {
                while (it.next()) {
                    if (it.wasAdded()) it.addedSubList.forEach { scene.stylesheets.add(it) }
                    if (it.wasRemoved()) it.removed.forEach { scene.stylesheets.remove(it) }
                }
            })
        }
    }
}

fun ignoreParentForFirstBuilder(op: () -> Unit) {
    FX.ignoreParentForFirstBuilder = true
    try {
        op()
    } finally {
        FX.ignoreParentForFirstBuilder = false
    }
}

fun addStageIcon(icon: Image, scope: Scope = DefaultScope) {
    val adder = { FX.getPrimaryStage(scope)?.icons?.add(icon) }
    if (FX.initialized.value) adder() else FX.initialized.addListener { obs, o, n -> adder() }
}

fun reloadStylesheetsOnFocus() {
    FX.reloadStylesheetsOnFocus = true
}

fun dumpStylesheets() {
    FX.dumpStylesheets = true
}

fun reloadViewsOnFocus() {
    FX.reloadViewsOnFocus = true
}

fun importStylesheet(stylesheet: String) {
    val css = FX::class.java.getResource(stylesheet)
    if (css != null)
        stylesheets.add(css.toExternalForm())
    else
        FX.log.log(Level.WARNING, "Unable to find stylesheet at $stylesheet - check that the path is correct")
}

fun <T : Stylesheet> importStylesheet(stylesheetType: KClass<T>) {
    val url = StringBuilder("css://${stylesheetType.java.name}")
    if (FX.osgiAvailable) {
        val bundleId = getBundleId(stylesheetType)
        if (bundleId != null) url.append("?$bundleId")
    }
    val urlString = url.toString()
    if (!FX.stylesheets.contains(urlString)) FX.stylesheets.add(url.toString())
}

fun <T : Stylesheet> removeStylesheet(stylesheetType: KClass<T>) {
    val url = StringBuilder("css://${stylesheetType.java.name}")
    if (FX.osgiAvailable) {
        val bundleId = getBundleId(stylesheetType)
        if (bundleId != null) url.append("?$bundleId")
    }
    FX.stylesheets.remove(url.toString())
}

inline fun <reified T : Component> find(scope: Scope = DefaultScope, vararg params: Pair<String, Any>): T = find(T::class, scope, *params)

inline fun <reified T : Injectable> setInScope(value: T, scope: Scope = DefaultScope) = FX.getComponents(scope).put(T::class, value)

@Suppress("UNCHECKED_CAST")
fun <T : Component> find(type: KClass<T>, scope: Scope = DefaultScope, vararg params: Pair<String, Any>): T {
    inheritScopeHolder.set(scope)
    inheritParamHolder.set(params.toMap())
    if (Injectable::class.java.isAssignableFrom(type.java)) {
        var components = FX.getComponents(scope)
        if (!components.containsKey(type as KClass<out Injectable>)) {
            synchronized(FX.lock) {
                if (!components.containsKey(type)) {
                    val cmp = type.java.newInstance()
                    if (cmp is UIComponent) cmp.init()
                    // if cmp.scope overrode the scope, inject into that instead
                    if (cmp is Component && cmp.scope != scope) {
                        components = FX.getComponents(scope)
                    }
                    components[type] = cmp
                }
            }
        }
        return components[type] as T
    }

    val cmp = type.java.newInstance()
    if (cmp is Fragment) cmp.init()
    return cmp
}

interface DIContainer {
    fun <T : Any> getInstance(type: KClass<T>): T
}

/**
 * Add the given node to the pane, invoke the node operation and return the node
 */
fun <T : Node> opcr(parent: EventTarget, node: T, op: (T.() -> Unit)? = null): T {
    parent.addChildIfPossible(node)
    op?.invoke(node)
    return node
}

@Suppress("UNNECESSARY_SAFE_CALL")
fun EventTarget.addChildIfPossible(node: Node) {
    if (FX.ignoreParentForFirstBuilder) return
    if (this is Node) {
        val target = builderTarget
        if (target != null) {
            // Trick to get around the disallowed use of invoke on out projected types
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            target!!(this).value = node
            return
        }
    }
    when (this) {
    // Only add if root is already created, else this will become the root
        is UIComponent -> root?.addChildIfPossible(node)
        is ScrollPane -> content = node
        is Tab -> content = node
        is TabPane -> {
            val uicmp = if (node is Parent) node.uiComponent<UIComponent>() else null
            val tab = Tab(uicmp?.title ?: node.toString(), node)
            tabs.add(tab)
        }
        is TitledPane -> {
            if (content is Parent) {
                content.addChildIfPossible(node)
            } else if (content is Node) {
                val container = VBox()
                container.children.addAll(content, node)
                content = container
            } else {
                content = node
            }
        }
        is DataGrid<*> -> {
        }
        else -> getChildList()?.apply { if (!contains(node)) add(node) }
    }
}

/**
 * Find the list of children from a Parent node. Gleaned code from ControlsFX for this.
 */
fun EventTarget.getChildList(): MutableList<Node>? = when (this) {
    is SplitPane -> items
    is ToolBar -> items
    is Pane -> children
    is Group -> children
    is Control -> if (skin is SkinBase<*>) (skin as SkinBase<*>).children else getChildrenReflectively()
    is Parent -> getChildrenReflectively()
    else -> null
}

@Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun Parent.getChildrenReflectively(): MutableList<Node>? {
    val getter = this.javaClass.findMethodByName("getChildren")
    if (getter != null && java.util.List::class.java.isAssignableFrom(getter.returnType)) {
        getter.isAccessible = true
        return getter.invoke(this) as MutableList<Node>
    }
    return null
}