package de.miraculixx.veinminerClient.config

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminer.pattern.BlockFilter
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.PatternType
import de.miraculixx.veinminer.utils.cBase
import de.miraculixx.veinminer.utils.cGold
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.cWhite
import de.miraculixx.veinminer.utils.cYellow
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.ShapeRouletteOverlay
import java.util.function.Consumer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.components.tabs.GridLayoutTab
import net.minecraft.client.gui.components.tabs.Tab
import net.minecraft.client.gui.components.tabs.TabManager
import net.minecraft.client.gui.components.tabs.TabNavigationBar
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

private const val ROW_HEIGHT = 48
private var colorPicker: FloatingColorPicker? = null

private enum class Panel { SHAPE, FILTER }
private const val FOOTER_HEIGHT = 38
private const val CHIP_HEIGHT = 13
private const val COMBINE_WIDTH = 84
private const val SUGGESTION_HEIGHT = 11
private const val VISIBLE_SUGGESTIONS = 6
private const val MAX_SUGGESTIONS = 50
private val EMPTY_HINT = Component.literal("No filter: Add blocks or #tags to limit this pattern")

/**
 * Best case scenario, never need to touch that again.
 * Otherwise, everything is based on try-and-see numbers.
 * Color picker is a shader-like for performance.
 */
class PatternConfigScreen(private val parent: Screen?) : Screen(Component.literal("Veinminer Patterns")) {
    private var addType = PatternType.TUNNEL
    private lateinit var patternList: PatternList
    private var patternListScrollAmount = 0.0
    private var currentTab = 0
    private lateinit var tabs: List<Tab>
    private lateinit var tabManager: TabManager
    private lateinit var tabBar: TabNavigationBar
    private var expanded: Pair<String, Panel>? = null
    private var revealExpanded = false
    private var autoClosedShape: String? = null
    private var nameEditor: PatternEntry? = null
    private val blockCandidates by lazy { BuiltInRegistries.BLOCK.keySet().map { it.toString() }.sorted() }
    private val itemCandidates by lazy { BuiltInRegistries.ITEM.keySet().map { it.toString() }.filter { it != "minecraft:air" }.sorted() }
    private var iconPicker: FloatingIconPicker? = null
    private val tagCandidates by lazy { BuiltInRegistries.BLOCK.tagNames.map { "#${it.location()}" }.toList().sorted() }

