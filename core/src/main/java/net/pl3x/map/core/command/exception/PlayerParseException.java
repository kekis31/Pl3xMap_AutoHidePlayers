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
package net.pl3x.map.core.command.exception;

import net.pl3x.map.core.command.Sender.Player;
import net.pl3x.map.core.configuration.Lang;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown to indicate that a method has been passed an illegal or inappropriate {@link Player} argument.
 */
public class PlayerParseException extends ArgumentParseException {
    public static final Reason MUST_SPECIFY_PLAYER = new Reason(() -> Lang.ERROR_MUST_SPECIFY_PLAYER);
    public static final Reason NO_SUCH_PLAYER = new Reason(() -> Lang.ERROR_NO_SUCH_PLAYER);

    /**
     * Construct a new PlayerParseException.
     *
     * @param input  Input
     * @param reason Failure reason
     */
    public PlayerParseException(@Nullable String input, @NonNull Reason reason) {
        super(input, "<player>", reason);
    }
}
