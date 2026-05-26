package de.miraculixx.veinminerClient.config

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import de.miraculixx.veinminer.pattern.PatternConfig
import de.miraculixx.veinminer.pattern.PatternType
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.ShapeRouletteOverlay
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ROW_HEIGHT = 48

/**
 * Best case scenario, never need to touch that again.
 * Otherwise, everything is based on try-and-see numbers.
 * Color picker is a shader-like for performance.
 */
class PatternConfigScreen(private val parent: Screen?) : Screen(Component.literal("Veinminer Patterns")) {
    private var addType = PatternType.TUNNEL
    private lateinit var patternList: PatternList
    private var colorPicker: FloatingColorPicker? = null
    private var patternListScrollAmount = 0.0

    override fun init() {
        val center = width / 2
        addRenderableWidget(
            CycleButton.booleanBuilder(Component.literal("Inverted"), Component.literal("Normal"), ClientPatternConfig.settings.invertedScroll)
                .create(16, 24, 100, 20, Component.literal("Scroll")) { _, value ->
                    ClientPatternConfig.settings.invertedScroll = value
                    ClientPatternConfig.save()
                }
        )
        addRenderableWidget(
            Button.builder(Component.literal("+ Pattern")) {
                ClientPatternConfig.add(addType)
                syncSelection()
                rebuildPatternWidgets()
            }.bounds(width - 80, 24, 64, 20).build()
        )

        patternList = PatternList(minecraft, panelWidth(), viewportHeight(), panelTop(), ROW_HEIGHT)
        patternList.updateSizeAndPosition(panelWidth(), viewportHeight(), panelLeft(), panelTop())
        patternList.setScrollAmount(patternListScrollAmount)
        addRenderableWidget(patternList)

        addRenderableWidget(
            Button.builder(Component.literal("Done")) {
                saveAndClose()
            }.bounds(center - 102, height - 28, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Reset")) {
                ClientPatternConfig.reset()
                syncSelection()
                rebuildPatternWidgets()
            }.bounds(center + 2, height - 28, 100, 20).build()
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val blockUnderlyingHover = colorPicker?.contains(mouseX.toDouble(), mouseY.toDouble()) == true
        val delegatedMouseX = if (blockUnderlyingHover) -1 else mouseX
        val delegatedMouseY = if (blockUnderlyingHover) -1 else mouseY
        super.render(graphics, delegatedMouseX, delegatedMouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF)
        colorPicker?.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        saveAndClose()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseClicked(event, doubleClick)) return true
            if (!picker.contains(event.x(), event.y())) {
                closeColorPicker()
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseReleased(event)) return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        colorPicker?.let { picker ->
            if (picker.mouseDragged(event, dx, dy)) return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.isEscape && colorPicker != null) {
            closeColorPicker()
            return true
        }
        colorPicker?.let { picker ->
            if (picker.keyPressed(event)) return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        colorPicker?.let { picker ->
            if (picker.charTyped(event)) return true
        }
        return super.charTyped(event)
    }

    private fun saveAndClose() {
        ClientPatternConfig.save()
        syncSelection()
        NetworkManager.sendPatternConfig()
        minecraft.setScreen(parent)
    }

    private fun openColorPicker(pattern: PatternConfig) {
        colorPicker = FloatingColorPicker(width, height, pattern) {
            ClientPatternConfig.save()
            syncSelection()
        }
        clearFocus()
    }

    private fun closeColorPicker() {
        colorPicker?.close()
        colorPicker = null
        ClientPatternConfig.save()
        syncSelection()
    }

    private fun rebuildPatternWidgets() {
        if (::patternList.isInitialized) {
            patternListScrollAmount = patternList.scrollAmount()
        }
        rebuildWidgets()
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
    }

    private fun panelWidth(): Int = (width - 32).coerceAtMost(900).coerceAtLeast(360)
    private fun panelLeft(): Int = width / 2 - panelWidth() / 2
    private fun panelTop(): Int = 52
    private fun panelBottom(): Int = height - 38
    private fun viewportHeight(): Int = (panelBottom() - panelTop()).coerceAtLeast(ROW_HEIGHT)

    private inner class PatternList(
        minecraft: Minecraft,
        width: Int,
        height: Int,
        y: Int,
        itemHeight: Int,
    ) : ContainerObjectSelectionList<PatternEntry>(minecraft, width, height, y, itemHeight) {
        init {
            centerListVertically = false
            replaceEntries(ClientPatternConfig.settings.patterns.map(::PatternEntry))
        }

        override fun getRowWidth(): Int = (width - 18).coerceAtLeast(340)

        override fun scrollBarX(): Int = x + getWidth() - 10
    }

    private inner class PatternEntry(val pattern: PatternConfig) : ContainerObjectSelectionList.Entry<PatternEntry>() {
        private val iconButton = PatternIconButton(pattern) {
            if (!pattern.enabled || ClientPatternConfig.canDisable(pattern)) {
                pattern.enabled = !pattern.enabled
                ClientPatternConfig.save()
                syncSelection()
            } else {
                rebuildPatternWidgets()
            }
        }

        private val typeButton = CycleButton.builder({ Component.literal("● Type") }, pattern.type)
            .withValues(PatternType.entries)
            .displayOnlyValue()
            .create(0, 0, 88, 22, Component.literal("Type")) { _, value ->
                pattern.type = value
                pattern.color = ClientPatternConfig.defaultFor(value).color
                ClientPatternConfig.save()
                syncSelection()
                rebuildPatternWidgets()
            }

        private val colorButton = ColorButton(pattern) {
            openColorPicker(pattern)
        }

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

        private val stairsDirectionButton = CycleButton.booleanBuilder(Component.literal("UP"), Component.literal("DOWN"), pattern.stairsUp)
            .displayOnlyValue()
            .create(0, 0, 92, 22, Component.literal("Direction")) { _, value ->
                pattern.stairsUp = value
                ClientPatternConfig.save()
                syncSelection()
            }

        private val upButton = MoveSpriteButton(1) {
            if (ClientPatternConfig.move(pattern, -1)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val downButton = MoveSpriteButton(2) {
            if (ClientPatternConfig.move(pattern, 1)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val removeButton = MoveSpriteButton(3) {
            if (ClientPatternConfig.remove(pattern)) {
                syncSelection()
                rebuildPatternWidgets()
            }
        }

        private val widgets: List<AbstractWidget> = listOf(
            iconButton,
            typeButton,
            colorButton,
            widthSlider,
            heightSlider,
            stairsDirectionButton,
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
            graphics.fill(x - 4, y - 3, right + 4, y + ROW_HEIGHT - 5, if (hovered) 0x80909090.toInt() else 0x70909090)
            graphics.fill(x - 4, y - 3, x, y + ROW_HEIGHT - 5, 0xFF000000.toInt() or accent)
            widgets.forEach { it.render(graphics, mouseX, mouseY, partialTick) }
            graphics.drawString(font, ClientPatternConfig.displayName(pattern), x + ROW_HEIGHT, y + 4, -1)
        }

        override fun children(): List<GuiEventListener> = widgets

        override fun narratables(): List<NarratableEntry> = widgets

        override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean =
            super.mouseClicked(event, doubleClick)

        override fun mouseReleased(event: MouseButtonEvent): Boolean =
            super.mouseReleased(event)

        override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean =
            super.mouseDragged(event, dx, dy)

        override fun keyPressed(event: KeyEvent): Boolean =
            super.keyPressed(event)

        override fun charTyped(event: CharacterEvent): Boolean =
            super.charTyped(event)

        private fun layoutWidgets() {
            val x = contentX
            val y = contentY
            val right = contentRight
            val moveX = right - 24
            val controlsX = x + ROW_HEIGHT
            val sliderX = controlsX + 108
            val directionW = 68 //if (pattern.type == PatternType.STAIRS) 68 else 0
            val sliderRight = moveX - 18 - directionW
            val sliderW = sliderRight - sliderX
            val buttonHeight = 20

            iconButton.place(x + 4, y, ROW_HEIGHT - 8, ROW_HEIGHT - 8)
            typeButton.place(controlsX, y + buttonHeight, 64, buttonHeight)
            colorButton.place(controlsX + 68, y + buttonHeight, buttonHeight, buttonHeight)
            widthSlider.place(sliderX, y, sliderW, buttonHeight - 1)
            heightSlider.place(sliderX, y + buttonHeight + 1, sliderW, buttonHeight - 1)
            stairsDirectionButton.place(sliderX + sliderW + 4, y + buttonHeight, 64, buttonHeight)
            upButton.place(moveX, y - 4, 14, 14)
            removeButton.place(moveX, y + 12, 14, 14)
            downButton.place(moveX, y + 28, 14, 14)

            iconButton.active = !pattern.enabled || ClientPatternConfig.canDisable(pattern)
            removeButton.active = ClientPatternConfig.canRemove(pattern)
            upButton.active = ClientPatternConfig.settings.patterns.indexOf(pattern) > 0
            downButton.active = ClientPatternConfig.settings.patterns.indexOf(pattern) < ClientPatternConfig.settings.patterns.lastIndex

            widgets.forEach { it.visible = true }
            widthSlider.visible = pattern.hasSize()
            heightSlider.visible = pattern.hasSize()
            stairsDirectionButton.visible = pattern.type == PatternType.STAIRS
        }
    }
}

private class PatternIconButton(
    private val pattern: PatternConfig,
    private val onToggle: () -> Unit,
) : AbstractWidget(0, 0, ROW_HEIGHT, ROW_HEIGHT, Component.literal("Toggle pattern")) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val bg = if (isHoveredOrFocused) 0xB0505050.toInt() else 0xB0404040.toInt()
        val tint = if (pattern.enabled) pattern.color and 0xFFFFFF else 0x7A7A7A
        graphics.fill(x, y, x + width, y + height, bg)
        graphics.fill(x, y, x + width, y + 1, 0xFFB8B8B8.toInt())
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF222222.toInt())
        graphics.fill(x, y, x + 1, y + height, 0xFFB8B8B8.toInt())
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF222222.toInt())
        graphics.blit(RenderPipelines.GUI_TEXTURED, pattern.icon(), x + 2, y + 2, 0f, 0f, ROW_HEIGHT - 12, ROW_HEIGHT - 12, 16, 16, 16, 16, 0xFF000000.toInt() or tint)
        handleCursor(graphics)
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        onToggle()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}

private class ColorButton(
    private val pattern: PatternConfig,
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, 34, 22, Component.literal("Color")) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val border = if (isHoveredOrFocused) 0xFFFFFFFF.toInt() else 0xFF808080.toInt()
        graphics.fill(x, y, x + width, y + height, 0xFF303030.toInt())
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF000000.toInt() or (pattern.color and 0xFFFFFF))
        graphics.fill(x, y, x + width, y + 1, border)
        graphics.fill(x, y + height - 1, x + width, y + height, border)
        graphics.fill(x, y, x + 1, y + height, border)
        graphics.fill(x + width - 1, y, x + width, y + height, border)
        handleCursor(graphics)
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        onPress()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}

