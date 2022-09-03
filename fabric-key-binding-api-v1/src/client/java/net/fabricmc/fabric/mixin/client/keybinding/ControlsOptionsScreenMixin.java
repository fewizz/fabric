/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.client.keybinding;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;

@Mixin(ControlsOptionsScreen.class)
abstract class ControlsOptionsScreenMixin extends GameOptionsScreen {
	@Unique
	private ClickableWidget previousButton = null;
	@Unique
	private boolean transformPosition = false;
	@Unique
	private int yIncrease = 0;

	ControlsOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
		super(parent, gameOptions, title);
	}

	@Override
	protected <T extends Element & Drawable & Selectable> T addDrawableChild(T element0) {
		ClickableWidget element = (ClickableWidget) element0;

		final int leftPosition = this.width / 2 - 155;
		final int rightPosition = leftPosition + 160;
		final int yShift = 24;

		final Consumer<ClickableWidget> setButtonPositionRelativeToPrevious = (ClickableWidget w) -> {
			final boolean prevWasAtLeft = previousButton.x < this.width / 2;
			// If previous was at left, current will be at right, and vice versa
			w.x = prevWasAtLeft ? rightPosition : leftPosition;
			w.y = previousButton.y;

			if (!prevWasAtLeft) {
				w.y += yShift;       // Previous was at right, increasing y
				yIncrease += yShift; // and overall increase
			}
		};

		boolean isAutoJump = false;

		if (element instanceof CyclingButtonWidget) {
			TextContent content = ((CyclingButtonWidgetAccessor) element).fabric_getOptionText().getContent();
			isAutoJump = content instanceof TranslatableTextContent && ((TranslatableTextContent) content).getKey().equals("options.autoJump");
		}

		if (isAutoJump) { // Current button is Auto Jump? Then inject all sticky keys before adding it
			for (SimpleOption<Boolean> option : KeyBindingRegistryImpl.getStickyKeyBindingOptions()) {
				ClickableWidget b = option.createButton(this.gameOptions, -1, -1, 150);
				setButtonPositionRelativeToPrevious.accept(b);
				super.addDrawableChild(b);
				previousButton = b;
			}

			transformPosition = true;
		}

		if (transformPosition && element instanceof CyclingButtonWidget) {
			setButtonPositionRelativeToPrevious.accept(element);
		} else { // Probably "Done" button. Stop position transformations, except overall y increase
			transformPosition = false;
			element.y += yIncrease;
		}

		super.addDrawableChild(element);
		previousButton = element;

		return element0;
	}
}
