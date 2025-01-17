package com.willr27.blocklings.client.gui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.willr27.blocklings.client.gui.Control;
import com.willr27.blocklings.client.gui.IControl;
import com.willr27.blocklings.client.gui.IScreen;
import com.willr27.blocklings.client.gui.controls.TabbedControl;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.util.event.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A container screen that includes the blockling gui tabs.
 */
@OnlyIn(Dist.CLIENT)
public abstract class TabbedContainerScreen<T extends Container> extends ContainerScreen<T> implements IControl, IScreen
{
    /**
     * The blockling.
     */
    @Nonnull
    protected final BlocklingEntity blockling;

    /**
     * The player opening the gui.
     */
    @Nonnull
    protected final PlayerEntity player;

    /**
     * The x position in the center of the screen.
     */
    protected int centerX;

    /**
     * The y position in the center of the screen.
     */
    protected int centerY;

    /**
     * The x position at the left of the gui's tabs.
     */
    protected int left;

    /**
     * The y position at the top of the gui.
     */
    protected int top;

    /**
     * The x position at the left of the gui excluding the tabs.
     */
    protected int contentLeft;

    /**
     * The y position at the top of the gui excluding.
     */
    protected int contentTop;

    /**
     * The x position at the right of the gui excluding the tabs.
     */
    protected int contentRight;

    /**
     * The y position at the bottom of the gui excluding.
     */
    protected int contentBottom;

    /**
     * The gui used to the draw and handle the tabs.
     */
    private TabbedControl tabbedControl;

    /**
     * The list of child controls.
     */
    @Nonnull
    private final ArrayList<Control> children = new ArrayList<>();

    /**
     * The currently held keys.
     */
    @Nonnull
    private final Map<Integer, Integer> heldKeys = new HashMap<>();

    /**
     *  The most recent control that was pressed without being released.
     */
    @Nullable
    private IControl recentlyPressedControl = null;

    /**
     *  The currently focused control.
     */
    @Nonnull
    private IControl focusedControl = this;

    /**
     *  The currently hovered control.
     */
    @Nonnull
    private IControl hoveredControl = this;

    /**
     *  The currently pressed control.
     */
    @Nullable
    private IControl pressedControl = this;

    /**
     * The mouse x position the pressed control was pressed at.
     */
    private int pressedMouseX = 0;

    /**
     * The mouse y position the pressed control was pressed at.
     */
    private int pressedMouseY = 0;

    /**
     *  The currently dragged control.
     */
    @Nullable
    private IControl draggedControl = null;

    /**
     * The event handler for hover events.
     */
    private final EventHandler<MouseEvent> onControlHover = new EventHandler<>();

    /**
     * The event handler for hover start events.
     */
    private final EventHandler<MouseEvent> onControlHoverStart = new EventHandler<>();

    /**
     * The event handler for hover stop events.
     */
    private final EventHandler<MouseEvent> onControlHoverStop = new EventHandler<>();

    /**
     * The event handler for mouse click events.
     */
    private final EventHandler<MouseButtonEvent> onControlMouseClicked = new EventHandler<>();

    /**
     * The event handler for mouse release events.
     */
    private final EventHandler<MouseButtonEvent> onControlMouseReleased = new EventHandler<>();

    /**
     * The event handler for mouse scroll events.
     */
    private final EventHandler<MouseScrollEvent> onControlMouseScrolled = new EventHandler<>();

    /**
     * The event handler for key pressed events.
     */
    private final EventHandler<KeyEvent> onControlKeyPressed = new EventHandler<>();

    /**
     * The event handler for key released events.
     */
    private final EventHandler<KeyEvent> onControlKeyReleased = new EventHandler<>();

    /**
     * The event handler for key held events.
     */
    private final EventHandler<KeyEvent> onControlKeyHeld = new EventHandler<>();

    /**
     * The event handler for char typed events.
     */
    private final EventHandler<CharEvent> onControlCharTyped = new EventHandler<>();

    /**
     * @param blockling the blockling.
     */
    public TabbedContainerScreen(@Nonnull T screenContainer, @Nonnull BlocklingEntity blockling)
    {
        super(screenContainer, Minecraft.getInstance().player.inventory, new StringTextComponent(""));
        this.blockling = blockling;
        this.player = Minecraft.getInstance().player;

        setupEventHandlers();
    }