private class MoveSpriteButton(
    private val type: Int,
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, 14, 14, Component.literal("Navigation")) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val highlight = isHoveredOrFocused && active
        val sprite = when (type) {
            1 -> if (highlight) MOVE_UP_HIGHLIGHTED_SPRITE else MOVE_UP_SPRITE
            2 -> if (highlight) MOVE_DOWN_HIGHLIGHTED_SPRITE else MOVE_DOWN_SPRITE
            else -> if (highlight) X_HIGHLIGHTED_SPRITE else X_SPRITE
        }
        val color = if (active) 0xFFFFFFFF.toInt() else 0x80777777.toInt()
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, color)
        handleCursor(graphics)
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        onPress()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }

    private companion object {
        val MOVE_UP_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_up")
        val MOVE_UP_HIGHLIGHTED_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_up_highlighted")
        val MOVE_DOWN_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_down")
        val MOVE_DOWN_HIGHLIGHTED_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/move_down_highlighted")
        val X_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/delete")
        val X_HIGHLIGHTED_SPRITE: Identifier = Identifier.fromNamespaceAndPath(ClientLifecycle.MOD_ID, "menu/delete_highlighted")
    }
}

private open class IntSlider(
    private val suffix: String,
    private val min: Int,
    private val max: Int,
    getter: () -> Int,
    private val setter: (Int) -> Unit,
) : AbstractSliderButton(0, 0, 160, 22, Component.empty(), 0.0) {
    init {
        value = normalize(getter())
        updateMessage()
    }

    override fun updateMessage() {
        setMessage(Component.literal("${currentValue()} ├──────┤ $suffix"))
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

    fun extractRenderState(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xE0000000.toInt())
        graphics.fill(x, y, x + width, y + height, 0xFFE8E8E8.toInt())
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF1E1E1E.toInt())

        graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000.toInt() or (pattern.color and 0xFFFFFF))
        graphics.renderOutline(previewX, previewY, previewSize, previewSize, 0xFFB8B8B8.toInt())


        graphics.gradientQuad(
            pickerX,
            pickerY,
            pickerW,
            pickerH,
            0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt() or hsvToRgb(hue, 1f, 1f),
            0xFF000000.toInt() or hsvToRgb(hue, 1f, 1f),
        )
        graphics.gradientQuad(
            pickerX,
            pickerY,
            pickerW,
            pickerH,
            0x00000000,
            0xFF000000.toInt(),
            0xFF000000.toInt(),
            0x00000000,
        )
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

    fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0 || !contains(event.x(), event.y())) return false
        if (hexBox.isMouseOver(event.x(), event.y())) {
            hexBox.isFocused = true
            hexBox.mouseClicked(event, doubleClick)
            dragging = null
            return true
        }
        hexBox.isFocused = false
        dragging = when {
            inBounds(event.x(), event.y(), pickerX, pickerY, pickerW, pickerH) -> DragTarget.PICKER
            inBounds(event.x(), event.y(), hueX, hueY, hueW, hueH) -> DragTarget.HUE
            else -> null
        }
        updateFromMouse(event.x(), event.y())
        return true
    }

    fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (hexBox.isFocused && hexBox.mouseDragged(event, dx, dy)) return true
        if (event.button() != 0 || dragging == null) return false
        updateFromMouse(event.x(), event.y())
        return true
    }

    fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (hexBox.isFocused && hexBox.mouseReleased(event)) return true
        if (event.button() != 0 || dragging == null) return false
        dragging = null
        return true
    }

    fun keyPressed(event: KeyEvent): Boolean = hexBox.keyPressed(event)

    fun charTyped(event: CharacterEvent): Boolean = hexBox.charTyped(event)

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
            DragTarget.HUE -> {
                hue = ((mouseX - hueX) / (hueW - 1)).toFloat().coerceIn(0f, 1f)
            }
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

