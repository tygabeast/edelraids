package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Picks the geo model and texture from the stack's weapon form. Resource locations are built once per form and cached,
 * so rendering allocates nothing per frame. A form without a model (for example one added by a datapack) falls back to
 * the longsword instead of failing to render.
 */
public class EdelWeaponModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation DEFAULT_FORM = EdelRaids.id("longsword");
    private static final ResourceLocation ANIMATION = EdelRaids.id("animations/item/edel_weapon.animation.json");

    private record Paths(ResourceLocation model, ResourceLocation texture) {}

    private final Map<ResourceLocation, Paths> cache = new HashMap<>();

    private Paths paths(ResourceLocation form) {
        Paths p = cache.get(form);
        if (p == null) {
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(form.getNamespace(), "geo/item/weapon/" + form.getPath() + ".geo.json");
            if (!form.equals(DEFAULT_FORM) && !GeckoLibCache.getBakedModels().containsKey(model)) {
                p = paths(DEFAULT_FORM);
            } else {
                p = new Paths(model, ResourceLocation.fromNamespaceAndPath(form.getNamespace(), "textures/item/weapon/" + form.getPath() + ".png"));
            }
            cache.put(form, p);
        }
        return p;
    }

    private Paths paths(GeoRenderer<T> renderer) {
        if (renderer instanceof GeoItemRenderer<?> itemRenderer) {
            EdelRaidData d = EdelWeaponItem.data(itemRenderer.getCurrentItemStack());
            if (d != null) return paths(d.form());
        }
        return paths(DEFAULT_FORM);
    }

    @Override
    public ResourceLocation getModelResource(T animatable, GeoRenderer<T> renderer) {
        return paths(renderer).model();
    }

    @Override
    public ResourceLocation getTextureResource(T animatable, GeoRenderer<T> renderer) {
        return paths(renderer).texture();
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return paths(DEFAULT_FORM).model();
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return paths(DEFAULT_FORM).texture();
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }
}