    /**
     * Called on first creation and whenever the screen is resized.
     */
    @Override
    protected void init()
    {
        removeChildren();

        centerX = width / 2;
        centerY = height / 2 + TabbedControl.OFFSET_Y;

        left = centerX - TabbedControl.GUI_WIDTH / 2;
        top = centerY - TabbedControl.GUI_HEIGHT / 2;

        contentLeft = centerX - TabbedControl.CONTENT_WIDTH / 2;
        contentTop = top;
        contentRight = contentLeft + TabbedControl.CONTENT_WIDTH;
        contentBottom = contentTop + TabbedControl.CONTENT_HEIGHT;

        removeChild(tabbedControl);
        tabbedControl = new TabbedControl(this, blockling, left, top);

        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(true);

        super.init();
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        matrixStack.pushPose();
        matrixStack.translate(0.0, 0.0, -100.0);

        renderScreen(matrixStack, mouseX, mouseY, partialTicks);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        matrixStack.popPose();

        if (getPressedControl() != null)
        {
            int difX = Math.abs(mouseX - getPressedMouseX());
            int difY = Math.abs(mouseY - getPressedMouseY());

            if (difX >= 4 || difY >= 4)
            {
                setDraggedControl(getPressedControl());
            }
        }

        forwardControlHover(new MouseEvent(mouseX, mouseY));
        preRenderAll(mouseX, mouseY, partialTicks);
        renderAll(matrixStack, mouseX, mouseY, partialTicks);

        RenderSystem.enableDepthTest();
        getHoveredControl().renderTooltip(matrixStack, mouseX, mouseY);
        RenderSystem.enableDepthTest();
        renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY)
    {
        // Leave empty to stop container labels being rendered
    }

