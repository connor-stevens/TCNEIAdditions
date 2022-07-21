package ru.timeconqueror.tcneiadditions.nei.arcaneworkbench;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect;
import com.djgiannuzz.thaumcraftneiplugin.nei.NEIHelper;
import com.djgiannuzz.thaumcraftneiplugin.nei.recipehandler.ArcaneShapelessRecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import ru.timeconqueror.tcneiadditions.util.TCNAConfig;
import ru.timeconqueror.tcneiadditions.util.TCUtil;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.client.lib.UtilsFX;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArcaneCraftingShapelessHandler extends ArcaneShapelessRecipeHandler {
    private final String userName = Minecraft.getMinecraft().getSession().getUsername();

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(this.getOverlayIdentifier())) {
            for (Object o : ThaumcraftApi.getCraftingRecipes()) {
                if (o instanceof ShapelessArcaneRecipe) {
                    ShapelessArcaneRecipe tcRecipe = (ShapelessArcaneRecipe) o;
                    boolean isResearchComplete = TCUtil.shouldShowRecipe(this.userName, tcRecipe.getResearch());
                    ArcaneShapelessCachedRecipe recipe = new ArcaneShapelessCachedRecipe(tcRecipe, isResearchComplete);
                    if (recipe.isValid()) {
                        this.arecipes.add(recipe);
                        this.aspectsAmount.add(getAmounts(tcRecipe));
                    }
                }
            }
        } else if (outputId.equals("item")) {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (Object o : ThaumcraftApi.getCraftingRecipes()) {
            if (o instanceof ShapelessArcaneRecipe) {
                ShapelessArcaneRecipe tcRecipe = (ShapelessArcaneRecipe) o;
                boolean isResearchComplete = TCUtil.shouldShowRecipe(this.userName, tcRecipe.getResearch());
                ArcaneShapelessCachedRecipe recipe = new ArcaneShapelessCachedRecipe(tcRecipe, isResearchComplete);
                if (recipe.isValid() && NEIServerUtils.areStacksSameTypeCraftingWithNBT(tcRecipe.getRecipeOutput(), result)) {
                    this.arecipes.add(recipe);
                    this.aspectsAmount.add(getAmounts(tcRecipe));
                }
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (Object o : ThaumcraftApi.getCraftingRecipes()) {
            if (o instanceof ShapelessArcaneRecipe) {
                ShapelessArcaneRecipe tcRecipe = (ShapelessArcaneRecipe) o;
                ArcaneShapelessCachedRecipe recipe = new ArcaneShapelessCachedRecipe(tcRecipe, true);
                if (recipe.isValid() && recipe.containsWithNBT(recipe.ingredients, ingredient) && TCUtil.shouldShowRecipe(this.userName, tcRecipe.getResearch())) {
                    recipe.setIngredientPermutation(recipe.ingredients, ingredient);
                    this.arecipes.add(recipe);
                    this.aspectsAmount.add(getAmounts(tcRecipe));
                }
            }
        }
    }

    @Override
    public void drawBackground(int recipeIndex) {
        ArcaneShapelessCachedRecipe recipe = (ArcaneShapelessCachedRecipe) arecipes.get(recipeIndex);
        if (recipe.isResearchComplete) {
            super.drawBackground(recipeIndex);
            return;
        }

        int x = 34;
        int y = -15;
        UtilsFX.bindTexture("textures/gui/gui_researchbook_overlay.png");
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(3042);
        GL11.glTranslatef((float)x, (float)y, 0.0F);
        GL11.glScalef(1.7F, 1.7F, 1.0F);
        GuiDraw.drawTexturedModalRect(20, 7, 20, 3, 16, 16);
        GL11.glPopMatrix();
    }

    @Override
    public List<PositionedStack> getIngredientStacksForOverlay(int recipeIndex) {
        CachedRecipe recipe = arecipes.get(recipeIndex);
        return recipe instanceof IArcaneOverlayProvider ? ((IArcaneOverlayProvider) recipe).getPositionedStacksForOverlay() : null;
    }

    @Override
    public void drawExtras(int recipeIndex) {
        ArcaneShapelessCachedRecipe recipe = (ArcaneShapelessCachedRecipe) arecipes.get(recipeIndex);
        if (recipe.isResearchComplete) {
            super.drawExtras(recipeIndex);
        } else {
            String textToDraw = I18n.format("tcneiadditions.research.missing");
            int y = 28;
            for (Object text : Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(textToDraw, 162)) {
                GuiDraw.drawStringC((String) text, 82, y, Color.BLACK.getRGB(), false);
                y += 11;
            }
        }

        if (TCNAConfig.showResearchKey) {
            int y = 135;
            String textToDraw = I18n.format("tcneiadditions.research.researchKey", recipe.researchKey);
            for (Object text : Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(textToDraw, 162)) {
                GuiDraw.drawStringC((String) text, 82, y, Color.BLACK.getRGB(), false);
                y += 11;
            }
        }
    }

    private boolean isValidInput(Object input) {
        return NEIServerUtils.extractRecipeItems(input).length != 0;
    }

    private class ArcaneShapelessCachedRecipe extends CachedShapelessRecipe implements IArcaneOverlayProvider {
        private final AspectList aspects;
        protected Object[] overlay;
        private final boolean isResearchComplete;
        private final String researchKey;

        public ArcaneShapelessCachedRecipe(ShapelessArcaneRecipe recipe, boolean isResearchComplete) {
            super(recipe.getInput(), recipe.getRecipeOutput());
            this.result = new PositionedStack(recipe.getRecipeOutput(), 74, 2);
            this.overlay = recipe.getInput().toArray();
            this.aspects = recipe.getAspects();
            this.isResearchComplete = isResearchComplete;
            this.researchKey = recipe.getResearch();
            NEIHelper.addAspectsToIngredients(this.aspects, this.ingredients, 0);
        }

        public AspectList getAspectList() {
            return this.aspects;
        }

        public boolean isValid() {
            return !this.ingredients.isEmpty() && this.result != null;
        }

        @Override
        public void setIngredients(List<?> items) {
            if (!items.isEmpty()) {
                int[][] positions = new int[][]{{48, 32}, {75, 33}, {103, 33}, {49, 60}, {76, 60}, {103, 60}, {49, 87}, {76, 87}, {103, 87}};
                int shiftX = 0;
                int shiftY = 0;

                for(int x = 0; x < items.size(); ++x) {
                    if (items.get(x) != null && isValidInput(items.get(x))) {
                        PositionedStack stack = new PositionedStack(items.get(x), positions[x][0] + shiftX, positions[x][1] + shiftY, false);
                        stack.setMaxSize(1);
                        this.ingredients.add(stack);
                    }
                }
            }

        }

        @Override
        public ArrayList<PositionedStack> getPositionedStacksForOverlay() {
            ArrayList<PositionedStack> stacks = new ArrayList<>();
            if (this.overlay != null && this.overlay.length > 0) {
                for(int x = 0; x < this.overlay.length; ++x) {
                    Object object = overlay[x];
                    if ((object instanceof ItemStack
                        || object instanceof ItemStack[]
                        || object instanceof String
                        || (object instanceof List && !((List<?>) object).isEmpty()))) {
                        stacks.add(new PositionedStack(object, 40 + x % 3 * 24, 40 + x / 3 * 24));
                    }
                }
            }

            return stacks;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            if (!this.isResearchComplete) return Collections.emptyList();
            return super.getIngredients();
        }

        @Override
        public void setIngredientPermutation(Collection<PositionedStack> ingredients, ItemStack ingredient) {
            if (ingredient.getItem() instanceof ItemAspect) return;
            super.setIngredientPermutation(ingredients, ingredient);
        }

        @Override
        public boolean contains(Collection<PositionedStack> ingredients, ItemStack ingredient) {
            if (ingredient.getItem() instanceof ItemAspect) {
                Aspect aspect = ItemAspect.getAspects(ingredient).getAspects()[0];
                return this.aspects.aspects.containsKey(aspect);
            }
            return super.contains(ingredients, ingredient);
        }
    }
}