private fun GuiGraphics.gradientQuad(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    topLeft: Int,
    bottomLeft: Int,
    bottomRight: Int,
    topRight: Int,
) {
    addGuiElement(
        GradientQuadRenderState(
            RenderPipelines.GUI,
            TextureSetup.noTexture(),
            Matrix3x2f(pose()),
            x,
            y,
            x + width,
            y + height,
            topLeft,
            bottomLeft,
            bottomRight,
            topRight,
            null,
        )
    )
}

private fun GuiGraphics.hueGradient(x: Int, y: Int, width: Int, height: Int) {
    addGuiElement(
        HueStripRenderState(
            RenderPipelines.GUI,
            TextureSetup.noTexture(),
            Matrix3x2f(pose()),
            x,
            y,
            x + width,
            y + height,
            null,
        )
    )
}

private fun GuiGraphics.addGuiElement(element: GuiElementRenderState) {
    guiRenderState.submitGuiElement(element)
}

private class GradientQuadRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    private val topLeft: Int,
    private val bottomLeft: Int,
    private val bottomRight: Int,
    private val topRight: Int,
    private val scissorArea: ScreenRectangle?,
) : GuiElementRenderState {
    private val bounds = bounds(x0, y0, x1, y1, pose, scissorArea)

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x0.toFloat(), y0.toFloat()).setColor(topLeft)
        vertexConsumer.addVertexWith2DPose(pose, x0.toFloat(), y1.toFloat()).setColor(bottomLeft)
        vertexConsumer.addVertexWith2DPose(pose, x1.toFloat(), y1.toFloat()).setColor(bottomRight)
        vertexConsumer.addVertexWith2DPose(pose, x1.toFloat(), y0.toFloat()).setColor(topRight)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds
}