    @Override
    protected void renderBg(@Nonnull MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {

    }

    @Override
    public void renderTooltip(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY)
    {
        super.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void renderScreen(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        renderTitle(matrixStack, mouseX, mouseY, partialTicks);
    }

    /**
     * Renders the screen's title.
     */
    protected void renderTitle(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        drawCenteredString(matrixStack, font, getTitle(), contentLeft + TabbedControl.CONTENT_WIDTH / 2, contentTop - 15, 0xffffff);
    }

    @Override
    public ITextComponent getTitle()
    {
        return tabbedControl.getChildren().stream().map(control -> ((TabbedControl.TabControl) control)).filter(tabControl -> tabControl.isSelected()).findFirst().get().tab.name;
    }

    @Nonnull
    @Override
    public IScreen getScreen()
    {
        return this;
    }

    @Override
    @Nullable
    public IControl getParent()
    {
        return null;
    }

    @Nonnull
    @Override
    public ArrayList<Control> getChildren()
    {
        return children;
    }

    @Override
    public void addChild(@Nonnull Control control)
    {
        if (!children.contains(control))
        {
            children.add(control);
            control.parent = this;
        }
    }

    @Override
    public void removeChild(@Nullable Control control)
    {
        if (control != null)
        {
            control.parent = null;
            children.remove(control);
        }
    }

    @Override
    public void tick()
    {
        tickAll();
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        MouseButtonEvent e = new MouseButtonEvent((int) mouseX, (int) mouseY, button);

        forwardControlMouseClicked(e);
        forwardGlobalMouseClicked(e);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        MouseButtonEvent e = new MouseButtonEvent((int) mouseX, (int) mouseY, button);

        forwardControlMouseReleased(e);
        forwardGlobalMouseReleased(e);

        setRecentlyClickedControl(null);
        setPressedControl(null, (int) mouseX, (int) mouseY);
        setDraggedControl(null);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scroll)
    {
        MouseScrollEvent e = new MouseScrollEvent((int) mouseX, (int) mouseY, scroll);

        forwardControlMouseScrolled(e);
        forwardGlobalMouseScrolled(e);

        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int mods)
    {
        if (isKeyHeld(keyCode) && heldKeys.get(keyCode) > 10)
        {
            KeyEvent e = new KeyEvent(keyCode, scanCode, mods);

            getFocusedControl().controlKeyHeld(e);
            forwardGlobalKeyHeld(e);
        }
        else
        {
            KeyEvent e = new KeyEvent(keyCode, scanCode, mods);

            getFocusedControl().controlKeyPressed(e);
            forwardGlobalKeyPressed(e);
        }

        Integer oldCount = heldKeys.put(keyCode, 0);

        if (oldCount != null)
        {
            heldKeys.put(keyCode, oldCount + 1);
        }

        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int mods)
    {
        KeyEvent e = new KeyEvent(keyCode, scanCode, mods);

        getFocusedControl().controlKeyReleased(e);
        forwardGlobalKeyReleased(e);

        heldKeys.remove(keyCode);

        return super.keyReleased(keyCode, scanCode, mods);
    }

    @Override
    public final boolean charTyped(char character, int keyCode)
    {
        CharEvent e = new CharEvent(character, keyCode);

        getFocusedControl().controlCharTyped(e);
        forwardGlobalCharTyped(e);

        return super.charTyped(character, keyCode);
    }

    @Override
    public int getScreenX()
    {
        return 0;
    }

    @Override
    public int getScreenY()
    {
        return 0;
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public float getScale()
    {
        return 1.0f;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean isKeyHeld(int keyCode)
    {
        Integer count = heldKeys.get(keyCode);

        return count != null && count > 0;
    }

    @Nonnull
    @Override
    public IControl getFocusedControl()
    {
        return focusedControl;
    }

    @Override
    public void setFocusedControl(@Nullable IControl control)
    {
        focusedControl = control == null ? this : control;
    }

    @Nonnull
    @Override
    public IControl getHoveredControl()
    {
        return hoveredControl;
    }

    @Override
    public void setHoveredControl(@Nullable IControl control, int mouseX, int mouseY)
    {
        control = control == null ? this : control;

        if (control != hoveredControl)
        {
            hoveredControl.controlHoverStop(new MouseEvent(mouseX, mouseY));
            control.controlHoverStart(new MouseEvent(mouseX, mouseY));
        }

        hoveredControl = control;
    }

    @Nullable
    @Override
    public IControl getPressedControl()
    {
        return pressedControl;
    }

    @Override
    public int getPressedMouseX()
    {
        return pressedMouseX;
    }

    @Override
    public int getPressedMouseY()
    {
        return pressedMouseY;
    }

    @Override
    public void setPressedControl(@Nullable IControl control, int mouseX, int mouseY)
    {
        this.pressedControl = control;
        this.pressedMouseX = mouseX;
        this.pressedMouseY = mouseY;
    }

    @Nullable
    @Override
    public IControl getDraggedControl()
    {
        return draggedControl;
    }

    @Override
    public void setDraggedControl(@Nullable IControl control)
    {
        draggedControl = control;
    }

    @Nonnull
    @Override
    public EventHandler<MouseEvent> getOnControlHover()
    {
        return onControlHover;
    }

    @Nonnull
    @Override
    public EventHandler<MouseEvent> getOnControlHoverStart()
    {
        return onControlHoverStart;
    }

    @Nonnull
    @Override
    public EventHandler<MouseEvent> getOnControlHoverStop()
    {
        return onControlHoverStop;
    }

    @Nonnull
    @Override
    public EventHandler<MouseButtonEvent> getOnControlMouseClicked()
    {
        return onControlMouseClicked;
    }

    @Nonnull
    @Override
    public EventHandler<MouseButtonEvent> getOnControlMouseReleased()
    {
        return onControlMouseReleased;
    }

    @Nonnull
    @Override
    public EventHandler<MouseScrollEvent> getOnControlMouseScrolled()
    {
        return onControlMouseScrolled;
    }

    @Nonnull
    @Override
    public EventHandler<KeyEvent> getOnControlKeyPressed()
    {
        return onControlKeyPressed;
    }

    @Nonnull
    @Override
    public EventHandler<KeyEvent> getOnControlKeyReleased()
    {
        return onControlKeyReleased;
    }

    @Nonnull
    @Override
    public EventHandler<KeyEvent> getOnControlKeyHeld()
    {
        return onControlKeyHeld;
    }

    @Nonnull
    @Override
    public EventHandler<CharEvent> getOnControlCharTyped()
    {
        return onControlCharTyped;
    }

    @Nullable
    @Override
    public IControl getRecentlyPressedControl()
    {
        return recentlyPressedControl;
    }

    @Override
    public void setRecentlyClickedControl(@Nullable IControl control)
    {
        recentlyPressedControl = control;
    }
}