    override fun init() {
        nameEditor?.commitName()
        iconPicker?.close()
        if (::tabManager.isInitialized) currentTab = tabs.indexOf(tabManager.currentTab).coerceAtLeast(0)
        val center = width / 2
        patternList = PatternList(minecraft!!, panelWidth())
        tabs = listOf(PatternsTab(), SettingsTab())
        tabManager = TabManager({ addRenderableWidget(it) }, { removeWidget(it) })
        tabBar = TabNavigationBar.builder(tabManager, width).addTabs(*tabs.toTypedArray()).build()
        addRenderableWidget(tabBar)
        tabBar.setWidth(width)
        tabBar.arrangeElements()
        val tabTop = tabBar.rectangle.bottom()
        tabManager.setTabArea(ScreenRectangle(0, tabTop, width, height - FOOTER_HEIGHT - tabTop))
        tabBar.selectTab(currentTab, false)
        patternList.setScrollAmount(patternListScrollAmount)
        if (revealExpanded) {
            revealExpanded = false
            patternList.revealPanel()
        }

        addRenderableWidget(
            Button.builder(Component.literal("Done")) {
                saveAndClose()
            }.bounds(center - 127, height - 28, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("HELP").withColor(cGold)) {
                Util.getPlatform().openUri("https://modrinth.com/mod/veinminer-client")
            }.bounds(center - 25, height - 28, 50, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Reset")) {
                ClientPatternConfig.reset()
                syncSelection()
                rebuildPatternWidgets()
            }.bounds(center + 27, height - 28, 100, 20).build()
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val blockUnderlyingHover = colorPicker?.contains(mouseX.toDouble(), mouseY.toDouble()) == true ||
            iconPicker?.contains(mouseX.toDouble(), mouseY.toDouble()) == true
        val delegatedMouseX = if (blockUnderlyingHover) -1 else mouseX
        val delegatedMouseY = if (blockUnderlyingHover) -1 else mouseY
        if (patternList.dirty) patternList.refresh()
        super.render(graphics, delegatedMouseX, delegatedMouseY, partialTick)
        activeWhitelist()?.renderSuggestions(graphics, mouseX, mouseY)
        colorPicker?.let { graphics.overlay { it.render(graphics, mouseX, mouseY, partialTick) } }
        iconPicker?.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        saveAndClose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseClicked(mouseX, mouseY, button)) return true
            if (!picker.contains(mouseX, mouseY)) {
                closeColorPicker()
                return true
            }
        }
        iconPicker?.let { picker ->
            if (!picker.mouseClicked(mouseX, mouseY, button)) picker.close()
            return true
        }
        return activeWhitelist()?.suggestionClicked(mouseX, mouseY) == true || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        iconPicker?.let { if (it.contains(mouseX, mouseY)) return it.mouseScrolled(mouseX, mouseY, scrollY) || true }
        return activeWhitelist()?.suggestionScrolled(mouseX, mouseY, scrollY) == true || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseReleased(mouseX, mouseY, button)) return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == InputConstants.KEY_ESCAPE && colorPicker != null) {
            closeColorPicker()
            return true
        }
        colorPicker?.let { picker ->
            if (picker.keyPressed(keyCode, scanCode, modifiers)) return true
        }
        iconPicker?.let { return it.keyPressed(keyCode, scanCode, modifiers) }
        if (nameEditor?.handleNameKey(keyCode) == true) return true
        return activeWhitelist()?.handleKey(keyCode) == true || tabBar.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        colorPicker?.let { picker ->
            if (picker.charTyped(codePoint, modifiers)) return true
        }
        iconPicker?.let { return it.charTyped(codePoint, modifiers) }
        return super.charTyped(codePoint, modifiers)
    }

    private fun saveAndClose() {
        nameEditor?.commitName()
        iconPicker?.close()
        colorPicker = null
        ClientPatternConfig.save()
        syncSelection()
        NetworkManager.sendPatternConfig()
        minecraft?.setScreen(parent)
    }

    private fun openIconPicker(pattern: PatternConfig) {
        if (colorPicker != null) closeColorPicker()
        nameEditor?.commitName()
        iconPicker = FloatingIconPicker(pattern)
        clearFocus()
    }

    private fun openColorPicker(pattern: PatternConfig) {
        iconPicker?.close()
        colorPicker = FloatingColorPicker(width, height, pattern) {
            ClientPatternConfig.save()
            syncSelection()
        }
        rebuildPatternWidgets()
        clearFocus()
    }

    private fun closeColorPicker() {
        colorPicker?.close()
        colorPicker = null
        ClientPatternConfig.save()
        rebuildPatternWidgets()
        syncSelection()
    }

    private fun rebuildPatternWidgets() {
        if (::patternList.isInitialized) patternListScrollAmount = patternList.getScrollAmount()
        rebuildWidgets()
    }

    private fun activeWhitelist(): WhitelistEntry? =
        (patternList.panelEntry as? WhitelistEntry)?.takeIf { tabManager.currentTab is PatternsTab && it.input.isFocused && patternList.children().contains(it) }

    private fun expandedPanel(pattern: PatternConfig): Panel? = expanded?.takeIf { it.first == pattern.id }?.second

    private fun toggleExpanded(pattern: PatternConfig, panel: Panel) {
        expanded = if (expandedPanel(pattern) == panel) null else pattern.id to panel
        autoClosedShape = null
        patternList.dirty = true
    }

    /** Hides the shape panel for shapes without settings and restores it once the type has settings again */
    private fun syncShapePanel(pattern: PatternConfig) {
        if (!pattern.hasSize() && expandedPanel(pattern) == Panel.SHAPE) {
            expanded = null
            autoClosedShape = pattern.id
        } else if (pattern.hasSize() && autoClosedShape == pattern.id && expanded == null) {
            expanded = pattern.id to Panel.SHAPE
            autoClosedShape = null
        }
    }

    private fun findSuggestions(query: String, exclude: Collection<String>): List<String> =
        matchSuggestions(query, if (query.trim().startsWith("#")) tagCandidates else blockCandidates + tagCandidates, exclude)

    private fun selectPattern(pattern: PatternConfig) {
        NetworkManager.selectedPattern = pattern
        syncSelection()
    }

    private fun isSelected(pattern: PatternConfig): Boolean = try {
        NetworkManager.selectedPattern.id == pattern.id
    } catch (_: UninitializedPropertyAccessException) {
        false
    }

    private fun syncSelection() {
        val enabled = ClientPatternConfig.enabledPatterns()
        val selected = try {
            enabled.firstOrNull { it.id == NetworkManager.selectedPattern.id }
        } catch (_: UninitializedPropertyAccessException) {
            null
        } ?: enabled.first()
        NetworkManager.selectedPattern = selected
        ShapeRouletteOverlay.syncTo(selected, NetworkManager.selectedDepth)
        ClientPatternConfig.setSelection(selected, NetworkManager.selectedDepth)
    }

    private fun panelWidth(): Int = (width - 32).coerceAtMost(900).coerceAtLeast(360)
    private fun panelLeft(): Int = width / 2 - panelWidth() / 2

    private inner class PatternsTab : Tab {
        private val addButton = Button.builder(Component.literal("+ Pattern")) {
            val pattern = ClientPatternConfig.add(addType)
            expanded = pattern.id to Panel.SHAPE
            revealExpanded = true
            syncSelection()
            rebuildPatternWidgets()
        }.size(64, 20).build()

        override fun getTabTitle(): Component = Component.literal("Patterns")

        override fun visitChildren(consumer: Consumer<AbstractWidget>) {
            consumer.accept(addButton)
            consumer.accept(patternList)
        }

        override fun doLayout(area: ScreenRectangle) {
            addButton.setPosition(panelLeft() + panelWidth() - addButton.width - 4, area.top() + 4)
            val listTop = area.top() + 28
            patternList.updateSizeAndPosition(panelWidth(), (area.bottom() - listTop).coerceAtLeast(ROW_HEIGHT), listTop)
            patternList.x = panelLeft()
        }
    }

    private class SettingsTab : GridLayoutTab(Component.literal("Settings")) {
        init {
            val settings = ClientPatternConfig.settings
            layout.rowSpacing(4)
            layout.addChild(toggle("Scroll", "Inverted", "Normal", settings.invertedScroll) { settings.invertedScroll = it }, 0, 0)
            layout.addChild(toggle("Pattern Scroll", "Locked", "Free", settings.lockPatternScroll) { settings.lockPatternScroll = it }, 1, 0)
            layout.addChild(toggle("Shortcut Hint", "Shown", "Hidden", settings.showShortcutHint) { settings.showShortcutHint = it }, 2, 0)
        }

        private fun toggle(label: String, on: String, off: String, value: Boolean, setter: (Boolean) -> Unit): CycleButton<Boolean> =
            CycleButton.booleanBuilder(Component.literal(on), Component.literal(off))
                .withInitialValue(value)
                .create(0, 0, 210, 20, Component.literal(label)) { _, v ->
                    setter(v)
                    ClientPatternConfig.save()
                }
    }

    /** 1.21.1 lists only support a fixed row height, row positions are calculated from the own [heights] instead */
    private inner class PatternList(minecraft: Minecraft, width: Int) : ContainerObjectSelectionList<ListEntry>(minecraft, width, 0, 0, ROW_HEIGHT) {
        private val patternEntries = HashMap<String, PatternEntry>()
        private val heights = HashMap<ListEntry, Int>()
        var panelEntry: PanelEntry? = null
            private set
        var dirty = false

        init {
            centerListVertically = false
            refresh()
        }

        /** Re-adds all entries (variable heights), keeps entry instances to preserve widget state */
        fun refresh() {
            dirty = false
            val scroll = getScrollAmount()
            clearEntries()
            heights.clear()
            val (expandedId, panel) = expanded ?: (null to null)
            val pattern = ClientPatternConfig.settings.patterns.firstOrNull { it.id == expandedId }
            val current = panelEntry
            if (pattern == null || panel == null) panelEntry = null
            else if (current?.pattern !== pattern || current.panel != panel) {
                panelEntry = if (panel == Panel.SHAPE) ShapeEntry(pattern) else WhitelistEntry(pattern)
            }
            ClientPatternConfig.settings.patterns.forEach { p ->
                addEntry(patternEntries.getOrPut(p.id) { PatternEntry(p) }, ROW_HEIGHT)
                panelEntry?.takeIf { it.pattern === p }?.let { addEntry(it, it.computeHeight(rowWidth)) }
            }
            setScrollAmount(scroll)
        }

        private fun addEntry(entry: ListEntry, height: Int) {
            heights[entry] = height
            addEntry(entry)
        }

        private fun heightOf(index: Int): Int = heights[children()[index]] ?: itemHeight

        fun revealPanel() {
            val index = children().indexOf(panelEntry ?: return)
            val top = getRowTop(index) - y - 4
            val bottom = getRowBottom(index) - y
            if (bottom > height) setScrollAmount(getScrollAmount() + bottom - height)
            if (top < 0) setScrollAmount(getScrollAmount() + top)
        }

        fun entryAt(mouseX: Double, mouseY: Double): ListEntry? {
            if (!isMouseOver(mouseX, mouseY) || mouseX < rowLeft - 4 || mouseX > rowRight + 4) return null
            return children().indices.firstOrNull { mouseY >= getRowTop(it) && mouseY < getRowBottom(it) }?.let { children()[it] }
        }

        override fun getRowTop(index: Int): Int = y + 4 - getScrollAmount().toInt() + headerHeight + (0 until index).sumOf(::heightOf)

        override fun getRowBottom(index: Int): Int = getRowTop(index) + heightOf(index)

        override fun getMaxPosition(): Int = children().indices.sumOf(::heightOf) + headerHeight

        override fun renderListItems(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val hovered = entryAt(mouseX.toDouble(), mouseY.toDouble())
            children().forEachIndexed { index, entry ->
                val top = getRowTop(index)
                val height = heightOf(index)
                if (top + height >= y && top <= bottom) {
                    entry.render(graphics, index, top, rowLeft, rowWidth, height, mouseX, mouseY, entry === hovered, partialTick)
                }
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            val entry = entryAt(mouseX, mouseY)
            if (entry != null && isValidMouseClick(button) && entry.mouseClicked(mouseX, mouseY, button)) {
                val current = getFocused()
                if (current !== entry && current is ContainerEventHandler) current.setFocused(null)
                setFocused(entry)
                isDragging = true
                return true
            }
            return super.mouseClicked(mouseX, mouseY, button)
        }

        override fun getRowWidth(): Int = (width - 18).coerceAtLeast(340)

        override fun getScrollbarPosition(): Int = x + width - 10
    }

    private abstract inner class ListEntry : ContainerObjectSelectionList.Entry<ListEntry>() {
        protected var contentX = 0
        protected var contentY = 0
        protected var contentWidth = 0
        protected var contentHeight = 0
        protected val contentRight get() = contentX + contentWidth

        override fun render(graphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            contentX = left
            contentY = top
            contentWidth = width
            contentHeight = height
            renderContent(graphics, mouseX, mouseY, hovered, partialTick)
        }

        abstract fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float)

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean = patternList.entryAt(mouseX, mouseY) === this
    }

    private inner class PatternEntry(val pattern: PatternConfig) : ListEntry() {
        private val iconButton = PatternIconButton(
            pattern,
            onSelect = { if (pattern.enabled) selectPattern(pattern) },
            onIcon = { openIconPicker(pattern) },
            onToggle = {
                if (!pattern.enabled || ClientPatternConfig.canDisable(pattern)) {
                    pattern.enabled = !pattern.enabled
                    ClientPatternConfig.save()
                    syncSelection()
                } else {
                    rebuildPatternWidgets()
                }
            }
        )

        private val typeButton = CycleButton.builder<PatternType> { getText("● ${it.name.lowercase().replaceFirstChar(Char::uppercase)}") }
            .withValues(PatternType.entries)
            .withInitialValue(pattern.type)
            .displayOnlyValue()
            .create(0, 0, 74, 20, Component.literal("● Type")) { _, value ->
                pattern.type = value
                pattern.color = ClientPatternConfig.defaultFor(value).color
                ClientPatternConfig.save()
                syncSelection()
                syncShapePanel(pattern)
                rebuildPatternWidgets()
            }

        private val colorButton = ColorButton(pattern) {
            openColorPicker(pattern)
        }

        private val shapeButton = Button.builder(Component.empty()) { toggleExpanded(pattern, Panel.SHAPE) }.build()

        private val filterButton = Button.builder(Component.empty()) { toggleExpanded(pattern, Panel.FILTER) }.build()

        private val upButton = MoveSpriteButton(MoveSpriteButton.Type.UP) {
            if (ClientPatternConfig.move(pattern, -1)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val downButton = MoveSpriteButton(MoveSpriteButton.Type.DOWN) {
            if (ClientPatternConfig.move(pattern, 1)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val removeButton = MoveSpriteButton(MoveSpriteButton.Type.REMOVE) {
            if (ClientPatternConfig.remove(pattern)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val nameBox = EditBox(font, 0, 0, 160, 14, Component.literal("Pattern name")).apply {
            setMaxLength(ClientPatternConfig.MAX_NAME_LENGTH)
            visible = false
        }
        private var focusNameBox = false

        private val widgets: List<AbstractWidget> = listOf(
            nameBox,
            iconButton,
            typeButton,
            colorButton,
            shapeButton,
            filterButton,
            upButton,
            removeButton,
            downButton,
        )

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            layoutWidgets()
            val x = contentX
            val y = contentY
            val right = contentRight
            val accent = pattern.color and 0xFFFFFF
            val selected = isSelected(pattern)
            val expanded = expandedPanel(pattern) != null
            val bottom = if (expanded) y + ROW_HEIGHT - 2 else y + ROW_HEIGHT - 5
            graphics.fill(x - 4, y - 3, right + 4, bottom, if (hovered || selected || expanded) 0x80505050.toInt() else 0x70383838)
            graphics.fill(x - 4, y - 3, x, bottom, 0xFF000000.toInt() or accent)
            if (selected) {
                val border = 0xFF000000.toInt() or accent
                graphics.fill(x - 4, y - 3, right + 4, y - 2, border)
                if (!expanded) graphics.fill(x - 4, bottom - 1, right + 4, bottom, border)
                graphics.fill(right + 3, y - 3, right + 4, bottom, border)
            }
            if (focusNameBox) {
                // Claimed after click handling, otherwise the list click resets the focus
                focusNameBox = false
                this@PatternConfigScreen.focused = patternList
                patternList.setFocused(this)
                focused = nameBox
            } else if (nameBox.visible && (!nameBox.isFocused || patternList.getFocused() !== this || this@PatternConfigScreen.focused !== patternList)) commitName()
            widgets.forEach { if (it.visible) it.render(graphics, mouseX, mouseY, partialTick) }
            if (!nameBox.visible && colorPicker == null) {
                val name = ClientPatternConfig.displayName(pattern)
                val nameX = x + ROW_HEIGHT
                graphics.drawString(font, name, nameX, y + 4, -1)
                if (isOverName(mouseX.toDouble(), mouseY.toDouble())) {
                    graphics.fill(nameX, y + 13, nameX + font.width(name), y + 14, 0xFFFFFFFF.toInt())
                    setTooltipForNextRenderPass(Component.literal("Click to rename"))
                }
            }
        }

        override fun children(): List<GuiEventListener> = widgets

        override fun narratables(): List<NarratableEntry> = widgets

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button == InputConstants.MOUSE_BUTTON_LEFT && isOverName(mouseX, mouseY)) {
                startEditing()
                return true
            }
            return super.mouseClicked(mouseX, mouseY, button)
        }

        private fun isOverName(mouseX: Double, mouseY: Double): Boolean {
            if (nameBox.visible || !patternList.isMouseOver(mouseX, mouseY)) return false
            val nameX = contentX + ROW_HEIGHT
            return inBounds(mouseX, mouseY, nameX, contentY + 2, font.width(ClientPatternConfig.displayName(pattern)), 12)
        }

        private fun startEditing() {
            nameEditor?.takeIf { it !== this }?.commitName()
            nameEditor = this
            nameBox.value = ClientPatternConfig.displayName(pattern)
            nameBox.moveCursorToEnd(false)
            nameBox.visible = true
            focusNameBox = true
        }

        fun commitName(apply: Boolean = true) {
            if (!nameBox.visible) return
            nameBox.visible = false
            nameBox.isFocused = false
            if (nameEditor === this) nameEditor = null
            if (apply) ClientPatternConfig.rename(pattern, nameBox.value)
        }

        /** Enter commits, escape cancels */
        fun handleNameKey(keyCode: Int): Boolean {
            if (!nameBox.visible) return false
            when {
                keyCode.isConfirmation() -> commitName()
                keyCode == InputConstants.KEY_ESCAPE -> commitName(apply = false)
                else -> return false
            }
            return true
        }

        private fun layoutWidgets() {
            val x = contentX
            val y = contentY
            val right = contentRight
            val moveX = right - 22
            val controlsX = x + ROW_HEIGHT
            val buttonHeight = 20
            val panel = expandedPanel(pattern)
            val filterSize = pattern.whitelist.size

            iconButton.place(x + 4, y, ROW_HEIGHT - 8, ROW_HEIGHT - 8)
            typeButton.place(controlsX, y + buttonHeight, 64, buttonHeight)
            colorButton.place(controlsX + 68, y + buttonHeight, buttonHeight, buttonHeight)
            shapeButton.place(controlsX + 94, y + buttonHeight, 70, buttonHeight)
            filterButton.place(controlsX + 168, y + buttonHeight, 80, buttonHeight)
            shapeButton.message = getText("Shape ${if (panel == Panel.SHAPE) "▲" else "▼"}")
            shapeButton.active = pattern.hasSize()
            val filterColor = when (WhitelistStatus.worst(pattern.whitelist)?.severity) {
                2 -> cRed
                1 -> cYellow
                else -> cWhite
            }
            filterButton.message = if (colorPicker != null) Component.empty() else Component.literal("Filter")
                .append(if (filterSize == 0) Component.empty() else Component.literal(" ($filterSize)").withColor(filterColor))
                .append(" ${if (panel == Panel.FILTER) "▲" else "▼"}")
            nameBox.place(controlsX - 4, y + 1, (moveX - 8 - (controlsX - 4)).coerceAtMost(170), 14)
            upButton.place(moveX, y - 4, 14, 14)
            removeButton.place(moveX, y + 12, 14, 14)
            downButton.place(moveX, y + 28, 14, 14)

            removeButton.active = ClientPatternConfig.canRemove(pattern)
            upButton.active = ClientPatternConfig.settings.patterns.indexOf(pattern) > 0
            downButton.active = ClientPatternConfig.settings.patterns.indexOf(pattern) < ClientPatternConfig.settings.patterns.lastIndex
        }
    }

    private abstract inner class PanelEntry(val pattern: PatternConfig, val panel: Panel) : ListEntry() {
        abstract fun computeHeight(rowWidth: Int): Int

        protected fun innerWidth(rowWidth: Int) = rowWidth - 12

        /** Background attached to the pattern entry above */
        protected fun renderPanelBackground(graphics: GuiGraphics) {
            val bottom = contentY + contentHeight - 1
            val accent = 0xFF000000.toInt() or (pattern.color and 0xFFFFFF)
            graphics.fill(contentX - 4, contentY - 2, contentRight + 4, bottom, 0x60505050)
            graphics.fill(contentX - 4, contentY - 2, contentX, bottom, accent)
            if (isSelected(pattern)) {
                graphics.fill(contentX - 4, bottom - 1, contentRight + 4, bottom, accent)
                graphics.fill(contentRight + 3, contentY - 2, contentRight + 4, bottom, accent)
            }
        }
    }

    private inner class ShapeEntry(pattern: PatternConfig) : PanelEntry(pattern, Panel.SHAPE) {
        private val widthSlider = IntSlider("X", 1, 10, { pattern.width }) {
            pattern.width = it
            ClientPatternConfig.save()
            syncSelection()
        }

        private val heightSlider = IntSlider("Y", 1, 10, { pattern.height }) {
            pattern.height = it
            ClientPatternConfig.save()
            syncSelection()
        }

        private val directionButton = CycleButton.booleanBuilder(getText("UP"), getText("DOWN"))
            .withInitialValue(pattern.stairsUp)
            .create(0, 0, 100, 20, Component.literal("Direction")) { _, value ->
                pattern.stairsUp = value
                ClientPatternConfig.save()
                syncSelection()
            }

        private val widgets: List<AbstractWidget> = when {
            !pattern.hasSize() -> emptyList()
            pattern.type == PatternType.STAIRS -> listOf(widthSlider, heightSlider, directionButton)
            else -> listOf(widthSlider, heightSlider)
        }

        override fun computeHeight(rowWidth: Int): Int = 30

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            renderPanelBackground(graphics)
            val left = contentX + 6
            if (widgets.isEmpty()) {
                val name = pattern.type.name.lowercase().replaceFirstChar { it.uppercase() }
                graphics.drawString(font, "$name has no shape settings", left, contentY + 8, 0xFFA0A0A0.toInt())
                return
            }
            val directionW = if (directionButton in widgets) directionButton.width + 4 else 0
            val sliderW = (innerWidth(contentWidth) - directionW - 4) / 2
            widthSlider.place(left, contentY + 2, sliderW, 20)
            heightSlider.place(left + sliderW + 4, contentY + 2, sliderW, 20)
            directionButton.setPosition(left + sliderW * 2 + 8, contentY + 2)
            widgets.forEach { it.render(graphics, mouseX, mouseY, partialTick) }
        }

        override fun children(): List<GuiEventListener> = widgets

        override fun narratables(): List<NarratableEntry> = widgets
    }

    private inner class WhitelistEntry(pattern: PatternConfig) : PanelEntry(pattern, Panel.FILTER) {
        val input: EditBox = EditBox(font, 0, 0, 100, 16, Component.literal("Block or tag")).apply {
            setMaxLength(BlockFilter.MAX_ENTRY_LENGTH)
            setHint(Component.literal("Add block or #tag...").withColor(0xFF707070.toInt()))
            setResponder { dropdown.update(findSuggestions(it, pattern.whitelist)) }
        }
        private val addButton = Button.builder(Component.literal("Add")) { submit(input.value) }.size(40, 16).build()
        private val combineButton = CycleButton.booleanBuilder(Component.literal("ON"), Component.literal("OFF"))
            .withInitialValue(pattern.combine)
            .withTooltip { Tooltip.create(combineTooltip()) }
            .create(0, 0, COMBINE_WIDTH, 16, Component.literal("Combine")) { _, value ->
                pattern.combine = value
                ClientPatternConfig.save()
            }
        private var chips: List<WhitelistChip> = emptyList()
        private var message: Component? = null
        private val dropdown: SuggestionDropdown = SuggestionDropdown(
            input,
            screenHeight = { this@PatternConfigScreen.height },
            anchorVisible = { input.y >= patternList.y && input.bottom <= patternList.bottom },
            colorOf = { suggestion, selected ->
                when (status(suggestion).state) {
                    WhitelistStatus.State.PARTIAL -> 0xFFFFFF55.toInt()
                    WhitelistStatus.State.BLOCKED -> 0xFFFF5555.toInt()
                    else -> if (selected) 0xFFFFFF55.toInt() else 0xFFE0E0E0.toInt()
                }
            },
            onAccept = ::submit,
        )

        init {
            rebuildChips()
        }

        private fun status(entry: String) = WhitelistStatus.cached(entry)

        private fun rebuildChips() {
            chips = pattern.whitelist.map { entry -> WhitelistChip(entry, status(entry)) { remove(entry) } }
        }

        private fun textLines(rowWidth: Int) = (message ?: if (chips.isEmpty()) EMPTY_HINT else null)
            ?.let { font.split(it, innerWidth(rowWidth)) } ?: emptyList()

        /** Lays out chips starting at [left]/[top], returns the used height */
        private fun layoutChips(left: Int, top: Int, width: Int): Int {
            var cx = left
            var cy = top
            chips.forEach { chip ->
                if (cx != left && cx + chip.width > left + width) {
                    cx = left
                    cy += CHIP_HEIGHT + 3
                }
                chip.setPosition(cx, cy)
                cx += chip.width + 3
            }
            return if (chips.isEmpty()) 0 else cy - top + CHIP_HEIGHT + 3
        }

        override fun computeHeight(rowWidth: Int): Int {
            val textHeight = textLines(rowWidth).size * (font.lineHeight + 1)
            return 22 + textHeight + layoutChips(0, 0, innerWidth(rowWidth)) + 6
        }

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            renderPanelBackground(graphics)
            val y = contentY
            val left = contentX + 6

            input.setRectangle(innerWidth(contentWidth) - 48 - COMBINE_WIDTH, 16, left, y + 2)
            addButton.setPosition(left + input.width + 4, y + 2)
            combineButton.setPosition(addButton.right + 4, y + 2)
            combineButton.active = !NetworkManager.isVeinminerActive || NetworkManager.settings.client.allowClientGroups
            input.render(graphics, mouseX, mouseY, partialTick)
            addButton.render(graphics, mouseX, mouseY, partialTick)
            combineButton.render(graphics, mouseX, mouseY, partialTick)

            var lineY = y + 23
            textLines(contentWidth).forEach { line ->
                graphics.drawString(font, line, left, lineY, 0xFFA0A0A0.toInt())
                lineY += font.lineHeight + 1
            }
            layoutChips(left, lineY, innerWidth(contentWidth))
            chips.forEach { it.render(graphics, mouseX, mouseY, partialTick) }
        }

        override fun children(): List<GuiEventListener> = listOf(input, addButton, combineButton) + chips

        override fun narratables(): List<NarratableEntry> = listOf(input, addButton, combineButton) + chips

        private fun combineTooltip(): Component =
            if (NetworkManager.isVeinminerActive && !NetworkManager.settings.client.allowClientGroups) {
                Component.literal("Disabled by the server").withColor(cRed)
            } else {
                Component.literal("Mine all listed blocks together as one group.\nOnly blocks the server allows and your tool can harvest are combined.")
            }

        private fun submit(raw: String) {
            if (raw.isBlank()) return
            val entry = BlockFilter.normalize(raw)
            val entryStatus = entry?.let(::status)
            message = when {
                entry == null || entryStatus?.state == WhitelistStatus.State.INVALID ->
                    Component.literal("Unknown block or tag: ${raw.trim()}").withColor(cRed)
                entry in pattern.whitelist -> Component.literal("$entry is already in the list")
                pattern.whitelist.size >= BlockFilter.MAX_ENTRIES -> Component.literal("Limit of ${BlockFilter.MAX_ENTRIES} entries reached").withColor(cRed)
                else -> {
                    pattern.whitelist.add(entry)
                    ClientPatternConfig.save()
                    rebuildChips()
                    input.value = ""
                    entryStatus?.message(entry)
                }
            }
            patternList.dirty = true
        }

        private fun remove(entry: String) {
            pattern.whitelist.remove(entry)
            ClientPatternConfig.save()
            rebuildChips()
            message = null
            patternList.dirty = true
        }

        fun handleKey(keyCode: Int): Boolean {
            if (keyCode.isConfirmation()) {
                submit(dropdown.selected ?: input.value)
                return true
            }
            return dropdown.handleKey(keyCode)
        }

        fun suggestionClicked(mouseX: Double, mouseY: Double): Boolean = dropdown.clicked(mouseX, mouseY)

        fun suggestionScrolled(mouseX: Double, mouseY: Double, scrollY: Double): Boolean = dropdown.scrolled(mouseX, mouseY, scrollY)

        fun renderSuggestions(graphics: GuiGraphics, mouseX: Int, mouseY: Int) = dropdown.render(graphics, mouseX, mouseY)
    }

    private inner class FloatingIconPicker(private val pattern: PatternConfig) {
        private val w = 240.coerceAtMost(this@PatternConfigScreen.width - 24)
        private val h = 62
        private val x = ((this@PatternConfigScreen.width - w) / 2).coerceAtLeast(8)
        private val y = 34
        private val input: EditBox = EditBox(font, x + 54, y + 22, w - 62, 16, Component.literal("Item id")).apply {
            setMaxLength(128)
            setHint(Component.literal("Item id, empty = default").withColor(0xFF707070.toInt()))
            value = ClientPatternConfig.settings.icons[pattern.id]?.let(::shortId) ?: ""
            moveCursorToEnd(false)
            setResponder { dropdown.update(matchSuggestions(it, itemCandidates)) }
            isFocused = true
        }
        private val dropdown: SuggestionDropdown = SuggestionDropdown(
            input,
            screenHeight = { this@PatternConfigScreen.height },
            anchorVisible = { true },
            colorOf = { _, selected -> if (selected) 0xFFFFFF55.toInt() else 0xFFE0E0E0.toInt() },
            onAccept = { close(it) },
        )

        fun contains(mouseX: Double, mouseY: Double): Boolean =
            inBounds(mouseX, mouseY, x, y, w, h) || dropdown.contains(mouseX, mouseY)

        fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) = graphics.overlay {
            graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xE0000000.toInt())
            graphics.fill(x, y, x + w, y + h, 0xFFE8E8E8.toInt())
            graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1E1E1E.toInt())
            graphics.fill(x + 6, y + 6, x + 48, y + 48, 0xB0404040.toInt())
            graphics.renderOutline(x + 6, y + 6, 42, 42, 0xFFB8B8B8.toInt())

            val stack = ClientPatternConfig.parseItem(input.value)?.let(::ItemStack)
            if (stack != null) graphics.scaledItem(stack, x + 11, y + 11, 32)
            else graphics.tintedIcon(pattern.icon(), x + 11, y + 11, 32, pattern.color and 0xFFFFFF)

            graphics.drawString(font, "Pattern icon", x + 54, y + 8, 0xFFFFFFFF.toInt())
            input.render(graphics, mouseX, mouseY, partialTick)
            val status = when {
                stack != null -> Component.literal("✔ ").append(stack.hoverName).withColor(cGreen)
                input.value.isBlank() -> Component.literal("Default icon").withColor(cBase)
                else -> Component.literal("Invalid item, default icon is used").withColor(cRed)
            }
            graphics.drawString(font, status, x + 54, y + 44, -1)
            dropdown.render(graphics, mouseX, mouseY)
        }

        fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (dropdown.clicked(mouseX, mouseY)) return true
            if (input.isMouseOver(mouseX, mouseY)) input.mouseClicked(mouseX, mouseY, button)
            return contains(mouseX, mouseY)
        }

        fun mouseScrolled(mouseX: Double, mouseY: Double, scrollY: Double): Boolean = dropdown.scrolled(mouseX, mouseY, scrollY)

        fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            when {
                keyCode.isConfirmation() -> close(dropdown.selected ?: input.value)
                dropdown.handleKey(keyCode) -> {}
                keyCode == InputConstants.KEY_ESCAPE -> iconPicker = null
                else -> input.keyPressed(keyCode, scanCode, modifiers)
            }
            return true
        }

        fun charTyped(codePoint: Char, modifiers: Int): Boolean = input.charTyped(codePoint, modifiers)

        /** Saves [raw] as icon (invalid/blank resets to default) and closes the picker */
        fun close(raw: String = input.value) {
            ClientPatternConfig.setIcon(pattern, raw)
            iconPicker = null
        }
    }
}

