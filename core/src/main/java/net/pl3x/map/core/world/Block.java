/*
 * MIT License
 *
 * Copyright (c) 2020 William Blake Galbreath
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.pl3x.map.core.world;

import java.util.Objects;
import net.pl3x.map.core.Keyed;
import net.pl3x.map.core.configuration.ColorsConfig;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Block extends Keyed {
    private final int index;
    private final int color;
    private final byte bools;
    private final BlockState defaultState;

    public Block(int index, @NonNull String id, int color) {
        super(id);
        this.index = index;
        this.color = ColorsConfig.BLOCK_COLORS.getOrDefault(id, color);

        boolean air = ColorsConfig.BLOCKS_AIR.contains(id);
        boolean foliage = ColorsConfig.BLOCKS_FOLIAGE.contains(id);
        boolean grass = ColorsConfig.BLOCKS_GRASS.contains(id);
        boolean water = ColorsConfig.BLOCKS_WATER.contains(id);
        boolean glass = ColorsConfig.BLOCKS_GLASS.contains(id);

        this.bools = (byte) ((air ? 1 << 5 : 0) |
                (foliage ? 1 << 4 : 0) |
                (grass ? 1 << 3 : 0) |
                (water ? 1 << 2 : 0) |
                (glass ? 1 << 1 : 0) |
                (water || "minecraft:lava".equals(id) ? 1 : 0)
        );

        this.defaultState = new BlockState(this);
    }

    public int getIndex() {
        return this.index;
    }

    public int color() {
        return this.color;
    }

    public boolean isAir() {
        return ((this.bools >> 5) & 1) > 0;
    }

    public boolean isFoliage() {
        return ((this.bools >> 4) & 1) > 0;
    }

    public boolean isGrass() {
        return ((this.bools >> 3) & 1) > 0;
    }

    public boolean isWater() {
        return ((this.bools >> 2) & 1) > 0;
    }

    public boolean isGlass() {
        return ((this.bools >> 1) & 1) > 0;
    }

    public boolean isFluid() {
        return (this.bools & 1) > 0;
    }

    public @NonNull BlockState getDefaultState() {
        return this.defaultState;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        Block other = (Block) o;
        return Objects.equals(getKey(), other.getKey())
                && color() == other.color()
                && isFluid() == other.isFluid()
                && isFoliage() == other.isFoliage()
                && isGrass() == other.isGrass()
                && isWater() == other.isWater()
                && isGlass() == other.isGlass();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), color(), isFluid(), isFoliage(), isGrass(), isWater(), isGlass());
    }

    @Override
    public @NonNull String toString() {
        return "BlockState{"
                + "key=" + getKey()
                + "color=" + color()
                + "isFluid=" + isFluid()
                + "isFoliage=" + isFoliage()
                + "isGrass=" + isGrass()
                + "isWater=" + isWater()
                + "isGlass=" + isGlass()
                + "}";
    }
}