private class HueStripRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    private val scissorArea: ScreenRectangle?,
) : GuiElementRenderState {
    private val bounds = bounds(x0, y0, x1, y1, pose, scissorArea)

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        val segments = HUE_COLORS.lastIndex
        val totalWidth = (x1 - x0).coerceAtLeast(1)
        for (i in 0 until segments) {
            val left = x0 + totalWidth * i / segments
            val right = x0 + totalWidth * (i + 1) / segments
            val leftColor = HUE_COLORS[i]
            val rightColor = HUE_COLORS[i + 1]
            vertexConsumer.addVertexWith2DPose(pose, left.toFloat(), y0.toFloat()).setColor(leftColor)
            vertexConsumer.addVertexWith2DPose(pose, left.toFloat(), y1.toFloat()).setColor(leftColor)
            vertexConsumer.addVertexWith2DPose(pose, right.toFloat(), y1.toFloat()).setColor(rightColor)
            vertexConsumer.addVertexWith2DPose(pose, right.toFloat(), y0.toFloat()).setColor(rightColor)
        }
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle = bounds

    private companion object {
        val HUE_COLORS = intArrayOf(
            0xFFFF0000.toInt(),
            0xFFFFFF00.toInt(),
            0xFF00FF00.toInt(),
            0xFF00FFFF.toInt(),
            0xFF0000FF.toInt(),
            0xFFFF00FF.toInt(),
            0xFFFF0000.toInt(),
        )
    }
}

private fun bounds(x0: Int, y0: Int, x1: Int, y1: Int, pose: Matrix3x2fc, scissorArea: ScreenRectangle?): ScreenRectangle {
    val bounds = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
    return scissorArea?.intersection(bounds) ?: bounds
}

private fun PatternConfig.hasSize(): Boolean = type == PatternType.TUNNEL || type == PatternType.STAIRS

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