/** Suggestion popup anchored below (or above) an [EditBox] */
private class SuggestionDropdown(
    private val input: EditBox,
    private val screenHeight: () -> Int,
    private val anchorVisible: () -> Boolean,
    private val colorOf: (String, Boolean) -> Int,
    private val onAccept: (String) -> Unit,
) {
    private val font = Minecraft.getInstance().font
    private var suggestions: List<String> = emptyList()
    private var index = 0
    private var scroll = 0
    private var hidden = false

    val selected: String? get() = if (visible()) suggestions.getOrNull(index) else null

    fun update(list: List<String>) {
        suggestions = list
        index = 0
        scroll = 0
        hidden = false
        updateGhostText()
    }

    fun visible(): Boolean = !hidden && suggestions.isNotEmpty() && anchorVisible()

    private fun updateGhostText() {
        val value = input.value
        val candidate = suggestions.getOrNull(index)?.takeIf { visible() && value.isNotEmpty() && it.startsWith(value) }
        input.setSuggestion(candidate?.substring(value.length))
    }

    private fun move(delta: Int) {
        index = (index + delta).mod(suggestions.size)
        if (index < scroll) scroll = index
        if (index >= scroll + VISIBLE_SUGGESTIONS) scroll = index - VISIBLE_SUGGESTIONS + 1
        updateGhostText()
    }

    /** Tab completes, up/down navigates, escape hides. Confirmation is handled by the owner */
    fun handleKey(keyCode: Int): Boolean {
        if (!visible()) return false
        when (keyCode) {
            InputConstants.KEY_TAB -> {
                input.value = suggestions[index]
                input.moveCursorToEnd(false)
                hidden = true
            }
            InputConstants.KEY_UP -> move(-1)
            InputConstants.KEY_DOWN -> move(1)
            InputConstants.KEY_ESCAPE -> hidden = true
            else -> return false
        }
        updateGhostText()
        return true
    }

    private fun bounds(): ScreenRectangle {
        val rows = suggestions.size.coerceAtMost(VISIBLE_SUGGESTIONS)
        val w = max(input.width, suggestions.maxOf { font.width(shortId(it)) } + 10)
        val h = rows * SUGGESTION_HEIGHT + 2
        val below = input.bottom + 1
        return ScreenRectangle(input.x, if (below + h > screenHeight()) input.y - h - 1 else below, w, h)
    }

    fun contains(mouseX: Double, mouseY: Double): Boolean {
        if (!visible()) return false
        val b = bounds()
        return inBounds(mouseX, mouseY, b.left(), b.top(), b.width(), b.height())
    }

    private fun suggestionAt(mouseX: Double, mouseY: Double): Int? {
        if (!contains(mouseX, mouseY)) return null
        return (scroll + ((mouseY - bounds().top() - 1) / SUGGESTION_HEIGHT).toInt()).coerceAtMost(suggestions.lastIndex)
    }

    fun clicked(mouseX: Double, mouseY: Double): Boolean {
        val i = suggestionAt(mouseX, mouseY) ?: return false
        onAccept(suggestions[i])
        return true
    }

    fun scrolled(mouseX: Double, mouseY: Double, scrollY: Double): Boolean {
        suggestionAt(mouseX, mouseY) ?: return false
        scroll = (scroll - scrollY.toInt()).coerceIn(0, (suggestions.size - VISIBLE_SUGGESTIONS).coerceAtLeast(0))
        return true
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!visible()) return
        val b = bounds()
        val hoveredIndex = suggestionAt(mouseX.toDouble(), mouseY.toDouble())
        graphics.overlay {
            graphics.fill(b.left(), b.top(), b.right(), b.bottom(), 0xF0101010.toInt())
            graphics.renderOutline(b.left(), b.top(), b.width(), b.height(), 0xFF505050.toInt())
            suggestions.drop(scroll).take(VISIBLE_SUGGESTIONS).forEachIndexed { i, suggestion ->
                val rowIndex = scroll + i
                val rowY = b.top() + 1 + i * SUGGESTION_HEIGHT
                if (rowIndex == index || rowIndex == hoveredIndex) graphics.fill(b.left() + 1, rowY, b.right() - 1, rowY + SUGGESTION_HEIGHT, 0xFF303030.toInt())
                graphics.drawString(font, shortId(suggestion), b.left() + 4, rowY + 2, colorOf(suggestion, rowIndex == index))
            }
            if (suggestions.size > VISIBLE_SUGGESTIONS) {
                val trackH = b.height() - 2
                val barH = (trackH * VISIBLE_SUGGESTIONS / suggestions.size).coerceAtLeast(4)
                val barY = b.top() + 1 + (trackH - barH) * scroll / (suggestions.size - VISIBLE_SUGGESTIONS)
                graphics.fill(b.right() - 3, barY, b.right() - 1, barY + barH, 0xFF808080.toInt())
            }
        }
    }
}

private fun matchSuggestions(query: String, pool: List<String>, exclude: Collection<String> = emptyList()): List<String> {
    val needle = query.trim().lowercase().removePrefix("#")
    if (needle.isEmpty()) return emptyList()
    return pool.asSequence()
        .filter { it !in exclude }
        .mapNotNull { candidate ->
            val id = candidate.removePrefix("#")
            when {
                id.startsWith(needle) || id.substringAfter(':').startsWith(needle) -> 0 to candidate
                id.contains(needle) -> 1 to candidate
                else -> null
            }
        }
        .sortedWith(compareBy({ it.first }, { it.second.length }))
        .take(MAX_SUGGESTIONS)
        .map { it.second }
        .toList()
}

/** No GUI strata in 1.21.1, flush everything below and render [block] above it (like tooltips) */
private inline fun GuiGraphics.overlay(z: Float = 400f, block: () -> Unit) {
    flush()
    pose().pushPose()
    pose().translate(0f, 0f, z)
    block()
    flush()
    pose().popPose()
}

/** Renders an item scaled to [size] pixels */
private fun GuiGraphics.scaledItem(stack: ItemStack, x: Int, y: Int, size: Int) {
    pose().pushPose()
    pose().translate(x.toFloat(), y.toFloat(), 0f)
    pose().scale(size / 16f, size / 16f, 1f)
    renderItem(stack, 0, 0)
    pose().popPose()
}

private fun GuiGraphics.tintedIcon(texture: ResourceLocation, x: Int, y: Int, size: Int, rgb: Int) {
    setColor(((rgb shr 16) and 0xFF) / 255f, ((rgb shr 8) and 0xFF) / 255f, (rgb and 0xFF) / 255f, 1f)
    blit(texture, x, y, size, size, 0f, 0f, 16, 16, 16, 16)
    setColor(1f, 1f, 1f, 1f)
}

private fun Int.isConfirmation(): Boolean = this == InputConstants.KEY_RETURN || this == InputConstants.KEY_NUMPADENTER

private class WhitelistChip(
    entry: String,
    private val status: WhitelistStatus,
    private val onRemove: () -> Unit,
) : AbstractWidget(0, 0, 0, CHIP_HEIGHT, Component.literal(entry)) {
    private val label = shortId(entry)

    init {
        width = Minecraft.getInstance().font.width(label) + 16
        val hint = Component.literal("Click to remove").withColor(cBase)
        setTooltip(Tooltip.create(status.message(entry)?.append("\n")?.append(hint) ?: Component.literal(entry).append("\n").append(hint)))
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val (bg, border) = when (status.state) {
            WhitelistStatus.State.PARTIAL -> 0xFF4A3F14.toInt() to 0xFFC8A830.toInt()
            WhitelistStatus.State.BLOCKED, WhitelistStatus.State.INVALID -> 0xFF4A1818.toInt() to 0xFFC83C3C.toInt()
            else -> 0xFF303030.toInt() to 0xFF707070.toInt()
        }
        graphics.fill(x, y, x + width, y + height, bg)
        graphics.renderOutline(x, y, width, height, if (isHoveredOrFocused) 0xFFFFFFFF.toInt() else border)
        val font = Minecraft.getInstance().font
        graphics.drawString(font, label, x + 4, y + 3, 0xFFFFFFFF.toInt(), false)
        graphics.drawString(font, "×", x + width - 9, y + 3, if (isHoveredOrFocused) 0xFFFF5555.toInt() else 0xFFA0A0A0.toInt(), false)
    }

    override fun onClick(mouseX: Double, mouseY: Double) = onRemove()

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}

private fun shortId(entry: String): String = entry.replaceFirst("minecraft:", "")

private class PatternIconButton(
    private val pattern: PatternConfig,
    private val onSelect: () -> Unit,
    private val onIcon: () -> Unit,
    private val onToggle: () -> Unit,
) : AbstractWidget(0, 0, ROW_HEIGHT, ROW_HEIGHT, Component.literal("Select pattern")) {
    init {
        setTooltip(Tooltip.create(Component.literal("Left click - select\nRight click - enable/disable\nMiddle click - change icon")))
    }

    override fun isValidClickButton(button: Int): Boolean =
        button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT || button == InputConstants.MOUSE_BUTTON_MIDDLE

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val bg = if (isHoveredOrFocused) 0xB0505050.toInt() else 0xB0404040.toInt()
        val tint = if (pattern.enabled) pattern.color and 0xFFFFFF else 0x7A7A7A
        graphics.fill(x, y, x + width, y + height, bg)
        graphics.fill(x, y, x + width, y + 1, 0xFFB8B8B8.toInt())
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF222222.toInt())
        graphics.fill(x, y, x + 1, y + height, 0xFFB8B8B8.toInt())
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF222222.toInt())
        val customIcon = ClientPatternConfig.iconStack(pattern)
        if (customIcon == null) {
            graphics.tintedIcon(pattern.icon(), x + 2, y + 2, width - 4, tint)
        } else {
            graphics.scaledItem(customIcon, x + 2, y + 2, width - 4)
            // Items can not be tinted, dim them with an overlay on top instead
            if (!pattern.enabled) graphics.overlay(200f) { graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xA0303030.toInt()) }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!active || !visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)) return false
        playDownSound(Minecraft.getInstance().soundManager)
        when (button) {
            InputConstants.MOUSE_BUTTON_RIGHT -> onToggle()
            InputConstants.MOUSE_BUTTON_MIDDLE -> onIcon()
            else -> onSelect()
        }
        return true
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}

private class ColorButton(
    private val pattern: PatternConfig,
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, 34, 20, Component.literal("Color")) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val border = if (isHoveredOrFocused) 0xFFFFFFFF.toInt() else 0xFF808080.toInt()
        graphics.fill(x, y, x + width, y + height, 0xFF303030.toInt())
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF000000.toInt() or (pattern.color and 0xFFFFFF))
        graphics.fill(x, y, x + width, y + 1, border)
        graphics.fill(x, y + height - 1, x + width, y + height, border)
        graphics.fill(x, y, x + 1, y + height, border)
        graphics.fill(x + width - 1, y, x + width, y + height, border)
    }

    override fun onClick(mouseX: Double, mouseY: Double) = onPress()

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}

private class MoveSpriteButton(
    private val type: Type,
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, 14, 14, Component.literal("Navigation")) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val highlighted = isHoveredOrFocused && active
        val sprite = when (type) {
            Type.UP -> if (highlighted) MOVE_UP_HIGHLIGHTED_SPRITE else MOVE_UP_SPRITE
            Type.DOWN -> if (highlighted) MOVE_DOWN_HIGHLIGHTED_SPRITE else MOVE_DOWN_SPRITE
            Type.REMOVE -> if (highlighted) DELETE_HIGHLIGHTED_SPRITE else DELETE_SPRITE
        }
        val color = if (active) 0xFFFFFFFF.toInt() else 0x80777777.toInt()
        graphics.blitSprite(sprite, x, y, 16, 16, color)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        if (active) onPress()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)

    enum class Type {
        UP,
        DOWN,
        REMOVE,
    }

    private companion object {
        val MOVE_UP_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_up")
        val MOVE_UP_HIGHLIGHTED_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_up_highlighted")
        val MOVE_DOWN_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_down")
        val MOVE_DOWN_HIGHLIGHTED_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_down_highlighted")
        val DELETE_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/delete")
        val DELETE_HIGHLIGHTED_SPRITE: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/delete_highlighted")
    }
}

private class IntSlider(
    private val suffix: String,
    private val min: Int,
    private val max: Int,
    getter: () -> Int,
    private val setter: (Int) -> Unit,
) : AbstractSliderButton(0, 0, 160, 20, Component.empty(), 0.0) {
    init {
        value = normalize(getter())
        updateMessage()
    }

    override fun updateMessage() {
        message = getText("${currentValue()} ├──────┤ $suffix")
    }

    override fun applyValue() {
        setter(currentValue())
    }

    private fun normalize(raw: Int): Double = (raw.coerceIn(min, max) - min).toDouble() / (max - min).toDouble()

    private fun currentValue(): Int = (min + value * (max - min)).roundToInt().coerceIn(min, max)
}

private class FloatingColorPicker(
    screenWidth: Int,
    screenHeight: Int,
    private val pattern: PatternConfig,
    private val onChanged: () -> Unit,
) {
    private val width = 310.coerceAtMost(screenWidth - 24)
    private val height = 112
    private val x = ((screenWidth - width) / 2).coerceAtLeast(8)
    private val y = 34.coerceAtMost((screenHeight - height - 8).coerceAtLeast(8))
    private val previewX = x + 8
    private val previewY = y + 10
    private val previewSize = 68
    private val pickerX = x + 84
    private val pickerY = y + 10
    private val pickerW = (width - 92).coerceAtLeast(120)
    private val pickerH = 68
    private val hueX = pickerX
    private val hueY = pickerY + pickerH + 8
    private val hueW = pickerW
    private val hueH = 16
    private val hexBox = EditBox(Minecraft.getInstance().font, previewX, previewY + previewSize + 8, previewSize, 16, Component.literal("Hex color"))
    private var hue: Float
    private var saturation: Float
    private var value: Float
    private var lastValidColor = pattern.color and 0xFFFFFF
    private var suppressHexResponder = false
    private var dragging: DragTarget? = null

    init {
        val hsv = pattern.color.rgbToHsv()
        hue = hsv.h
        saturation = hsv.s
        value = hsv.v
        hexBox.setMaxLength(7)
        hexBox.value = "#${lastValidColor.toHex()}"
        hexBox.setTextColor(0xFFFFFFFF.toInt())
        hexBox.setTextColorUneditable(0xFFAAAAAA.toInt())
        hexBox.setResponder { raw ->
            if (suppressHexResponder) return@setResponder
            raw.parseHexColor()?.let(::setColorFromHex)
        }
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xE0000000.toInt())
        graphics.fill(x, y, x + width, y + height, 0xFFE8E8E8.toInt())
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF1E1E1E.toInt())

        graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000.toInt() or (pattern.color and 0xFFFFFF))
        graphics.renderOutline(previewX, previewY, previewSize, previewSize, 0xFFB8B8B8.toInt())

        graphics.saturationValueGradient(pickerX, pickerY, pickerW, pickerH, hue)
        graphics.renderOutline(pickerX, pickerY, pickerW, pickerH, 0xFFB8B8B8.toInt())
        graphics.hueGradient(hueX, hueY, hueW, hueH)
        graphics.renderOutline(hueX, hueY, hueW, hueH, 0xFFB8B8B8.toInt())

        val selectorX = pickerX + (saturation * (pickerW - 1)).roundToInt()
        val selectorY = pickerY + ((1f - value) * (pickerH - 1)).roundToInt()
        graphics.fill(selectorX - 3, selectorY - 3, selectorX + 4, selectorY + 4, 0xFF000000.toInt())
        graphics.fill(selectorX - 2, selectorY - 2, selectorX + 3, selectorY + 3, 0xFFFFFFFF.toInt())

        val hueSelectorX = hueX + (hue * (hueW - 1)).roundToInt()
        graphics.fill(hueSelectorX - 3, hueY - 3, hueSelectorX + 4, hueY + hueH + 3, 0xFF000000.toInt())
        graphics.fill(hueSelectorX - 2, hueY - 2, hueSelectorX + 3, hueY + hueH + 2, 0xFFFFFFFF.toInt())

        hexBox.render(graphics, mouseX, mouseY, partialTick)
    }

    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !contains(mouseX, mouseY)) return false
        if (hexBox.isMouseOver(mouseX, mouseY)) {
            hexBox.isFocused = true
            hexBox.mouseClicked(mouseX, mouseY, button)
            dragging = null
            return true
        }
        hexBox.isFocused = false
        dragging = when {
            inBounds(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH) -> DragTarget.PICKER
            inBounds(mouseX, mouseY, hueX, hueY, hueW, hueH) -> DragTarget.HUE
            else -> null
        }
        updateFromMouse(mouseX, mouseY)
        return true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (hexBox.isFocused && hexBox.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
        if (button != 0 || dragging == null) return false
        updateFromMouse(mouseX, mouseY)
        return true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (hexBox.isFocused && hexBox.mouseReleased(mouseX, mouseY, button)) return true
        if (button != 0 || dragging == null) return false
        dragging = null
        return true
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = hexBox.keyPressed(keyCode, scanCode, modifiers)

    fun charTyped(codePoint: Char, modifiers: Int): Boolean = hexBox.charTyped(codePoint, modifiers)

    fun close() {
        if (hexBox.value.parseHexColor() == null) {
            applyColor(lastValidColor, updateHex = true)
        }
    }

    private fun updateFromMouse(mouseX: Double, mouseY: Double) {
        when (dragging) {
            DragTarget.PICKER -> {
                saturation = ((mouseX - pickerX) / (pickerW - 1)).toFloat().coerceIn(0f, 1f)
                value = (1f - ((mouseY - pickerY) / (pickerH - 1)).toFloat()).coerceIn(0f, 1f)
            }
            DragTarget.HUE -> hue = ((mouseX - hueX) / (hueW - 1)).toFloat().coerceIn(0f, 1f)
            null -> return
        }
        applyColor(hsvToRgb(hue, saturation, value), updateHex = true)
    }

    private fun setColorFromHex(color: Int) {
        val hsv = color.rgbToHsv()
        hue = hsv.h
        saturation = hsv.s
        value = hsv.v
        applyColor(color, updateHex = false)
    }

    private fun applyColor(color: Int, updateHex: Boolean) {
        val clean = color and 0xFFFFFF
        pattern.color = clean
        lastValidColor = clean
        if (updateHex) {
            suppressHexResponder = true
            hexBox.value = "#${clean.toHex()}"
            suppressHexResponder = false
        }
        onChanged()
    }

    private enum class DragTarget {
        PICKER,
        HUE,
    }
}

private fun AbstractWidget.place(x: Int, y: Int, width: Int, height: Int) {
    setRectangle(width, height, x, y)
}

private fun PatternConfig.hasSize(): Boolean = type == PatternType.TUNNEL || type == PatternType.STAIRS

private fun GuiGraphics.saturationValueGradient(x: Int, y: Int, width: Int, height: Int, hue: Float) {
    for (i in 0 until width) {
        val saturation = i.toFloat() / (width - 1).coerceAtLeast(1)
        fillGradient(x + i, y, x + i + 1, y + height, 0xFF000000.toInt() or hsvToRgb(hue, saturation, 1f), 0xFF000000.toInt())
    }
}

private fun GuiGraphics.hueGradient(x: Int, y: Int, width: Int, height: Int) {
    for (i in 0 until width) {
        val hue = i.toFloat() / (width - 1).coerceAtLeast(1)
        fill(x + i, y, x + i + 1, y + height, 0xFF000000.toInt() or hsvToRgb(hue, 1f, 1f))
    }
}

private fun inBounds(mouseX: Double, mouseY: Double, x: Int, y: Int, width: Int, height: Int): Boolean =
    mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height

private data class Hsv(val h: Float, val s: Float, val v: Float)

private fun Int.rgbToHsv(): Hsv {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).mod(6f) / 6f
        max == g -> (((b - r) / delta) + 2f) / 6f
        else -> (((r - g) / delta) + 4f) / 6f
    }
    val saturation = if (max == 0f) 0f else delta / max
    return Hsv(hue.coerceIn(0f, 1f), saturation.coerceIn(0f, 1f), max.coerceIn(0f, 1f))
}

private fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
    val h = (hue.coerceIn(0f, 1f) * 6f).let { if (it >= 6f) 0f else it }
    val c = value.coerceIn(0f, 1f) * saturation.coerceIn(0f, 1f)
    val x = c * (1f - ((h % 2f) - 1f).let { if (it < 0f) -it else it })
    val m = value.coerceIn(0f, 1f) - c
    val sector = floor(h).toInt()
    val (rp, gp, bp) = when (sector) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((rp + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((gp + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((bp + m) * 255f).roundToInt().coerceIn(0, 255)
    return (r shl 16) or (g shl 8) or b
}

private fun Int.toHex(): String = (this and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

private fun String.parseHexColor(): Int? {
    val clean = trim().removePrefix("#")
    if (clean.length != 6) return null
    return clean.toIntOrNull(16)?.and(0xFFFFFF)
}

private fun getText(text: String): Component
    = if (colorPicker == null) Component.literal(text) else Component.empty()
